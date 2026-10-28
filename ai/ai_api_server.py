import os

# ==============================================================================
# TỐI ƯU HÓA CPU & RAM TRÊN VPS UBUNTU (ĐẶT TRƯỚC KHI IMPORT LÕI AI)
# ==============================================================================
os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
os.environ["OMP_NUM_THREADS"] = "1"
os.environ["MKL_NUM_THREADS"] = "1"
os.environ["OPENBLAS_NUM_THREADS"] = "1"
os.environ["VECLIB_MAXIMUM_THREADS"] = "1"
os.environ["NUMEXPR_NUM_THREADS"] = "1"

import io
import cv2
import numpy as np
import pandas as pd
import joblib
import uvicorn
import platform
import random
import json
import torch
import pymongo
from pymongo import MongoClient
import re
import unicodedata
from datetime import datetime
import pickle
import urllib.request
import urllib.error
import threading
import time

from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from PIL import Image
from starlette.concurrency import run_in_threadpool

from ultralytics import YOLO
from vietocr.tool.predictor import Predictor
from vietocr.tool.config import Cfg
from deepface import DeepFace
from pyvi import ViTokenizer

torch.set_num_threads(1)
torch.set_num_interop_threads(1)

BASE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models")

NOSHOW_MODEL_PATH = os.path.join(BASE_DIR, "noshow_rf_model.pkl")
NOSHOW_COLS_PATH = os.path.join(BASE_DIR, "model_columns.pkl")
CCCD_YOLO_PATH = os.path.join(BASE_DIR, "best.pt")
SKLEARN_CHATBOT_PATH = os.path.join(BASE_DIR, "chatbot_sklearn.pkl")
INTENTS_PATH = os.path.join(BASE_DIR, "intents.json")

app = FastAPI(title="May Hotel AI Microservices", version="2.0")

FACE_MODEL_NAME = os.getenv("FACE_MODEL", "Facenet512")
FACE_DETECTOR_BACKEND = os.getenv("FACE_DETECTOR", "opencv")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

print("Đang khởi động và nạp các lõi AI vào bộ nhớ (Chế độ tối ưu RAM)...")

try:
    DeepFace.build_model(FACE_MODEL_NAME)
    print(f"[OK] Đã nạp model FaceMatch {FACE_MODEL_NAME}")
except Exception as exc:
    print(f"[WARN] Chưa thể nạp sẵn model FaceMatch: {exc}")

rf_model = None
model_columns = []
try:
    rf_model = joblib.load(NOSHOW_MODEL_PATH)
    if os.path.exists(NOSHOW_COLS_PATH):
        model_columns = joblib.load(NOSHOW_COLS_PATH)
    print("[OK] Đã nạp Model No-show")
except Exception as e:
    print(f"[WARN] Bỏ qua Model No-show: {e}")

booking_sessions = {}
booking_session_locks = {}
booking_session_guard = threading.Lock()
BOOKING_SESSION_TTL_SECONDS = int(os.getenv("CHATBOT_SESSION_TTL_SECONDS", "1800"))

class ChatRequest(BaseModel):
    message: str
    user_id: str = 'default'
    session_id: str = ''

class NoShowPredictionRequest(BaseModel):
    data: dict

def decode_uploaded_image(raw_bytes, field_name):
    if not raw_bytes:
        raise HTTPException(status_code=400, detail=f"{field_name} không được để trống")
    if len(raw_bytes) > 10 * 1024 * 1024:
        raise HTTPException(status_code=413, detail=f"{field_name} vượt quá 10 MB")
    image = cv2.imdecode(np.frombuffer(raw_bytes, dtype=np.uint8), cv2.IMREAD_COLOR)
    if image is None:
        raise HTTPException(status_code=400, detail=f"{field_name} không phải ảnh hợp lệ")
    return image

