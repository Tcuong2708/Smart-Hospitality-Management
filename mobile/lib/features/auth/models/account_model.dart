class AccountModel {
  final int? idTaiKhoan;
  final String tenDangNhap;
  final String matKhau;
  final String hoTen;
  final String email;
  final String soDienThoai;
  final String diaChi;
  final String quocTich;
  final int roleID;
  final bool trangThai;

  AccountModel({
    this.idTaiKhoan,
    required this.tenDangNhap,
    required this.matKhau,
    required this.hoTen,
    required this.email,
    required this.soDienThoai,
    required this.diaChi,
    required this.quocTich,
    this.roleID = 3,
    this.trangThai = true,
  });

  factory AccountModel.fromJson(Map<String, dynamic> json) {
    return AccountModel(
      idTaiKhoan: json['IDTaiKhoan'],
      tenDangNhap: json['TenDangNhap'] ?? '',
      matKhau: json['MatKhau'] ?? '',
      hoTen: json['HoTen'] ?? '',
      email: json['Email'] ?? '',
      soDienThoai: json['SoDienThoai'] ?? '',
      diaChi: json['DiaChi'] ?? '',
      quocTich: json['QuocTich'] ?? '',
      roleID: json['RoleID'] ?? 3,
      trangThai: json['TrangThai'] ?? true,
    );
  }

  Map<String, dynamic> toJson() => {
    "TenDangNhap": tenDangNhap,
    "MatKhau": matKhau,
    "HoTen": hoTen,
    "Email": email,
    "SoDienThoai": soDienThoai,
    "DiaChi": diaChi,
    "QuocTich": quocTich,
    "RoleID": roleID,
    "TrangThai": trangThai,
  };
}