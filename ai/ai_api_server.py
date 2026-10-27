import os

# ==============================================================================
# TỐI ƯU HÓA CPU & RAM TRÊN VPS UBUNTU (PHẢI ĐẶT TRƯỚC KHI IMPORT AI)
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

# Nạp sẵn FaceMatch trước khi nhận request. DeepFace giữ model trong cache nội bộ,
# nhờ đó lần xác thực đầu tiên trên mobile không phải chờ tải model giữa luồng check-in.
try:
    DeepFace.build_model(FACE_MODEL_NAME)
    print(f"[OK] Đã nạp model FaceMatch {FACE_MODEL_NAME}")
except Exception as exc:
    print(f"[WARN] Chưa thể nạp sẵn model FaceMatch: {exc}")

# 1. Nạp Model No-show
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

MONGO_URI = os.getenv("MONGO_URI", "mongodb://127.0.0.1:27017/MayHotelDB")

def get_db_connection():
    return MongoClient(MONGO_URI)["MayHotelDB"]

def get_next_sequence(db, seq_name):
    ret = db.database_sequences.find_one_and_update(
        {"_id": seq_name},
        {"$inc": {"seq": 1}},
        upsert=True,
        return_document=pymongo.ReturnDocument.AFTER
    )
    return ret["seq"]

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

def get_room_price_summary():
    """Trả lời nhu cầu tư vấn giá trước khi bắt đầu luồng đặt phòng."""
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
        print(f"Không thể tải bảng giá phòng: {exc}")
    return "Giá phòng thay đổi theo loại phòng và thời gian lưu trú."

def get_backend_availability(checkin, checkout, adults, children):
    """Lấy phòng trống theo ngày từ backend để dùng chung một nguồn nghiệp vụ."""
    backend_url = os.getenv("BACKEND_SERVER_URL", "http://127.0.0.1:8080").rstrip("/")
    internal_token = os.getenv("CHATBOT_INTERNAL_TOKEN", "").strip()
    if not internal_token:
        raise RuntimeError("CHATBOT_INTERNAL_TOKEN chưa được cấu hình")
    payload = {
        "checkIn": datetime.strptime(checkin, "%d/%m/%Y").strftime("%Y-%m-%d"),
        "checkOut": datetime.strptime(checkout, "%d/%m/%Y").strftime("%Y-%m-%d"),
        "guestCount": max(int(adults) + int(children), 1),
    }
    request = urllib.request.Request(
        backend_url + "/api/v1/internal/chatbot/availability",
        data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
        headers={"Content-Type": "application/json", "X-Internal-Token": internal_token},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=15) as response:
        result = json.loads(response.read().decode("utf-8"))
    return result.get("roomTypes", [])