# ==============================================================================
# CẤU HÌNH KẾT NỐI MONGODB ATLAS DIRECT (DATABASE: MayHotelDB)
# ==============================================================================
MONGO_URI = os.getenv(
    "MONGO_URI",
    "mongodb+srv://vocuong_mayhotel_db:Tricuongadmin@cluster0.unq6xcf.mongodb.net/MayHotelDB?retryWrites=true&w=majority"
)

def get_db_connection():
    return MongoClient(MONGO_URI)["MayHotelDB"]

def clean_and_parse_date(text):
    if not text:
        return None, "Vui lòng nhập ngày theo định dạng dd/mm/yyyy (Ví dụ: 25/10/2026)."
    text = text.strip()
    cleaned = re.sub(r'[-.\s]+', '/', text)
    match = re.match(r'^(\d{1,2})/(\d{1,2})/(\d{4})$', cleaned)
    if not match:
        return None, "Định dạng ngày không hợp lệ! Vui lòng nhập ngày dạng dd/mm/yyyy (Ví dụ: 25/10/2026)."
    day, month, year = int(match.group(1)), int(match.group(2)), int(match.group(3))
    try:
        return datetime(year, month, day).date(), None
    except ValueError:
        return None, f"Ngày {text} không hợp lệ! Bạn vui lòng kiểm tra lại."

def normalize_vietnamese(text):
    normalized = unicodedata.normalize("NFD", text.lower())
    return "".join(char for char in normalized if unicodedata.category(char) != "Mn").replace("đ", "d")

def extract_booking_params(text):
    """
    Trích xuất linh hoạt Check-in, Check-out, Người lớn, Trẻ em 
    từ chuỗi câu nhập tự do không cố định trình tự.
    """
    text_clean = text.strip()
    text_norm = normalize_vietnamese(text_clean)
    
    date_pattern = r'(\d{1,2}[\/\.-]\d{1,2}[\/\.-]\d{4})'
    found_dates = re.findall(date_pattern, text_clean)
    
    str_checkin, str_checkout = None, None
    if len(found_dates) >= 2:
        str_checkin, str_checkout = found_dates[0], found_dates[1]
    elif len(found_dates) == 1:
        str_checkin = found_dates[0]

    adults, children = None, 0
    
    adult_match = re.search(r'(\d+)\s*(?:nguoi\s*lon|nguoi|khach|lon|nl)', text_norm)
    if adult_match:
        adults = int(adult_match.group(1))
        
    child_match = re.search(r'(\d+)\s*(?:tre\s*em|tre|te|em\s*be|con)', text_norm)
    if child_match:
        children = int(child_match.group(1))

    text_without_dates = text_clean
    for d in found_dates:
        text_without_dates = text_without_dates.replace(d, '')
        
    remaining_numbers = re.findall(r'\b\d+\b', text_without_dates)
    
    if adults is None:
        if len(remaining_numbers) > 0:
            adults = int(remaining_numbers[0])
            if children == 0 and len(remaining_numbers) > 1:
                children = int(remaining_numbers[1])
        else:
            adults = 2

    return str_checkin, str_checkout, adults, children

def get_room_price_summary():
    try:
        db = get_db_connection()
        pipeline = [
            {"$group": {"_id": "$room_type_id", "price": {"$min": "$price"}}},
            {"$lookup": {"from": "room_types", "localField": "_id", "foreignField": "_id", "as": "room_type"}},
            {"$unwind": "$room_type"},
            {"$sort": {"price": 1}},
        ]
        rows = list(db.rooms.aggregate(pipeline))
        if rows:
            return "\n".join(
                f"- **{row['room_type']['name']}**: từ {float(row.get('price') or 0):,.0f} VNĐ/đêm"
                for row in rows
            )
    except Exception as exc:
        print(f"Không thể tải bảng giá phòng từ MongoDB Atlas: {exc}")
    return "Giá phòng thay đổi theo loại phòng và thời gian lưu trú."

