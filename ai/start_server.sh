#!/bin/bash
# ==============================================================================
# SCRIPT CÀI ĐẶT & KHỞI CHẠY AI SERVER TRÊN UBUNTU 24.04 (ORACLE VPS)
# Tối ưu cho cấu hình: 1GB RAM + 8GB Swap
# ==============================================================================

echo "=========================================="
echo "🚀 BẮT ĐẦU CÀI ĐẶT AI SERVER..."
echo "=========================================="

# 1. Cập nhật hệ thống và cài đặt các thư viện lõi hệ điều hành
echo "📦 1. Đang cài đặt thư viện hệ thống cần thiết (OpenCV dependencies)..."
sudo apt-get update -y
sudo apt-get install -y python3-pip python3-venv libgl1-mesa-glx libglib2.0-0 wget

# 2. Tạo môi trường ảo (Virtual Environment) để tránh xung đột thư viện Ubuntu 24.04
echo "🌍 2. Khởi tạo môi trường Python ảo (venv)..."
if [ ! -d "venv" ]; then
    python3 -m venv venv
    echo "✅ Đã tạo thư mục venv/"
fi

# Kích hoạt môi trường ảo
source venv/bin/activate

# 3. Cài đặt các thư viện Python (Chống Out-of-memory bằng --no-cache-dir)
echo "📚 3. Đang cài đặt các thư viện AI. Quá trình này có thể mất vài phút..."
# Sử dụng --no-cache-dir cực kỳ quan trọng cho VPS 1GB RAM để tránh bị Killed (OOM)
pip install --no-cache-dir --upgrade pip
pip install --no-cache-dir fastapi uvicorn python-multipart pydantic numpy pandas joblib opencv-python-headless pillow ultralytics nest-asyncio deepface tf-keras

# Cài đặt VietOCR (Thường yêu cầu torch, torchvision cài trước nhưng pip sẽ tự xử lý)
# Nếu cài pytorch làm tràn RAM, bạn có thể buộc cài bản CPU-only:
pip install --no-cache-dir torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu
pip install --no-cache-dir vietocr

# 4. Tạo thư mục Models nếu chưa có
echo "📁 4. Kiểm tra thư mục chứa Model..."
if [ ! -d "models" ]; then
    mkdir models
    echo "⚠️ Đã tạo thư mục 'models/'. VUI LÒNG UPLOAD CÁC FILE MODEL (best.pt, noshow_rf_model.pkl, model_columns.pkl) VÀO ĐÂY!"
fi

# Hướng dẫn tùy chỉnh mã nguồn python
echo "=========================================="
echo "⚠️ LƯU Ý QUAN TRỌNG TRƯỚC KHI CHẠY:"
echo "Hãy mở file ai_api_server.py và sửa lại đường dẫn BASE_DIR thành thư mục cục bộ của bạn:"
echo "BASE_DIR = './models'"
echo "Bỏ qua ngrok nếu bạn dùng IP công khai của VPS Oracle."
echo "=========================================="

# 5. Khởi chạy Server
echo "🚀 5. KHỞI CHẠY UVICORN SERVER (0.0.0.0:8000)..."
# Chạy với worker giới hạn để tiết kiệm RAM
uvicorn ai_api_server:app --host 0.0.0.0 --port 8000 --workers 1
