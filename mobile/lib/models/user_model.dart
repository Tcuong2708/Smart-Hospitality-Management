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
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      idTaiKhoan: json['IdTaiKhoan'] ?? json['idTaiKhoan'] ?? 0,
      tenDangNhap: json['TenDangNhap'] ?? json['tenDangNhap'] ?? '',
      matKhau: json['MatKhau'] ?? json['matKhau'] ?? '',
      hoTen: json['HoTen'] ?? json['hoTen'] ?? '',
      sdt: json['SDT'] ?? json['sdt'] ?? '',
      email: json['Email'] ?? json['email'] ?? '',
      diaChi: json['DiaChi'] ?? json['diaChi'] ?? '',
      quocTich: json['QuocTich'] ?? json['quocTich'] ?? '',
      trangThai: json['TrangThai'] ?? json['trangThai'] ?? true,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      "IdTaiKhoan": idTaiKhoan,
      "TenDangNhap": tenDangNhap,
      "MatKhau": matKhau,
      "HoTen": hoTen,
      "SDT": sdt,
      "Email": email,
      "DiaChi": diaChi,
      "QuocTich": quocTich,
      "TrangThai": trangThai,
    };
  }
}