def get_db_availability(checkin_str, checkout_str, adults, children):
    """Truy vấn trực tiếp phòng trống và giá chính xác từ MongoDB Atlas MayHotelDB."""
    try:
        db = get_db_connection()
        checkin_dt = datetime.strptime(checkin_str, "%d/%m/%Y")
        checkout_dt = datetime.strptime(checkout_str, "%d/%m/%Y")

        # 1. Lọc đơn đặt phòng bị trùng lịch
        overlapping_orders = list(db.booking_orders.find({
            "status": {"$nin": ["Đã hủy", "Canceled", "Hủy"]},
            "expected_in": {"$lt": checkout_dt},
            "expected_out": {"$gt": checkin_dt}
        }, {"_id": 1}))
        
        booked_order_ids = [order["_id"] for order in overlapping_orders]
        
        booked_room_ids = []
        if booked_order_ids:
            details = list(db.booking_details.find({"booking_id": {"$in": booked_order_ids}}, {"room_id": 1}))
            booked_room_ids = [d["room_id"] for d in details]

        room_types = list(db.room_types.find())
        available_summary = []

        for rt in room_types:
            rt_id = rt["_id"]
            rt_name = rt.get("name", "Phòng")

            # --- LẤY GIÁ PHÒNG LINH HOẠT ---
            # Thử lấy giá từ room_types (các alias phổ biến)
            rt_price = float(rt.get("price") or rt.get("basePrice") or rt.get("pricePerNight") or rt.get("price_per_night") or 0)

            # Nếu room_types không có giá, truy vấn giá min từ collection rooms theo room_type_id
            if rt_price == 0:
                sample_room = db.rooms.find_one(
                    {"room_type_id": rt_id, "price": {"$gt": 0}},
                    sort=[("price", 1)]
                )
                if sample_room and sample_room.get("price"):
                    rt_price = float(sample_room["price"])

            # Đếm số phòng khả dụng
            total_rooms = db.rooms.count_documents({"room_type_id": rt_id, "status": {"$ne": "Bảo trì"}})
            booked_count = db.rooms.count_documents({"room_type_id": rt_id, "_id": {"$in": booked_room_ids}})
            
            avail_count = total_rooms - booked_count
            if avail_count > 0:
                available_summary.append({
                    "name": rt_name,
                    "price": rt_price,
                    "availableCount": avail_count
                })

        if available_summary:
            return available_summary
            
    except Exception as exc:
        print(f"[ERROR] Tra cứu MongoDB Atlas thất bại: {exc}")

    return [
        {"name": "Phòng Đơn Standard", "price": 500000.0, "availableCount": 3},
        {"name": "Phòng Đôi Superior", "price": 850000.0, "availableCount": 2},
        {"name": "Phòng VIP Suite", "price": 1500000.0, "availableCount": 1}
    ]

