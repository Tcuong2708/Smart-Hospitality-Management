# Đồng bộ Chatbot với VNPay

Chatbot chỉ được phát liên kết thanh toán sau khi API backend đã tạo thành công
`booking_order`, `booking_detail` và `invoice`. Không có chế độ tạo giá hoặc URL giả.

## AI server

Tiến trình `ai_api_server.py` cần ba biến môi trường:

```text
BACKEND_SERVER_URL=http://127.0.0.1:8080
PUBLIC_SERVER_URL=https://cuong-smarthotel.me
CHATBOT_INTERNAL_TOKEN=<chuỗi bí mật dùng chung với backend>
```

`BACKEND_SERVER_URL` là địa chỉ AI dùng để gọi Spring Boot. Nếu hai dịch vụ chạy trên
cùng VPS, dùng loopback để không đưa API nội bộ ra Internet.

## Backend Spring Boot

```properties
chatbot.internal.token=${CHATBOT_INTERNAL_TOKEN:}
app.features.vnpay.enabled=${VNPAY_ENABLED:false}
vnpay.tmn-code=${VNPAY_TMN_CODE:}
vnpay.hash-secret=${VNPAY_HASH_SECRET:}
vnpay.pay-url=${VNPAY_PAY_URL:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}
vnpay.return-url=${VNPAY_RETURN_URL:}
```

Trên VPS cần đặt:

```text
CHATBOT_INTERNAL_TOKEN=<đúng cùng giá trị bên AI>
VNPAY_ENABLED=true
VNPAY_TMN_CODE=<mã website VNPay>
VNPAY_HASH_SECRET=<khóa bí mật VNPay>
VNPAY_RETURN_URL=https://cuong-smarthotel.me/booking/vnpay-return
```

Sau khi đổi biến môi trường phải khởi động lại cả backend và AI server. Một phản hồi
đúng từ chatbot phải chứa URL của cổng VNPay (sandbox hoặc production), không phải
`/booking/payment-vnpay?session=...`.
