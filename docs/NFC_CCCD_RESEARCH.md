# Phạm vi NFC CCCD trong đồ án

## Cách mô tả trong khóa luận

Ứng dụng triển khai **nghiên cứu đọc chip CCCD bằng NFC theo BAC/PACE trên Android**, kết hợp đối sánh khuôn mặt. Kết quả chỉ phục vụ quy trình nhận phòng nội bộ của MAY HOTEL và **không phải kết quả xác thực chính thức từ C06**.

Ba mức xử lý trong ứng dụng:

1. `REAL_NFC`: đọc DG1/DG2 từ chip sau khi người dùng nhập dữ liệu MRZ; mức tin cậy nội bộ `MEDIUM` vì chưa xác minh chuỗi chứng thư quốc gia.
2. `SIGNED_NDEF`: thẻ mô phỏng dùng payload có chữ ký RSA; chỉ dùng để demo và kiểm thử.
3. `AI_ONLY`: ngoại lệ khi không đọc được NFC hoặc khách nước ngoài; lễ tân vẫn phải đối chiếu giấy tờ.

Backend chỉ lưu dấu vân tay HMAC, bốn ký tự cuối, phương thức, mức tin cậy và điểm đối sánh. Không lưu ảnh CCCD, ảnh khuôn mặt hoặc số giấy tờ đầy đủ.

## Cấu hình bắt buộc

Backend và AI trên VPS phải dùng cùng một bí mật ngẫu nhiên:

```text
CHATBOT_INTERNAL_TOKEN=<chuỗi ngẫu nhiên dài tối thiểu 32 byte>
```

Backend:

```text
IDENTITY_HASH_PEPPER=<chuỗi ngẫu nhiên riêng, không dùng lại token chatbot>
NFC_DEMO_PUBLIC_KEY_BASE64=<public key RSA dạng SubjectPublicKeyInfo base64>
FACE_MATCH_THRESHOLD=60
MOBILE_IDENTITY_LEGACY_ENABLED=false
```

QR chatbot chỉ được bật khi cấu hình đủ:

```text
VIETQR_BANK_ID=<mã ngân hàng>
VIETQR_ACCOUNT_NO=<số tài khoản>
VIETQR_ACCOUNT_NAME=MAY HOTEL
```

Không đưa các giá trị thật vào Git. Cấu hình bằng biến môi trường/service trên VPS.

## Thiết bị demo tin cậy

Mặc định ứng dụng chặn thiết bị root/jailbreak, bootloader đã mở, USB debugging và mọi máy ảo. Chỉ tối đa hai điện thoại demo được bỏ qua các kiểm tra này bằng dấu vân tay SHA-256:

```powershell
flutter run --dart-define=TRUSTED_DEVICE_FINGERPRINTS=<sha256-may-1>,<sha256-may-2>
```

Dấu vân tay được in trong debug log với tiền tố `[Security] Device fingerprint:`. Không cấu hình fingerprint của ba máy ảo; chúng sẽ luôn bị chặn. Bản release Android không cho phép HTTP cleartext, còn manifest debug chỉ mở HTTP để phát triển local.

## Tạo thẻ NFC mô phỏng có chữ ký

Tạo cặp khóa RSA ở máy quản trị và giữ private key bên ngoài repository. Sau đó chạy:

```powershell
.\deployment\generate-nfc-demo-payload.ps1 `
  -PrivateKeyPath D:\secure\mayhotel-nfc-private.pem `
  -Last4 1234 `
  -OutputPath D:\secure\nfc-demo-payload.json
```

Ghi toàn bộ JSON đầu ra vào một NDEF Text record. Chỉ public key được cấu hình trên backend; private key không được chép lên mobile hoặc VPS chạy ứng dụng.