def save_final_booking(session, user_id):
    try:
        # Tự động lấy tên miền public từ biến môi trường (mặc định lấy BACKEND_SERVER_URL)
        public_url = os.getenv("PUBLIC_SERVER_URL", os.getenv("BACKEND_SERVER_URL", "http://127.0.0.1:8080")).rstrip("/")
        backend_url = os.getenv("BACKEND_SERVER_URL", "http://127.0.0.1:8080").rstrip("/")
        internal_token = os.getenv("CHATBOT_INTERNAL_TOKEN", "").strip()
        if not internal_token:
            raise RuntimeError("CHATBOT_INTERNAL_TOKEN chưa được cấu hình trên AI server")
        
        checkin_iso = datetime.strptime(session["checkin"], "%d/%m/%Y").strftime("%Y-%m-%d")
        checkout_iso = datetime.strptime(session["checkout"], "%d/%m/%Y").strftime("%Y-%m-%d")
        payload = {
            "customerName": session["customer_name"],
            "phone": session["phone"],
            "roomType": session.get("selected_room", ""),
            "checkIn": checkin_iso,
            "checkOut": checkout_iso,
            "adultCount": max(int(session.get("adults", 1)), 1),
            "childCount": max(int(session.get("children", 0)), 0),
            "infantCount": max(int(session.get("infants", 0)), 0),
            "paymentMethod": "ONLINE",
            "note": session.get("note", ""),
        }
        request = urllib.request.Request(
            backend_url + "/api/v1/internal/chatbot/bookings",
            data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
            headers={"Content-Type": "application/json", "X-Internal-Token": internal_token},
            method="POST",
        )
        with urllib.request.urlopen(request, timeout=12) as response:
            result = json.loads(response.read().decode("utf-8"))
        
        raw_payment_url = str(result.get("paymentUrl") or "").strip()
        invoice_id = result.get("invoiceId")
        if not raw_payment_url or invoice_id is None:
            raise RuntimeError("Backend chưa trả về mã hóa đơn hoặc liên kết VNPay")
        # Tự động ghi đè 127.0.0.1 thành tên miền public thực tế
        if "127.0.0.1" in raw_payment_url or "localhost" in raw_payment_url:
            raw_payment_url = re.sub(r'http://(127\.0\.0\.1|localhost):8080', public_url, raw_payment_url)

        if not raw_payment_url.startswith("https://"):
            raise RuntimeError("Backend trả về liên kết thanh toán không an toàn")
        session["total_amount"] = float(result.get("totalAmount", 0.0))
        session["deposit_amount"] = float(result.get("depositAmount", 0.0))
        session["payment_url"] = raw_payment_url
        session["invoice_id"] = invoice_id
        return result
    except urllib.error.HTTPError as e:
        response_body = e.read().decode("utf-8", errors="replace")
        try:
            backend_message = json.loads(response_body).get("message", response_body)
        except json.JSONDecodeError:
            backend_message = response_body
        print(f"[ERROR] Backend từ chối tạo booking ({e.code}): {backend_message}")
        raise RuntimeError(f"Backend từ chối tạo booking ({e.code}): {backend_message}") from e
    except urllib.error.URLError as e:
        print(f"[ERROR] Không kết nối được backend tại {backend_url}: {e.reason}")
        raise RuntimeError(f"Không kết nối được backend tại {backend_url}") from e
    except Exception as e:
        print(f"[ERROR] Tạo đơn đặt phòng qua Backend thất bại: {e}")
        raise RuntimeError("Backend chưa tạo được booking và liên kết VNPay; không phát hành liên kết giữ chỗ giả") from e

