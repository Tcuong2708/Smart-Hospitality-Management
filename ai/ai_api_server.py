import os
import io
import cv2
import numpy as np
import pandas as pd
import joblib
import nest_asyncio
import uvicorn
import platform
import random
import json
import torch
import torch.nn as nn
import pymongo
from pymongo import MongoClient
import re
from datetime import datetime

from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from pydantic import BaseModel
from PIL import Image

from ultralytics import YOLO
from vietocr.tool.predictor import Predictor
from vietocr.tool.config import Cfg
from deepface import DeepFace
from transformers import AutoTokenizer, AutoModel
from pyvi import ViTokenizer
from gensim.utils import simple_preprocess

# Tối ưu hóa CPU & RAM trên VPS Ubuntu
os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
torch.set_num_threads(2)

# ==============================================================================
# CẤU HÌNH ĐƯỜNG DẪN MÔ HÌNH AI
# ==============================================================================
BASE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models")

NOSHOW_MODEL_PATH = os.path.join(BASE_DIR, "noshow_rf_model.pkl")
NOSHOW_COLS_PATH = os.path.join(BASE_DIR, "model_columns.pkl")
CCCD_YOLO_PATH = os.path.join(BASE_DIR, "best.pt")
PHOBERT_PATH = os.path.join(BASE_DIR, "phobert_may_hotel.h5")
INTENTS_PATH = os.path.join(BASE_DIR, "intents.json")

# ==============================================================================
# KHỞI TẠO FASTAPI & NẠP MÔ HÌNH AI
# ==============================================================================
app = FastAPI(title="May Hotel AI Microservices", version="2.0")

print("Đang khởi động và nạp các lõi AI vào bộ nhớ...")

# 1. Nạp Model No-show
try:
    rf_model = joblib.load(NOSHOW_MODEL_PATH)
    if os.path.exists(NOSHOW_COLS_PATH):
        model_columns = joblib.load(NOSHOW_COLS_PATH)
    else:
        model_columns = []
    print("[OK] Đã nạp Model No-show")
except Exception as e:
    print(f"[WARN] Bỏ qua Model No-show: {e}")

# 2. Nạp Model CCCD (YOLO + VietOCR)
try:
    yolo_model = YOLO(CCCD_YOLO_PATH)
    config = Cfg.load_config_from_name('vgg_transformer')
    config['cnn']['pretrained'] = False
    config['device'] = 'cpu'
    ocr_predictor = Predictor(config)
    print("[OK] Đã nạp Model YOLO CCCD và VietOCR")
except Exception as e:
    print(f"[WARN] Lỗi nạp Model CCCD/VietOCR: {e}")

# 3. Nạp Model Chatbot PhoBERT
device = torch.device('cpu')
hardware_name = platform.processor() or "Linux CPU"
print(f"Chatbot khởi chạy trên: {hardware_name} ({device})")

MODEL_NAME = "vinai/phobert-base"
class_names = ['Enjoyment', 'Disgust', 'Sadness', 'Anger', 'Surprise', 'Fear', 'Other']

class SentimentClassifier(nn.Module):
    def __init__(self, n_classes):
        super(SentimentClassifier, self).__init__()
        self.bert = AutoModel.from_pretrained(MODEL_NAME, use_safetensors=True)
        self.drop = nn.Dropout(p=0.3)
        self.fc = nn.Linear(self.bert.config.hidden_size, n_classes)

    def forward(self, input_ids, attention_mask):
        _, output = self.bert(input_ids=input_ids, attention_mask=attention_mask, return_dict=False)
        return self.fc(self.drop(output))

tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME, use_fast=False)
chatbot_model = SentimentClassifier(len(class_names))

try:
    chatbot_model.load_state_dict(torch.load(PHOBERT_PATH, map_location=device, weights_only=True))
    chatbot_model.to(device)
    chatbot_model.eval()
    print("[OK] Đã nạp mô hình PhoBERT thành công.")
except Exception as e:
    print(f"[WARN] Lỗi nạp trọng số PhoBERT: {e}")

if os.path.exists(INTENTS_PATH):
    with open(INTENTS_PATH, 'r', encoding='utf-8') as f:
        intents = json.load(f)
else:
    intents = {"intents": []}

booking_sessions = {}

# ==============================================================================
# SCHEMAS & TIỆN ÍCH
# ==============================================================================
class BookingData(BaseModel):
    data: dict

class ChatRequest(BaseModel):
    message: str
    user_id: str = 'default'
    session_id: str = ''

