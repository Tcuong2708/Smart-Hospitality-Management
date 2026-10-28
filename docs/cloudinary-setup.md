# Cấu hình Cloudinary

Ứng dụng chỉ bật upload ảnh khi đủ bốn cấu hình sau. Không lưu giá trị thật trong Git.

## Máy phát triển Windows

Thêm vào tệp ngoài repository đang được `spring.config.import` trỏ tới, ví dụ
`D:/Document/KhoaLuanCuNhan/Projects/application.properties`:

```properties
app.features.cloudinary.enabled=true
cloudinary.cloud-name=TEN_CLOUD
cloudinary.api-key=API_KEY
cloudinary.api-secret=API_SECRET
```

Khởi động lại backend sau khi cập nhật cấu hình.

## VPS

Ưu tiên cấp biến môi trường cho tiến trình Spring Boot:

```text
CLOUDINARY_ENABLED=true
CLOUDINARY_CLOUD_NAME=TEN_CLOUD
CLOUDINARY_API_KEY=API_KEY
CLOUDINARY_API_SECRET=API_SECRET
```

Không đưa khóa vào image Docker, tệp JAR, systemd unit được commit hoặc lịch sử shell.
Nếu dùng systemd, đặt biến trong một `EnvironmentFile` chỉ tài khoản dịch vụ đọc được,
sau đó khởi động lại dịch vụ.

## Phạm vi lưu trữ

- Ảnh phòng: `may_hotel/rooms`
- Ảnh loại phòng: `may_hotel/categories`
- Định dạng: JPEG, PNG, GIF, WebP
- Dung lượng tối đa: 5 MB

Khi thay ảnh, hệ thống chỉ xóa ảnh cũ sau khi MongoDB lưu URL mới thành công. Nếu lưu
MongoDB thất bại, ảnh vừa tải lên được xóa để tránh tệp rác.
