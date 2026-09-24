# ==============================================================================
# BƯỚC 1: IMPORT CÁC THƯ VIỆN CẦN THIẾT
# ==============================================================================
import os
import io
import cv2
import numpy as np
import pandas as pd
import joblib
import nest_asyncio
import uvicorn
from pyngrok import ngrok
from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from pydantic import BaseModel
from PIL import Image

# Thư viện AI cho CCCD & Khuôn mặt
from ultralytics import YOLO
from vietocr.tool.predictor import Predictor
from vietocr.tool.config import Cfg
from deepface import DeepFace

# ==============================================================================
# BƯỚC 2: CẤU HÌNH ĐƯỜNG DẪN MÔ HÌNH AI (ĐỌC TRỰC TIẾP TỪ GOOGLE DRIVE)
# ==============================================================================
import os

# Đường dẫn tuyệt đối tới thư mục chứa Model trên Google Drive của bạn
BASE_DIR = "/content/drive/MyDrive/Bài học/Khóa luận cử nhân/Model AI"

# 3 dòng code trỏ trực tiếp tới 3 file nằm trong thư mục Model AI
NOSHOW_MODEL_PATH = os.path.join(BASE_DIR, "noshow_rf_model.pkl")
NOSHOW_COLS_PATH = os.path.join(BASE_DIR, "model_columns.pkl")
CCCD_YOLO_PATH = os.path.join(BASE_DIR, "best.pt")

# (Nếu có file Chatbot DB thì để đây, nếu không có hàm chat sẽ báo lỗi nhẹ không sao)
CHATBOT_VECTOR_DB_PATH = os.path.join(BASE_DIR, "Chatbot_VectorDB")

# Lưu ý quan trọng khi chạy trên Colab:
# Bạn cần chạy đoạn code sau ở Cell đầu tiên của Colab để kết nối với Google Drive:
# from google.colab import drive
# drive.mount('/content/drive')

# ==============================================================================
# BƯỚC 3: KHỞI TẠO FASTAPI & NẠP MÔ HÌNH AI VÀO RAM
# ==============================================================================
app = FastAPI(title="May Hotel AI Microservices", version="2.0")

print("⏳ Đang khởi động và nạp các lõi AI vào bộ nhớ RAM...")

# 3.1 Nạp Model No-show
try:
    rf_model = joblib.load(NOSHOW_MODEL_PATH)
    model_columns = joblib.load(NOSHOW_COLS_PATH)
    print("✅ Đã nạp Model No-show")
except:
    print("⚠️ Bỏ qua Model No-show do chưa thấy file")

# 3.2 Nạp Model CCCD (YOLO + VietOCR)
try:
    yolo_model = YOLO(CCCD_YOLO_PATH)
    config = Cfg.load_config_from_name('vgg_transformer')
    config['cnn']['pretrained'] = False
    config['device'] = 'cpu'
    ocr_predictor = Predictor(config)
    print("✅ Đã nạp Model YOLOv8 CCCD và VietOCR")
except:
    print("⚠️ Lỗi khởi tạo model CCCD")

# ==============================================================================
# BƯỚC 4: ĐỊNH NGHĨA SCHEMAS
# ==============================================================================
class BookingData(BaseModel):
    data: dict

class ChatRequest(BaseModel):
    message: str
    session_id: str

# Hàm hỗ trợ Gom nhóm dòng Text (Multiline) từ OCR
def aggregate_ocr_results(ocr_items):
    """
    ocr_items format: [{'label': 'address', 'text': 'Phường 1', 'y_coord': 150}, ...]
    Gom các nhãn trùng nhau lại và nối chuỗi theo thứ tự từ trên xuống dưới (Y tăng dần)
    """
    aggregated = {}
    grouped = {}
    
    for item in ocr_items:
        lbl = item['label']
        if lbl not in grouped:
            grouped[lbl] = []
        grouped[lbl].append(item)
        
    for lbl, items in grouped.items():
        # Sắp xếp các dòng theo tọa độ Y (dòng nào ở trên thì ghép trước)
        items.sort(key=lambda x: x['y_coord'])
        # Nối text các dòng cách nhau bằng khoảng trắng
        combined_text = " ".join([x['text'] for x in items]).strip()
        aggregated[lbl] = combined_text
        
    return aggregated

# ==============================================================================
# BƯỚC 5: API ENDPOINTS
# ==============================================================================

# 5.1 API No-show
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