def aggregate_ocr_results(ocr_items):
    grouped = {}
    for item in ocr_items:
        lbl = item['label']
        grouped.setdefault(lbl, []).append(item)
    aggregated = {}
    for lbl, items in grouped.items():
        items.sort(key=lambda x: x['y_coord'])
        aggregated[lbl] = " ".join([x['text'] for x in items]).strip()
    return aggregated

def normalize_dialect(text):
    dialect_dict = {
        "xịn xò": "chất lượng cao", "book": "đặt phòng", "tui": "tôi",
        "nhiêu": "bao nhiêu", "okela": "đồng ý", "mần": "làm", "nỏ": "không"
    }
    for word, official in dialect_dict.items():
        text = text.replace(word, official)
    return text

def clean_and_parse_date(text):
    if not text:
        return None, "Bạn chưa nhập ngày tháng. Vui lòng gõ theo định dạng Ngày/Tháng/Năm (Ví dụ: 26/05/2026)."
    text = text.strip()
    cleaned = re.sub(r'[-.\s]+', '/', text)
    match = re.match(r'^(\d{1,2})/(\d{1,2})/(\d{4})$', cleaned)
    if not match:
        return None, "Định dạng ngày không hợp lệ! Vui lòng nhập định dạng Ngày/Tháng/Năm (Ví dụ: 26/05/2026)."
    day, month, year = int(match.group(1)), int(match.group(2)), int(match.group(3))
    try:
        return datetime(year, month, day).date(), None
    except ValueError:
        return None, f"Ngày {text} không hợp lệ! Bạn vui lòng kiểm tra lại."

# ==============================================================================
# KẾT NỐI MONGODB (Hỗ trợ cấu hình qua Env Var)
# ==============================================================================
MONGO_URI = os.getenv("MONGO_URI", "mongodb://localhost:27017/")

def get_db_connection():
    client = MongoClient(MONGO_URI)
    return client["MayHotelDB"]

def get_next_sequence(db, seq_name):
    ret = db.database_sequences.find_one_and_update(
        {"_id": seq_name},
        {"$inc": {"seq": 1}},
        upsert=True,
        return_document=pymongo.ReturnDocument.AFTER
    )
    return ret["seq"]

def get_room_prices():
    try:
        db = get_db_connection()
        pipeline = [
            {"$match": {"status": {"$in": ["Phòng trống", "Trống", "Vacant", "Còn phòng"]}}},
            {"$group": {
                "_id": "$room_type_id",
                "GiaTu": {"$min": "$price"},
                "SoLuong": {"$sum": 1}
            }},
            {"$lookup": {
                "from": "room_types",
                "localField": "_id",
                "foreignField": "_id",
                "as": "roomType"
            }},
            {"$unwind": "$roomType"}
        ]
        rows = list(db.rooms.aggregate(pipeline))
        if not rows: 
            return "Dạ hiện tại bên em hết phòng trống ạ."
        res = "Dạ, giá phòng hôm nay:\n" + "\n".join([f"- {r['roomType']['name']}: {r['GiaTu']:,.0f} VNĐ ({r['SoLuong']} phòng)" for r in rows])
        return res + "\n\nBạn muốn đặt loại nào nhắn mình 'Đặt phòng' nhé!"
    except Exception as e: 
        print(f"LỖI TRUY VẤN MONGODB: {e}")
        return "Hệ thống giá đang bảo trì, bạn vui lòng nhắn lại sau nhé!"

