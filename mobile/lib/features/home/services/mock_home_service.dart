import '../models/room_model.dart';

class MockHomeService {
  Future<List<RoomModel>> fetchRooms() async {
    // Giả lập chờ mạng 1.5 giây
    await Future.delayed(const Duration(milliseconds: 1500));

    return [
      RoomModel(
        maPhong: 1, // Chuyển thành int
        tenPhong: "P101", // Đổi từ 'name' sang 'tenPhong'
        price: 500000.0,
        imageUrl: "assets/images/room1.jpg",
        detail: "Phòng đơn Standard - Đầy đủ tiện nghi cho khách lẻ.", // Đổi sang 'detail'
        maTrangThai: 1, // THÊM MỚI: Trạng thái Trống (Màu xanh)
      ),
      RoomModel(
        maPhong: 2,
        tenPhong: "P102",
        price: 950000.0,
        imageUrl: "assets/images/room2.jpg",
        detail: "Phòng Đôi Deluxe - View thành phố cực đẹp.",
        maTrangThai: 2, // THÊM MỚI: Trạng thái Đang ở (Màu đỏ)
      ),
      RoomModel(
        maPhong: 3,
        tenPhong: "P103",
        price: 2500000.0,
        imageUrl: "assets/images/room3.jpg",
        detail: "Phòng VIP May Hotel - Dịch vụ đặc quyền.",
        maTrangThai: 3, // THÊM MỚI: Trạng thái Dọn dẹp (Màu cam)
      ),
    ];
  }
}