# 5.2 API Trích xuất CCCD & Xác Thực Khuôn Mặt (ALL-IN-ONE)
@app.post("/api/v1/ai/verify-cccd")
async def verify_cccd(cccd_image: UploadFile = File(...), selfie_image: UploadFile = File(None)):
    """
    API Xử lý 2 bước:
    1. Quét CCCD lấy Text (có ghép dòng)
    2. Nếu có gửi selfie_image -> So sánh khuôn mặt thẻ với khuôn mặt thật
    """
    try:
        # --- ĐỌC ẢNH CCCD ---
        cccd_bytes = await cccd_image.read()
        cccd_pil = Image.open(io.BytesIO(cccd_bytes)).convert("RGB")
        cccd_cv2 = cv2.cvtColor(np.array(cccd_pil), cv2.COLOR_RGB2BGR)

        # --- 1. CHẠY YOLOv8 CẮT VÙNG ---
        results = yolo_model(cccd_pil)
        ocr_raw_items = []
        face_img_array = None

        for result in results:
            for box in result.boxes:
                x1, y1, x2, y2 = map(int, box.xyxy[0])
                class_id = int(box.cls[0])
                label = yolo_model.names[class_id]

                # Nếu YOLO phát hiện ra vùng chứa "Ảnh thẻ/Khuôn mặt" trên CCCD
                if label.lower() in ["face", "avatar", "photo"]:
                    face_img_array = cccd_cv2[y1:y2, x1:x2]
                    continue # Không chạy OCR cho ảnh mặt

                # Nếu là các vùng Text -> Cắt ảnh và chạy VietOCR
                cropped_img = cccd_pil.crop((x1, y1, x2, y2))
                ocr_text = ocr_predictor.predict(cropped_img)
                
                # Lưu nhãn, text và tọa độ y1 để lát ghép nối dòng (multiline)
                ocr_raw_items.append({'label': label, 'text': ocr_text, 'y_coord': y1})

        # Xử lý gom dòng Text hoàn chỉnh
        extracted_data = aggregate_ocr_results(ocr_raw_items)

        # Xử lý ngoại lệ Ngày hết hạn (Thẻ người cao tuổi)
        # Tìm key có chứa chữ 'het' hoặc 'expiry' (tùy thuộc vào nhãn YOLO của bạn)
        expiry_keys = [k for k in extracted_data.keys() if 'het' in k.lower() or 'exp' in k.lower()]
        for key in expiry_keys:
            val = extracted_data[key].lower().replace(" ", "")
            # Nhận diện các lỗi OCR phổ biến của chữ "Không thời hạn"
            if "khong" in val or "thoi" in val or "han" in val or "k" in val:
                # Nếu không có số nào trong chuỗi, khả năng cao là chữ Không thời hạn
                if not any(char.isdigit() for char in val):
                    extracted_data[key] = "Không thời hạn"

        # --- 2. XÁC THỰC KHUÔN MẶT (Nếu có gửi Selfie) ---
        verification_result = None
        if selfie_image and face_img_array is not None:
            selfie_bytes = await selfie_image.read()
            selfie_pil = Image.open(io.BytesIO(selfie_bytes)).convert("RGB")
            selfie_cv2 = cv2.cvtColor(np.array(selfie_pil), cv2.COLOR_RGB2BGR)

            try:
                # Dùng DeepFace so sánh 2 mảng ảnh (Không cần lưu file cứng)
                df_result = DeepFace.verify(
                    img1_path=face_img_array, 
                    img2_path=selfie_cv2, 
                    model_name="ArcFace", # Hoặc Facenet, VGG-Face
                    enforce_detection=False 
                )
                verification_result = {
                    "is_match": df_result["verified"],
                    "similarity": round((1 - df_result["distance"]) * 100, 2) # Tính % giống
                }
            except Exception as e:
                verification_result = {"error": f"Không thể so sánh khuôn mặt: {str(e)}"}

        # --- 3. TRẢ VỀ JSON CHO JAVA BACKEND ---
        return {
            "status": "success",
            "extracted_data": extracted_data,
            "face_verification": verification_result if verification_result else "Không yêu cầu xác thực khuôn mặt"
        }

    except Exception as e:
        return {"status": "error", "message": str(e)}

# 5.3 API So khớp Khuôn mặt (Dành riêng cho Mobile dùng NFC)
@app.post("/api/v1/ai/verify-face-nfc")
async def verify_face_nfc(nfc_image: UploadFile = File(...), selfie_image: UploadFile = File(...)):
    """
    API Dành riêng cho nền tảng di động:
    Mobile đã dùng NFC quét ra Text và Ảnh chân dung (nfc_image).
    Chỉ cần gửi ảnh NFC và ảnh Selfie lên đây để AI so khớp sinh trắc học.
    """
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

# 5.4 API Chatbot
@app.post("/api/v1/ai/chat")
async def chat_with_bot(request: ChatRequest):
    return {"status": "success", "reply": f"Trợ lý AI đang xử lý: {request.message}"}

# ==============================================================================
# BƯỚC 6: KHỞI CHẠY SERVER FASTAPI & NGROK
# ==============================================================================
if __name__ == "__main__":
    nest_asyncio.apply()
    
    # [TÙY CHỌN] Nếu bạn có AuthToken của Ngrok, hãy điền vào đây để kết nối ổn định hơn
    # ngrok.set_auth_token("YOUR_NGROK_AUTH_TOKEN_HERE")
    
    # Khởi tạo Ngrok tunnel ở port 8000
    public_url = ngrok.connect(8000).public_url
    print("*" * 50)
    print(f"🔗 NGROK PUBLIC URL CỦA BẠN: {public_url}")
    print("👉 Hãy copy link này dán vào cấu hình của Spring Boot / Flutter")
    print("*" * 50)
    
    print("🚀 Bắt đầu chạy Server API trên cổng 8000...")
    uvicorn.run(app, host="0.0.0.0", port=8000)