def save_booking(name, phone, room_type, checkin, checkout, id_tai_khoan, total_price, ghi_chu_services):
    try:
        db = get_db_connection()
        checkin_date = datetime.strptime(checkin, "%d/%m/%Y")
        checkout_date = datetime.strptime(checkout, "%d/%m/%Y")
        user_room_lower = room_type.lower()
        all_loai = list(db.room_types.find())
        ma_loai = None
        ten_loai_chuan = ""
        for row in all_loai:
            db_name_lower = row.get('name', '').lower()
            if db_name_lower in user_room_lower or user_room_lower in db_name_lower:
                ma_loai = row['_id']
                ten_loai_chuan = row.get('name', '')
                break
        if not ma_loai:
            return None
        pipeline = [
            {"$match": {"room_type_id": ma_loai, "status": {"$in": ["Phòng trống", "Trống", "Vacant", "Còn phòng"]}}},
            {"$sample": {"size": 1}}
        ]
        random_room = list(db.rooms.aggregate(pipeline))
        if not random_room:
            return None
        ma_phong_ngau_nhien = random_room[0]['_id']
        ghi_chu = f"[CHATBOT AI] Đặt ngẫu nhiên phòng {ma_phong_ngau_nhien} ({ten_loai_chuan}) | Dịch vụ: {ghi_chu_services}"
        order_id = get_next_sequence(db, "booking_orders_sequence")
        booking_order = {
            "_id": order_id,
            "customer_id": id_tai_khoan,
            "booking_date": datetime.now(),
            "expected_in": checkin_date,
            "expected_out": checkout_date,
            "status": "Đã xác nhận cọc",
            "_class": "com.votricuong.mayhotel.documents.BookingOrder"
        }
        db.booking_orders.insert_one(booking_order)
        detail_id = get_next_sequence(db, "booking_details_sequence")
        booking_detail = {
            "_id": detail_id,
            "booking_id": order_id,
            "room_id": ma_phong_ngau_nhien,
            "unit_price": float(total_price),
            "_class": "com.votricuong.mayhotel.documents.BookingDetail"
        }
        db.booking_details.insert_one(booking_detail)
        db.rooms.update_one({"_id": ma_phong_ngau_nhien}, {"$set": {"status": "Đã đặt", "note": ghi_chu}})
        return order_id
    except Exception as e:
        print(f"LỖI XẾP PHÒNG MONGODB: {e}") 
        return None

def handle_booking_flow(user_id, msg):
    session = booking_sessions.get(user_id)
    if not session:
        booking_sessions[user_id] = {"step": "ask_name"}
        return "May Hotel sẵn lòng hỗ trợ! Bạn cho mình xin **Họ tên** người đặt nhé?"
    step = session["step"]
    if step == "ask_name":
        session.update({"name": msg, "step": "ask_phone"})
        return f"Chào {msg}, cho mình xin **Số điện thoại** liên hệ nhé?"
    if step == "ask_phone":
        session.update({"phone": msg, "step": "ask_room"})
        return "Bạn muốn đặt loại phòng nào ạ? (Ví dụ: Phòng Đơn, Phòng VIP...)"
    if step == "ask_room":
        session.update({"room_type": msg, "step": "ask_checkin"})
        return "Dạ, bạn dự kiến **Nhận phòng (Check-in)** vào ngày nào ạ? (Ví dụ: 26/05/2026)"
    if step == "ask_checkin":
        checkin_date, error_msg = clean_and_parse_date(msg)
        if error_msg: return error_msg
        today = datetime.now().date()
        if checkin_date < today:
            return f"Ngày nhận phòng ({msg}) đã qua. Vui lòng nhập lại ngày nhận phòng hợp lệ!"
        session.update({"checkin": checkin_date.strftime("%d/%m/%Y"), "step": "ask_checkout"})
        return "Dạ, bạn dự kiến **Trả phòng (Check-out)** vào ngày nào ạ? (Ví dụ: 28/05/2026)"
    if step == "ask_checkout":
        checkout_date, error_msg = clean_and_parse_date(msg)
        if error_msg: return error_msg
        checkin_date = datetime.strptime(session["checkin"], "%d/%m/%Y").date()
        if checkout_date < checkin_date:
            return f"Ngày trả phòng không được trước ngày nhận (**{session['checkin']}**). Bạn vui lòng nhập lại!"
        session.update({"checkout": checkout_date.strftime("%d/%m/%Y"), "step": "ask_services"})
        return (
            "May Hotel có các dịch vụ bổ sung sau, bạn có muốn đăng ký kèm không?\n\n"
            "1. **Buffet sáng:** 150.000 đ/người\n"
            "2. **Thuê xe máy:** 150.000 đ/ngày\n"
            "3. **Dịch vụ Spa:** 300.000 đ/lượt\n"
            "0. **Không đăng ký dịch vụ**\n\n"
            "*(Nhập số lựa chọn, ví dụ: 1,2 hoặc 0)*"
        )
    if step == "ask_services":
        selected_services = []
        services_price = 0
        if "1" in msg:
            selected_services.append("Buffet sáng")
            services_price += 150000
        if "2" in msg:
            selected_services.append("Thuê xe máy")
            services_price += 150000
        if "3" in msg:
            selected_services.append("Dịch vụ Spa")
            services_price += 300000
        services_booked = ", ".join(selected_services) if selected_services else "Không đăng ký"
        room_price = 0
        room_type_chuan = session["room_type"]
        try:
            db = get_db_connection()
            all_loai = list(db.room_types.find())
            for row in all_loai:
                if row.get('name', '').lower() in session["room_type"].lower():
                    room_type_chuan = row.get('name', '')
                    room = db.rooms.find_one({"room_type_id": row['_id']})
                    if room:
                        room_price = float(room.get('price', 0))
                    break
        except Exception as e:
            print(f"Lỗi tính giá phòng: {e}")
        try:
            ci = datetime.strptime(session["checkin"], "%d/%m/%Y").date()
            co = datetime.strptime(session["checkout"], "%d/%m/%Y").date()
            nights = max((co - ci).days, 1)
        except:
            nights = 1
        total_bill = (room_price * nights) + services_price
        session.update({
            "room_type_chuan": room_type_chuan,
            "nights": nights,
            "services_booked": services_booked,
            "total_price": total_bill,
            "step": "ask_confirm"
        })
        return (
            f"**XÁC NHẬN ĐẶT PHÒNG**\n"
            f"- Khách hàng: **{session['name']}** - SĐT: **{session['phone']}**\n"
            f"- Loại phòng: **{room_type_chuan}** ({room_price:,.0f} đ/đêm)\n"
            f"- Lưu trú: {session['checkin']} -> {session['checkout']} ({nights} đêm)\n"
            f"- Dịch vụ: {services_booked}\n"
            f"**TỔNG TIỀN DỰ KIẾN:** **{total_bill:,.0f} đ**\n\n"
            f"Bạn vui lòng nhắn **'Đồng ý'** để chốt phòng hoặc **'Hủy'** để hủy bỏ."
        )
    if step == "ask_confirm":
        user_choice = msg.lower()
        if any(w in user_choice for w in ['hủy', 'không', 'cancel', 'no']):
            del booking_sessions[user_id]
            return "May Hotel đã hủy yêu cầu đặt phòng."
        if any(w in user_choice for w in ['đồng ý', 'ok', 'chốt', 'có', 'yes']):
            id_tai_khoan = int(user_id) if str(user_id).isdigit() else None
            order_id = save_booking(
                session["name"], session["phone"], session["room_type_chuan"],
                session["checkin"], session["checkout"], id_tai_khoan,
                session["total_price"], session["services_booked"]
            )
            if order_id:
                del booking_sessions[user_id]
                qr_url = f"https://img.vietqr.io/image/vietinbank-113366668888-compact2.png?amount={int(session['total_price'])}&addInfo=THANHTOAN{order_id}&accountName=MAY%20HOTEL"
                return (
                    f"**ĐẶT PHÒNG THÀNH CÔNG! MÃ ĐƠN: #{order_id}**\n\n"
                    f"Vui lòng quét mã QR để thanh toán cọc:\n"
                    f"![QR Thanh Toán]({qr_url})"
                )
            return "Hạng phòng này hiện vừa hết. Bạn vui lòng nhắn 'Đặt phòng' để chọn hạng khác nhé!"
        return "Bạn gõ **'Đồng ý'** để chốt đơn hoặc **'Hủy'** nha."