def save_final_booking(session, user_id):
    try:
        # AI chỉ thu thập hội thoại; toàn bộ kiểm tra phòng và ghi dữ liệu do backend thực hiện.
        backend_url = os.getenv("BACKEND_SERVER_URL", "http://127.0.0.1:8080").rstrip("/")
        internal_token = os.getenv("CHATBOT_INTERNAL_TOKEN", "").strip()
        if not internal_token:
            raise RuntimeError("CHATBOT_INTERNAL_TOKEN chưa được cấu hình")
        checkin_iso = datetime.strptime(session["checkin"], "%d/%m/%Y").strftime("%Y-%m-%d")
        checkout_iso = datetime.strptime(session["checkout"], "%d/%m/%Y").strftime("%Y-%m-%d")
        payload = {
            "customerName": session["customer_name"],
            "phone": session["phone"],
            "roomType": session.get("selected_room", ""),
            "checkIn": checkin_iso,
            "checkOut": checkout_iso,
            "guestCount": max(int(session.get("adults", 1)) + int(session.get("children", 0)), 1),
            "paymentMethod": "ONLINE" if session.get("payment_method") == "Thanh toán Online" else "COD",
            "note": session.get("note", ""),
        }
        request = urllib.request.Request(
            backend_url + "/api/v1/internal/chatbot/bookings",
            data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
            headers={"Content-Type": "application/json", "X-Internal-Token": internal_token},
            method="POST",
        )
        with urllib.request.urlopen(request, timeout=20) as response:
            result = json.loads(response.read().decode("utf-8"))
        session["risk_percentage"] = float(result.get("riskPercentage", 0.0))
        session["total_amount"] = float(result.get("totalAmount", 0.0))
        return int(result["bookingId"])

        # Legacy direct-Mongo implementation retained below temporarily for data-reference only;
        # it is unreachable so the chatbot cannot bypass backend business rules.
        db = get_db_connection()
        booking_date = datetime.utcnow()
        checkin_date = datetime.strptime(session["checkin"], "%d/%m/%Y")
        checkout_date = datetime.strptime(session["checkout"], "%d/%m/%Y")
        
        # --- 1. TÌM KIẾM PHÒNG TRỐNG ---
        user_room_lower = session.get("selected_room", "").lower()
        all_loai = list(db.room_types.find())
        ma_loai = None
        for row in all_loai:
            if row.get('name', '').lower() in user_room_lower or user_room_lower in row.get('name', '').lower():
                ma_loai = row['_id']
                break

        pipeline = [{"$match": {"status": {"$in": ["Phòng trống", "Trống", "Vacant", "Còn phòng"]}}}, {"$sample": {"size": 1}}]
        if ma_loai:
            pipeline[0]["$match"]["room_type_id"] = ma_loai
        random_room = list(db.rooms.aggregate(pipeline))
        if not random_room:
            random_room = list(db.rooms.aggregate([{"$sample": {"size": 1}}]))
        ma_phong = random_room[0]['_id'] if random_room else 101

        # --- 2. CHẠY AI DỰ ĐOÁN NO-SHOW TỰ ĐỘNG ---
        lead_time = (checkin_date - booking_date).days
        lead_time = max(lead_time, 0)
        has_deposit = 1 if session['payment_method'] == "Thanh toán Online" else 0
        nights = max((checkout_date - checkin_date).days, 1)
        
        risk_percentage = 0.0
        is_canceled = 0
        if rf_model is not None and len(model_columns) > 0:
            try:
                df_input = pd.DataFrame([{
                    "lead_time": lead_time,
                    "adults": int(session.get("adults", 2)),
                    "children": int(session.get("children", 0)),
                    "stays_in_week_nights": nights,
                    "is_deposit": has_deposit
                }])
                df_encoded = pd.get_dummies(df_input)
                for col in model_columns:
                    if col not in df_encoded.columns:
                        df_encoded[col] = 0
                df_final = df_encoded[model_columns]
                is_canceled = int(rf_model.predict(df_final)[0])
                risk_percentage = float(rf_model.predict_proba(df_final)[0][1] * 100)
            except Exception as e:
                print(f"Lỗi AI No-show: {e}")

        # Ghi nhận kết quả No-Show vào Ghi chú nội bộ
        note_str = f"[CHATBOT AI] Khách: {session['customer_name']} | SĐT: {session['phone']} | Cảnh báo No-show: {risk_percentage:.1f}% | Y/C: {session['note']}"
        risk_level = "Cao" if risk_percentage >= 70 else ("Trung bình" if risk_percentage >= 40 else "Thấp")
        cancel_time = checkin_date.replace(hour=14 if risk_level == "Cao" else 18, minute=0, second=0, microsecond=0)

        # --- 3. LƯU VÀO CƠ SỞ DỮ LIỆU ---
        order_id = get_next_sequence(db, "booking_orders_sequence")
        
        # Tạo ID Khách vãng lai bằng Timestamp (Long) nếu chưa có ID thực
        id_tai_khoan = int(user_id) if str(user_id).isdigit() else int(datetime.now().timestamp() * 1000)
        
        # Lưu vào collection: booking_orders
        booking_order = {
            "_id": int(order_id),
            "customer_id": id_tai_khoan,
            "booking_date": booking_date,
            "expected_in": checkin_date,
            "expected_out": checkout_date,
            "status": "Đã xác nhận", # Theo yêu cầu của bạn
            "notes": note_str,
            "note": note_str,
            "no_show_risk_level": risk_level,
            "no_show_risk_percentage": risk_percentage,
            "noshow_risk_percent": risk_percentage,
            "auto_cancel_time": cancel_time,
            "guest_count": max(int(session.get("adults", 1)) + int(session.get("children", 0)), 1),
            "verified_guest_count": 0,
            "guest_verifications": [],
            "is_face_verified": False,
            "_class": "com.votricuong.mayhotel.documents.BookingOrder"
        }
        db.booking_orders.insert_one(booking_order)

        # Lưu vào collection: booking_details
        detail_id = get_next_sequence(db, "booking_details_sequence")
        booking_detail = {
            "_id": int(detail_id),
            "booking_id": int(order_id),
            "room_id": int(ma_phong),
            "unit_price": float(session.get("total_price", 800000)),
            "_class": "com.votricuong.mayhotel.documents.BookingDetail"
        }
        db.booking_details.insert_one(booking_detail)
        
        # Cập nhật phòng
        db.rooms.update_one({"_id": ma_phong}, {"$set": {"status": "Đã đặt", "note": note_str}})
        
        # Trả ID và risk về session
        session["risk_percentage"] = risk_percentage
        return order_id
    except Exception as e:
        print(f"LỖI XỬ LÝ LƯU BOOKING: {e}")
        raise RuntimeError(f"Không thể lưu đặt phòng qua backend: {e}") from e

