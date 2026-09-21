import '../models/user_model.dart';

class AuthService {
  Future<UserModel?> login(String username, String password) async {
    // gọi DbAuthHelper ở đây
    // await Future.delayed(const Duration(seconds: 1)); // Giả lập độ trễ mạng

    if (username == "admin" && password == "admin123") {
      return UserModel(username: "Admin", role: "admin");
    } else if (username == "staff" && password == "staff123") {
      return UserModel(username: "Staff", role: "staff");
    } else if (username == "user" && password == "user123") {
      return UserModel(username: "customer", role: "customer");
    }
    return null; // Đăng nhập thất bại
  }
}