# ==============================================================================
# ENDPOINTS
# ==============================================================================
@app.get("/health")
def health():
    return {"status": "healthy", "service": "May Hotel AI"}

@app.post("/api/v1/ai/predict-noshow")
async def predict_noshow(booking: BookingData):
    try:
        df_input = pd.DataFrame([booking.data])
        df_encoded = pd.get_dummies(df_input)
        for col in model_columns:
            if col not in df_encoded.columns:
                df_encoded[col] = 0
        df_final = df_encoded[model_columns]
        prediction = rf_model.predict(df_final)
        prob = rf_model.predict_proba(df_final)[0][1]
        return {"status": "success", "is_canceled_prediction": int(prediction[0]), "risk_percentage": round(prob * 100, 2)}
    except Exception as e:
        return {"status": "error", "message": str(e)}

@app.post("/api/v1/ai/verify-cccd")
async def verify_cccd(cccd_image: UploadFile = File(...), selfie_image: UploadFile = File(None)):
    try:
        cccd_bytes = await cccd_image.read()
        cccd_pil = Image.open(io.BytesIO(cccd_bytes)).convert("RGB")
        cccd_cv2 = cv2.cvtColor(np.array(cccd_pil), cv2.COLOR_RGB2BGR)
        results = yolo_model(cccd_pil)
        ocr_raw_items = []
        face_img_array = None
        for result in results:
            for box in result.boxes:
                x1, y1, x2, y2 = map(int, box.xyxy[0])
                class_id = int(box.cls[0])
                label = yolo_model.names[class_id]
                if label.lower() in ["face", "avatar", "photo"]:
                    face_img_array = cccd_cv2[y1:y2, x1:x2]
                    continue
                cropped_img = cccd_pil.crop((x1, y1, x2, y2))
                ocr_text = ocr_predictor.predict(cropped_img)
                ocr_raw_items.append({'label': label, 'text': ocr_text, 'y_coord': y1})
        extracted_data = aggregate_ocr_results(ocr_raw_items)
        expiry_keys = [k for k in extracted_data.keys() if 'het' in k.lower() or 'exp' in k.lower()]
        for key in expiry_keys:
            val = extracted_data[key].lower().replace(" ", "")
            if "khong" in val or "thoi" in val or "han" in val or "k" in val:
                if not any(char.isdigit() for char in val):
                    extracted_data[key] = "Không thời hạn"
        verification_result = None
        if selfie_image and face_img_array is not None:
            selfie_bytes = await selfie_image.read()
            selfie_pil = Image.open(io.BytesIO(selfie_bytes)).convert("RGB")
            selfie_cv2 = cv2.cvtColor(np.array(selfie_pil), cv2.COLOR_RGB2BGR)
            try:
                df_result = DeepFace.verify(
                    img1_path=face_img_array, 
                    img2_path=selfie_cv2, 
                    model_name="ArcFace",
                    enforce_detection=False 
                )
                verification_result = {
                    "is_match": df_result["verified"],
                    "similarity": round((1 - df_result["distance"]) * 100, 2)
                }
            except Exception as e:
                verification_result = {"error": f"Không thể so sánh khuôn mặt: {str(e)}"}
        return {
            "status": "success",
            "extracted_data": extracted_data,
            "face_verification": verification_result if verification_result else "Không yêu cầu xác thực khuôn mặt"
        }
    except Exception as e:
        return {"status": "error", "message": str(e)}

