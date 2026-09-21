class StaffModel {
  final int maNV;
  final String hoTen;
  final String sdt;
  final String tenDangNhap;
  final String matKhau;
  final String chucVu;
  final bool trangThai;

  StaffModel({
    required this.maNV,
    required this.hoTen,
    required this.sdt,
    required this.tenDangNhap,
    required this.matKhau,
    required this.chucVu,
    required this.trangThai,
  });


  factory StaffModel.fromJson(Map<String, dynamic> json) {
    return StaffModel(
      maNV: json['MaNV'] ?? json['maNV'] ?? 0,
      hoTen: json['HoTen'] ?? json['hoTen'] ?? '',
      sdt: json['SDT'] ?? json['sdt'] ?? '',
      tenDangNhap: json['TenDangNhap'] ?? json['tenDangNhap'] ?? '',
      matKhau: json['MatKhau'] ?? json['matKhau'] ?? '',
      chucVu: json['ChucVu'] ?? json['chucVu'] ?? 'Staff',
      trangThai: json['TrangThai'] ?? json['trangThai'] ?? true,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      "MaNV": maNV,
      "HoTen": hoTen,
      "SDT": sdt,
      "TenDangNhap": tenDangNhap,
      "MatKhau": matKhau,
      "ChucVu": chucVu,
      "TrangThai": trangThai,
    };
  }
}