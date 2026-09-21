class BookingModel {
  final int maHD;
  final DateTime ngayNhan;
  final DateTime ngayTra;
  final String hoTen;
  final String dienThoai;
  final double tongTien;
  final bool daThanhToan;
  final int? soDem; // Thuộc tính NotMapped từ C#

  BookingModel({
    required this.maHD,
    required this.ngayNhan,
    required this.ngayTra,
    required this.hoTen,
    required this.dienThoai,
    required this.tongTien,
    required this.daThanhToan,
    this.soDem,
  });

  factory BookingModel.fromJson(Map<String, dynamic> json) {
    return BookingModel(
      maHD: json['maHD'],
      ngayNhan: DateTime.parse(json['ngayNhan']),
      ngayTra: DateTime.parse(json['ngayTra']),
      hoTen: json['hoTen'] ?? "",
      dienThoai: json['dienThoai'] ?? "",
      tongTien: (json['tongTien'] as num?)?.toDouble() ?? 0.0,
      daThanhToan: json['daThanhToan'] ?? false,
      soDem: json['soDem'],
    );
  }
}