@app.post("/api/v1/ai/verify-face-nfc")
async def verify_face_nfc(nfc_image: UploadFile = File(...), selfie_image: UploadFile = File(...)):
    try:
        nfc_bytes = await nfc_image.read()
        nfc_cv2 = cv2.cvtColor(np.array(Image.open(io.BytesIO(nfc_bytes)).convert("RGB")), cv2.COLOR_RGB2BGR)
        selfie_bytes = await selfie_image.read()
        selfie_cv2 = cv2.cvtColor(np.array(Image.open(io.BytesIO(selfie_bytes)).convert("RGB")), cv2.COLOR_RGB2BGR)
        df_result = DeepFace.verify(
            img1_path=nfc_cv2, 
            img2_path=selfie_cv2, 
            model_name="ArcFace",
            enforce_detection=False 
        )
        return {
            "status": "success",
            "is_match": df_result["verified"],
            "similarity": round((1 - df_result["distance"]) * 100, 2)
        }
    except Exception as e:
        return {"status": "error", "message": f"Lỗi so khớp khuôn mặt NFC: {str(e)}"}

@app.post("/api/v1/ai/chat")
@app.post("/predict")
async def chat_with_bot(request: ChatRequest):
    user_id = request.user_id
    user_msg = request.message

    if user_id in booking_sessions or any(w in user_msg.lower() for w in ['đặt phòng', 'book']):
        reply = handle_booking_flow(user_id, user_msg)
        return {"status": "success", "reply": reply, "answer": reply}

    if any(w in user_msg.lower() for w in ['giá', 'nhiêu', 'bao tiền']):
        reply = get_room_prices()
        return {"status": "success", "reply": reply, "answer": reply}

    clean_text = normalize_dialect(user_msg.lower())
    tokenized = ViTokenizer.tokenize(' '.join(simple_preprocess(clean_text)))
    inputs = tokenizer(tokenized, max_length=120, truncation=True, padding='max_length', return_tensors='pt').to(device)

    with torch.no_grad():
        out = chatbot_model(inputs['input_ids'], inputs['attention_mask'])
        prob, pred = torch.max(torch.softmax(out, dim=1), dim=1)
        tag = class_names[pred.item()]

    if prob.item() > 0.65:
        for i in intents.get('intents', []):
            if i['tag'].lower() == tag.lower():
                resp = random.choice(i['responses'])
                return {"status": "success", "reply": resp, "answer": resp, "tag": tag}
    
    default_msg = "May Hotel chưa hiểu ý bạn. Bạn có thể hỏi về giá hoặc đặt phòng nhé!"
    return {"status": "success", "reply": default_msg, "answer": default_msg}

if __name__ == "__main__":
    nest_asyncio.apply()
    uvicorn.run(app, host="0.0.0.0", port=8000, workers=1)