def handle_structured_booking(user_id, msg):
    msg_clean = msg.strip()
    normalized_msg = normalize_vietnamese(msg_clean)
    session = booking_sessions.get(user_id)

    if normalized_msg in ["reset", "bat dau lai", "huy tu van"]:
        booking_sessions.pop(user_id, None)
        session = None

    if not session:
        if any(keyword in normalized_msg for keyword in ["nhan vien", "le tan", "nguoi that", "ho tro truc tiep"]):
            return "Mình sẽ chuyển yêu cầu của bạn đến lễ tân. Bạn vui lòng để lại số điện thoại để khách sạn liên hệ hỗ trợ."

        booking_keywords = ["dat phong", "gia", "bao nhieu", "phong", "con phong", "check-in", "check in"]
        greeting_keywords = ["chao", "hello", "hi", "xin chao"]
        if any(keyword in normalized_msg for keyword in booking_keywords):
            booking_sessions[user_id] = {"step": "ask_dates"}
            price_summary = get_room_price_summary() if any(
                keyword in normalized_msg for keyword in ["gia", "bao nhieu"]
            ) else "Mình sẽ kiểm tra loại phòng và giá chính xác theo ngày bạn chọn."
            return (
                f"{price_summary}\n\n"
                "Để kiểm tra phòng trống và giá chính xác, vui lòng nhập:\n"
                "**[Ngày check-in] - [Ngày check-out] - [Số người lớn] - [Số trẻ em]**\n\n"
                "*(Hoặc nhập tự do: Ví dụ: 28/10/2026 đến 29/10/2026 cho 2 người lớn 1 trẻ em)*"
            )

        if not any(keyword in normalized_msg for keyword in greeting_keywords):
            return "Tôi chưa hiểu câu hỏi của bạn! Bạn có thể hỏi về giá, loại phòng, phòng trống hoặc nhập **đặt phòng** để bắt đầu."

        booking_sessions[user_id] = {"step": "ask_dates"}
        return (
            "Chào mừng bạn đến với May Hotel!\n\n"
            "Để tra cứu thông tin phòng trống, bạn vui lòng nhập thông tin theo cú pháp:\n"
            "👉 **[Ngày check-in] - [Ngày check-out] - [Số người lớn] - [Số trẻ em]**\n\n"
            "*(Hoặc nhập tự do: Ví dụ: 28/10/2026 đến 29/10/2026 cho 2 người lớn)*"
        )

    step = session.get("step")

    if step == "ask_dates":
        str_checkin, str_checkout, adults, children = extract_booking_params(msg_clean)

        if not str_checkin or not str_checkout:
            return (
                "Cảm ơn bạn! Để tra cứu chính xác, vui lòng cung cấp **Ngày check-in** và **Ngày check-out**.\n\n"
                "👉 *Ví dụ:* **28/10/2026 đến 29/10/2026 cho 2 người lớn 1 trẻ em**\n"
                "hoặc **28/10/2026 - 29/10/2026 - 2 - 0**"
            )

        checkin_dt, err1 = clean_and_parse_date(str_checkin)
        checkout_dt, err2 = clean_and_parse_date(str_checkout)

        if err1: return err1
        if err2: return err2
        if checkout_dt <= checkin_dt:
            return "Ngày trả phòng (Check-out) phải sau ngày nhận phòng (Check-in). Bạn vui lòng kiểm tra lại nhé!"

        session.update({
            "checkin": checkin_dt.strftime("%d/%m/%Y"),
            "checkout": checkout_dt.strftime("%d/%m/%Y"),
            "adults": adults,
            "children": children,
            "step": "ask_room_choice"
        })

        rows = get_db_availability(session["checkin"], session["checkout"], adults, children)
        if rows:
            session["available_room_types"] = [str(r["name"]) for r in rows]
            room_list_str = "\n".join(
                f"- **{r['name']}**: {float(r.get('price') or 0):,.0f} VNĐ/đêm "
                f"(Còn {int(r.get('availableCount') or 0)} phòng)"
                for r in rows
            )
        else:
            return (
                "Hiện không còn phòng trống phù hợp cho giai đoạn đã chọn. "
                "Bạn vui lòng chọn khoảng thời gian khác giúp khách sạn nhé."
            )

        return (
            f"May Hotel hiện có các loại phòng trống cho **{session['checkin']} -> {session['checkout']}** "
            f"({adults} người lớn, {children} trẻ em):\n\n"
            f"{room_list_str}\n\n"
            f"👉 Bạn muốn chọn loại phòng nào? (Ví dụ nhập: **Phòng Đôi** hoặc **Phòng VIP**)"
        )

    if step == "ask_room_choice":
        requested_room = normalize_vietnamese(msg_clean)
        available_types = session.get("available_room_types", [])
        selected_room = next(
            (
                room_name for room_name in available_types
                if normalize_vietnamese(room_name) in requested_room
                or requested_room in normalize_vietnamese(room_name)
            ),
            None,
        )
        if selected_room is None:
            return (
                "Loại phòng bạn chọn không có trong danh sách còn trống. "
                "Vui lòng nhập đúng tên một loại phòng ở danh sách phía trên."
            )
        session.update({"selected_room": selected_room, "step": "ask_customer_info"})
        return (
            f"Bạn đã chọn: **{selected_room}**.\n\n"
            f"Để tiếp tục, bạn vui lòng nhập thông tin người đặt theo cú pháp:\n"
            f"👉 **[Tên khách đặt] - [Số điện thoại] - [Yêu cầu đặc biệt (Ghi chú)]**\n\n"
            f"*(Ví dụ: Nguyễn Văn A - 0987654321 - Nhận phòng muộn lúc 20h)*"
        )

    if step == "ask_customer_info":
        parts = [p.strip() for p in msg_clean.split("-")]
        if len(parts) < 2:
            return "Vui lòng nhập tối thiểu **Tên khách đặt** và **Số điện thoại** phân tách bằng dấu gạch ngang (-)."
        session.update({
            "customer_name": parts[0],
            "phone": parts[1],
            "note": parts[2] if len(parts) >= 3 else "Không có",
            "step": "ask_payment"
        })
        return (
            f"Cảm ơn **{parts[0]}**!\n\n"
            f"Đặt phòng trực tuyến yêu cầu **đặt cọc 10% qua VNPay**.\n"
            f"Phần còn lại thanh toán tại khách sạn khi nhận phòng.\n\n"
            f"👉 Nhập **đồng ý** để tạo yêu cầu giữ chỗ 15 phút và nhận liên kết VNPay."
        )

    if step == "ask_payment":
        payment_method = "Đặt cọc 10% qua VNPay"
        session["payment_method"] = payment_method
        try:
            booking_result = save_final_booking(session, user_id)
        except Exception as exc:
            print(f"[ERROR] Hoàn tất booking thất bại: {exc}")
            return (
                "Không thể hoàn tất đặt phòng lúc này. Thông tin của bạn vẫn được giữ; "
                "vui lòng thử lại hoặc liên hệ lễ tân."
            )
        booking_sessions[user_id]["step"] = "completed"

        res_msg = (
            f"🕒 **MAY HOTEL ĐÃ GIỮ CHỖ TRONG 15 PHÚT**\n\n"
            f"📋 **THÔNG TIN YÊU CẦU ĐẶT PHÒNG**:\n"
            f"- Khách hàng: **{session['customer_name']}** ({session['phone']})\n"
            f"- Loại phòng: **{session['selected_room']}**\n"
            f"- Lưu trú: **{session['checkin']} -> {session['checkout']}**\n"
            f"- Phương thức: **{payment_method}**\n\n"
        )
        amount = int(session.get("deposit_amount", 0))
        payment_url = session.get("payment_url", "#")
        res_msg += (
            f"Tổng giá trị: **{session.get('total_amount', 0):,.0f} VNĐ**\n"
            f"Tiền cọc 10%: **{amount:,.0f} VNĐ**\n\n"
            f"[Thanh toán cọc an toàn qua VNPay]({payment_url})\n\n"
            "Mã đặt phòng chỉ được xác nhận sau khi VNPay báo giao dịch thành công."
        )
        return res_msg

    booking_sessions[user_id] = {"step": "ask_dates"}
    return handle_structured_booking(user_id, "bắt đầu")

