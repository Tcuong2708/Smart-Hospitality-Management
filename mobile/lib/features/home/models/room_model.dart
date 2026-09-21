class RoomModel {
  final int maPhong;
  final String tenPhong;
  final double price;
  final String imageUrl;
  final String detail;
  final int maTrangThai;

  RoomModel({
    required this.maPhong,
    required this.tenPhong,
    required this.price,
    required this.imageUrl,
    required this.detail,
    required this.maTrangThai,
  });

  factory RoomModel.fromJson(Map<String, dynamic> json) {
    return RoomModel(
      maPhong: json['id'] ?? 0,
      tenPhong: (json['name'] ?? "N/A").toString(),
      // Dùng double.tryParse để tránh lỗi định dạng số từ SQL
      price: double.tryParse(json['price']?.toString() ?? '0') ?? 0.0,
      maTrangThai: json['maTrangThai'] ?? 1,
      imageUrl: (json['imageUrl'] ?? "").toString(),
      detail: (json['detail'] ?? "").toString(),
    );
  }
}