def handle_structured_booking(user_id, msg):
    msg_clean = msg.strip()
    normalized_msg = normalize_vietnamese(msg_clean)
    session = booking_sessions.get(user_id)

    # 1. Khởi tạo
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
                "Ví dụ: **25/10/2026 - 27/10/2026 - 2 - 1**"
            )

        if not any(keyword in normalized_msg for keyword in greeting_keywords):
            return "Tôi chưa hiểu câu hỏi của bạn! Bạn có thể hỏi về giá, loại phòng, phòng trống hoặc nhập **đặt phòng** để bắt đầu."

        booking_sessions[user_id] = {"step": "ask_dates"}
        return (
            "Chào mừng bạn đến với May Hotel!\n\n"
            "Để tra cứu thông tin phòng trống, bạn vui lòng nhập thông tin theo cú pháp:\n"
            "👉 **[Ngày check-in] - [Ngày check-out] - [Số người lớn] - [Số trẻ em]**\n\n"
            "*(Ví dụ: 25/10/2026 - 27/10/2026 - 2 - 1)*"
        )

    step = session.get("step")

    # 2. Bước 1: Tra cứu ngày & số lượng người
    if step == "ask_dates":
        parts = [p.strip() for p in msg_clean.split("-")]
        if len(parts) < 4:
            return "Vui lòng nhập đúng cú pháp 4 thông tin phân tách bằng dấu gạch ngang (-):\n**Ngày check-in - Ngày check-out - Số người lớn - Số trẻ em**\n*(Ví dụ: 25/10/2026 - 27/10/2026 - 2 - 1)*"

        checkin_dt, err1 = clean_and_parse_date(parts[0])
        checkout_dt, err2 = clean_and_parse_date(parts[1])

        if err1: return err1
        if err2: return err2
        if checkout_dt <= checkin_dt:
            return "Ngày trả phòng (Check-out) phải sau ngày nhận phòng (Check-in). Bạn vui lòng nhập lại nhé!"

        try:
            adults, children = int(parts[2]), int(parts[3])
        except ValueError:
            return "Số người lớn và trẻ em phải là số nguyên (Ví dụ: 2 - 1). Bạn vui lòng nhập lại!"
        if adults < 1 or adults > 20 or children < 0 or children > 20:
            return "Số người lớn phải từ 1-20 và số trẻ em phải từ 0-20. Bạn vui lòng nhập lại!"

        session.update({
            "checkin": checkin_dt.strftime("%d/%m/%Y"),
            "checkout": checkout_dt.strftime("%d/%m/%Y"),
            "adults": adults,
            "children": children,
            "step": "ask_room_choice"
        })

        # Backend là nguồn duy nhất kiểm tra giao nhau ngày đặt, tình trạng dọn phòng và sức chứa.
        try:
            rows = get_backend_availability(
                session["checkin"], session["checkout"], adults, children
            )
            if rows:
                session["available_room_types"] = [str(r["name"]) for r in rows]
                room_list_str = "\n".join(
                    f"- **{r['name']}**: {float(r.get('price') or 0):,.0f} VNĐ/đêm "
                    f"(Còn {int(r.get('availableCount') or 0)} phòng)"
                    for r in rows
                )
            else:
                return (
                    "Hiện không còn hạng phòng phù hợp với thời gian và số khách đã chọn. "
                    "Bạn vui lòng chọn giai đoạn khác."
                )
        except Exception as exc:
            print(f"Không thể kiểm tra phòng trống qua backend: {exc}")
            return (
                "Mình chưa thể kiểm tra phòng trống từ hệ thống lúc này. "
                "Vui lòng thử lại sau hoặc liên hệ lễ tân."
            )

        return (
            f"May Hotel hiện có các loại phòng trống cho giai đoạn **{session['checkin']} -> {session['checkout']}**:\n\n"
            f"{room_list_str}\n\n"
            f"👉 Bạn muốn chọn loại phòng nào? (Nhập tên loại phòng, ví dụ: **Phòng Đôi** hoặc **Phòng Ba**)"
        )

    # 3. Bước 2: Nhập thông tin khách
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

    # 4. Bước 3: Chọn phương thức thanh toán
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
            f"Vui lòng chọn **Phương thức thanh toán**:\n"
            f"1️⃣ **Thanh toán tại quầy** (Thanh toán bằng tiền mặt/thẻ khi nhận phòng)\n"
            f"2️⃣ **Thanh toán Online** (Quét mã QR qua ví điện tử/ngân hàng)\n\n"
            f"👉 *(Nhập số **1** hoặc **2**)*"
        )

    # 5. Bước 4: Hoàn tất
    if step == "ask_payment":
        payment_method = "Thanh toán tại quầy" if "1" in msg_clean or "quầy" in msg_clean.lower() else "Thanh toán Online"
        session["payment_method"] = payment_method
        # Gọi Database & AI No-Show
        try:
            order_id = save_final_booking(session, user_id)
        except Exception:
            # Keep the session at the payment step so the customer can retry after
            # the backend/AI service is available again without re-entering data.
            return (
                "Không thể hoàn tất đặt phòng lúc này. Thông tin của bạn vẫn được giữ; "
                "vui lòng thử lại hoặc liên hệ lễ tân."
            )
        booking_sessions[user_id]["step"] = "completed"

        res_msg = (
            f"🎉 **CẢM ƠN BẠN ĐÃ ĐẶT PHÒNG TẠI MAY HOTEL!** 🎉\n\n"
            f"📋 **THÔNG TIN ĐƠN ĐẶT PHÒNG #{order_id}**:\n"
            f"- Khách hàng: **{session['customer_name']}** ({session['phone']})\n"
            f"- Loại phòng: **{session['selected_room']}**\n"
            f"- Lưu trú: **{session['checkin']} -> {session['checkout']}**\n"
            f"- Phương thức: **{payment_method}**\n\n"
        )
        if payment_method == "Thanh toán Online":
            bank_id = os.getenv("VIETQR_BANK_ID", "").strip()
            account_no = os.getenv("VIETQR_ACCOUNT_NO", "").strip()
            account_name = os.getenv("VIETQR_ACCOUNT_NAME", "MAY HOTEL").strip().replace(" ", "%20")
            amount = int(session.get("total_amount", 0))
            if bank_id and account_no and amount > 0:
                qr_url = (
                    f"https://img.vietqr.io/image/{bank_id}-{account_no}-compact2.png"
                    f"?amount={amount}&addInfo=MAYHOTEL{order_id}&accountName={account_name}"
                )
                res_msg += (
                    f"Tổng tiền: **{amount:,.0f} VNĐ**. Quý khách vui lòng quét mã QR dưới đây:\n"
                    f"![QR VietQR]({qr_url})\n\n"
                )
            else:
                res_msg += (
                    "Hệ thống chưa cấu hình QR thanh toán. Đơn đã được giữ và quý khách "
                    "có thể thanh toán tại quầy.\n\n"
                )
        
        # Hiển thị demo Rủi ro No-show cho bạn xem kết quả AI
        res_msg += f"*(Đã lưu CSDL & Kích hoạt AI đánh giá No-Show tự động ngầm: {session['risk_percentage']:.1f}% rủi ro)*"
        return res_msg

    booking_sessions[user_id] = {"step": "ask_dates"}
    return handle_structured_booking(user_id, "bắt đầu")

def handle_structured_booking_safely(user_id, message):
    """Tuần tự hóa hội thoại của từng người dùng và tự dọn phiên quá hạn."""
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
    """So khớp khuôn mặt trên CCCD với ảnh selfie; không tự ghi dữ liệu đặt phòng."""
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
    """Dự đoán thuần, không ghi MongoDB; backend là nơi lưu cảnh báo và lịch hủy."""
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