def handle_structured_booking_safely(user_id, message):
    with booking_session_guard:
        user_lock = booking_session_locks.setdefault(user_id, threading.Lock())

    with user_lock:
        session = booking_sessions.get(user_id)
        if session and time.time() - float(session.get("_updated_at", 0)) > BOOKING_SESSION_TTL_SECONDS:
            booking_sessions.pop(user_id, None)
        reply = handle_structured_booking(user_id, message)
        active_session = booking_sessions.get(user_id)
        if active_session is not None:
            active_session["_updated_at"] = time.time()
        return reply

@app.post("/api/v1/ai/verify-cccd")
async def verify_cccd(
    cccd_image: UploadFile = File(...),
    selfie_image: UploadFile = File(...),
):
    allowed_types = {"image/jpeg", "image/jpg", "image/png", "image/webp"}
    if cccd_image.content_type not in allowed_types or selfie_image.content_type not in allowed_types:
        raise HTTPException(status_code=415, detail="Chỉ chấp nhận ảnh JPEG, PNG hoặc WebP")

    cccd_frame = decode_uploaded_image(await cccd_image.read(), "Ảnh CCCD")
    selfie_frame = decode_uploaded_image(await selfie_image.read(), "Ảnh selfie")

    try:
        result = await run_in_threadpool(
            lambda: DeepFace.verify(
                img1_path=cccd_frame,
                img2_path=selfie_frame,
                model_name=FACE_MODEL_NAME,
                detector_backend=FACE_DETECTOR_BACKEND,
                distance_metric="cosine",
                enforce_detection=True,
                align=True,
            )
        )
    except ValueError as exc:
        raise HTTPException(
            status_code=422,
            detail="Không phát hiện rõ khuôn mặt trên ảnh CCCD hoặc ảnh selfie",
        ) from exc
    except Exception as exc:
        raise HTTPException(status_code=503, detail=f"Không thể chạy mô hình FaceMatch: {exc}") from exc

    distance = float(result.get("distance", 1.0))
    threshold = float(result.get("threshold", 0.0))
    similarity = round(max(0.0, min(100.0, (1.0 - distance) * 100.0)), 2)
    verified = bool(result.get("verified", False))
    return {
        "status": "success",
        "face_verification": {
            "verified": verified,
            "similarity": similarity,
            "distance": round(distance, 6),
            "threshold": threshold,
            "model": result.get("model", FACE_MODEL_NAME),
        },
    }

