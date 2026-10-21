class UserModel {
  final int idTaiKhoan;
  final String tenDangNhap;
  final String matKhau;
  final String hoTen;
  final String sdt;
  final String email;
  final String diaChi;
  final String quocTich;
  final bool trangThai;
  final int roleId;

  UserModel({
    required this.idTaiKhoan,
    required this.tenDangNhap,
    required this.matKhau,
    required this.hoTen,
    required this.sdt,
    required this.email,
    required this.diaChi,
    required this.quocTich,
    required this.trangThai,
    this.roleId = 5,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      idTaiKhoan: json['id'] ?? json['IdTaiKhoan'] ?? json['idTaiKhoan'] ?? 0,
      tenDangNhap:
          json['username'] ?? json['TenDangNhap'] ?? json['tenDangNhap'] ?? '',
      matKhau: '',
      hoTen: json['fullName'] ?? json['HoTen'] ?? json['hoTen'] ?? '',
      sdt: json['phone'] ?? json['SDT'] ?? json['sdt'] ?? '',
      email: json['email'] ?? json['Email'] ?? '',
      diaChi: json['address'] ?? json['DiaChi'] ?? json['diaChi'] ?? '',
      quocTich:
          json['nationality'] ?? json['QuocTich'] ?? json['quocTich'] ?? '',
      trangThai: _isActive(
        json['status'] ?? json['TrangThai'] ?? json['trangThai'],
      ),
      roleId: int.tryParse((json['roleId'] ?? 5).toString()) ?? 5,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      "id": idTaiKhoan,
      "username": tenDangNhap,
      if (matKhau.isNotEmpty) "password": matKhau,
      "fullName": hoTen,
      "phone": sdt,
      "email": email,
      "address": diaChi,
      "nationality": quocTich,
      "status": trangThai ? "Hoạt động" : "Bị khóa",
      "roleId": roleId,
    };
  }

  static bool _isActive(dynamic value) {
    if (value is bool) return value;
    final normalized = value?.toString().trim().toLowerCase();
    return normalized == null ||
        normalized.isEmpty ||
        normalized == 'hoạt động' ||
        normalized == 'active' ||
        normalized == 'true';
  }
}