@app.post("/api/v1/ai/predict-noshow")
async def predict_noshow(request: NoShowPredictionRequest):
    if rf_model is None or not model_columns:
        raise HTTPException(status_code=503, detail="Mô hình no-show chưa sẵn sàng")

    try:
        raw = request.data or {}
        df_input = pd.DataFrame([{
            "lead_time": max(int(raw.get("lead_time", 0)), 0),
            "adults": max(int(raw.get("adults", 1)), 1),
            "children": max(int(raw.get("children", 0)), 0),
            "stays_in_week_nights": max(int(raw.get("stays_in_week_nights", 1)), 1),
            "is_deposit": 1 if int(raw.get("is_deposit", 0)) else 0,
            "total_of_special_requests": max(int(raw.get("total_of_special_requests", 0)), 0),
        }])
        df_encoded = pd.get_dummies(df_input)
        for col in model_columns:
            if col not in df_encoded.columns:
                df_encoded[col] = 0
        df_final = df_encoded[model_columns]
        probability = float(rf_model.predict_proba(df_final)[0][1] * 100)
        level = "Cao" if probability >= 70 else ("Trung bình" if probability >= 40 else "Thấp")
        reason = (
            f"Mức {level}; lead time={df_input.iloc[0]['lead_time']} ngày; "
            f"đặt cọc={df_input.iloc[0]['is_deposit']}; "
            f"yêu cầu đặc biệt={df_input.iloc[0]['total_of_special_requests']}"
        )
        return {
            "status": "success",
            "risk_percentage": round(probability, 1),
            "risk_level": level,
            "reason": reason,
        }
    except (TypeError, ValueError) as exc:
        raise HTTPException(status_code=400, detail=f"Dữ liệu dự đoán không hợp lệ: {exc}") from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Không thể dự đoán no-show: {exc}") from exc

@app.post("/api/v1/ai/chat")
@app.post("/predict")
async def chat_with_bot(request: ChatRequest):
    user_id = request.user_id
    user_msg = request.message
    reply = await run_in_threadpool(handle_structured_booking_safely, user_id, user_msg)
    return {"status": "success", "reply": reply, "answer": reply}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=5000)
