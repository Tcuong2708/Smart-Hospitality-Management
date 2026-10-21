import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import '../../../services/hotel_api_client.dart';
import '../models/app_role.dart';

class AuthApiService {
  final Dio _dio = HotelApiClient.instance.dio;

  /// 1. Đăng nhập qua API
  /// Trả về một Map chứa: { 'id', 'name', 'role' } nếu thành công
  Future<Map<String, dynamic>?> login(String username, String password) async {
    try {
      final response = await _dio.post(
        "/api/v1/mobile/auth/login",
        data: {"TenDangNhap": username, "MatKhau": password},
      );

      if (response.statusCode == 200 && response.data != null) {
        final data = response.data;
        final accessToken = data['accessToken']?.toString();
        if (accessToken != null && accessToken.isNotEmpty) {
          await HotelApiClient.instance.setAccessToken(accessToken);
        } else {
          await HotelApiClient.instance.clearAccessToken();
        }
        // Map RoleID thành chuỗi vai trò tương ứng trong Flutter App
        // RoleID = 1: admin, 2: staff, 3: customer
        final roleStr = _mapRole(data);

        return {
          "id": data['id'],
          "name": data['name'] ?? username,
          "role": roleStr,
          "accessToken": accessToken,
        };
      }
      return null;
    } catch (e) {
      debugPrint("❌ Lỗi Đăng nhập API: ${_parseError(e)}");
      rethrow;
    }
  }

  /// 2. Quên mật khẩu - Gửi mã OTP
  Future<bool> sendForgotPasswordOtp(String email) async {
    try {
      final response = await _dio.post(
        "/api/v1/mobile/auth/forgot-password",
        queryParameters: {"email": email},
      );
      return response.statusCode == 200;
    } catch (e) {
      debugPrint("❌ Lỗi Gửi OTP Đặt lại mật khẩu: ${_parseError(e)}");
      rethrow;
    }
  }

  /// 3. Xác thực OTP & Đặt lại mật khẩu mới
  Future<bool> resetPassword(
    String email,
    String otpCode,
    String newPassword,
  ) async {
    try {
      final response = await _dio.post(
        "/api/v1/mobile/auth/reset-password",
        data: {"Email": email, "OTPCode": otpCode, "NewPassword": newPassword},
      );
      return response.statusCode == 200;
    } catch (e) {
      debugPrint("❌ Lỗi Đặt lại mật khẩu: ${_parseError(e)}");
      rethrow;
    }
  }

  /// 4. Lấy dữ liệu báo cáo thống kê
  Future<Map<String, dynamic>> getStatsSummary() async {
    try {
      final response = await _dio.get("/api/stats/summary");
      if (response.statusCode == 200 && response.data != null) {
        return Map<String, dynamic>.from(response.data);
      }
      throw Exception("Không thể tải dữ liệu thống kê từ Server");
    } catch (e) {
      debugPrint("❌ Lỗi tải dữ liệu thống kê: ${_parseError(e)}");
      rethrow;
    }
  }

  /// Phân tích và trích xuất lỗi cụ thể từ response của Server
  String _parseError(dynamic e) {
    if (e is DioException) {
      if (e.response != null) {
        final data = e.response!.data;
        if (data is String) return data;
        if (data is Map && data.containsKey('message')) {
          return data['message'].toString();
        }
        return "Lỗi server (${e.response!.statusCode})";
      }
      return "Không thể kết nối đến máy chủ (${e.message})";
    }
    return e.toString();
  }

  /// 5. Lấy thông tin chi tiết tài khoản của người dùng (Profile)
  Future<Map<String, dynamic>?> getUserProfile(String username) async {
    try {
      final response = await _dio.get(
        "/api/auth/profile",
        queryParameters: {"username": username},
      );
      if (response.statusCode == 200 && response.data != null) {
        return Map<String, dynamic>.from(response.data);
      }
      return null;
    } catch (e) {
      debugPrint("❌ Lỗi gọi API lấy Profile: ${_parseError(e)}");
      rethrow;
    }
  }

  /// 6. Ghi đè cập nhật thông tin cá nhân lên SQL Server
  Future<bool> updateUserProfile({
    required String username,
    required String hoTen,
    required String email,
    required String soDienThoai,
    required String ngaySinh,
    required String gioiTinh,
  }) async {
    try {
      final response = await _dio.post(
        "/api/auth/update-profile",
        data: {
          "TenDangNhap": username,
          "HoTen": hoTen,
          "Email": email,
          "SoDienThoai": soDienThoai,
          "NgaySinh": ngaySinh,
          "GioiTinh": gioiTinh,
        },
      );
      return response.statusCode == 200;
    } catch (e) {
      debugPrint("❌ Lỗi gọi API cập nhật Profile: ${_parseError(e)}");
      rethrow;
    }
  }

  /// 7. Hủy và xóa tài khoản vĩnh viễn khỏi SQL Server (Danger Zone)
  Future<bool> deleteAccount(String username) async {
    try {
      final response = await _dio.delete(
        "/api/auth/delete-account",
        queryParameters: {
          "username":
              username, // Truyền tham số qua URL trùng khớp với C# Web API
        },
      );

      // Trả về true nếu xóa thành công (HTTP Status 200 OK)
      return response.statusCode == 200;
    } catch (e) {
      debugPrint("❌ Lỗi gọi API xóa tài khoản vĩnh viễn: ${_parseError(e)}");
      rethrow;
    }
  }

  /// 8. Đăng nhập hoặc Tự động đăng ký qua Google OAuth2 Token
  Future<Map<String, dynamic>?> loginWithGoogle(String idToken) async {
    try {
      final response = await _dio.post(
        "/api/v1/mobile/auth/google-login",
        data: {
          "IdToken": idToken, // Gửi chuỗi Token mã hóa của Google
        },
      );

      if (response.statusCode == 200 && response.data != null) {
        final data = response.data;
        final accessToken = data['accessToken']?.toString();
        if (accessToken != null && accessToken.isNotEmpty) {
          await HotelApiClient.instance.setAccessToken(accessToken);
        } else {
          await HotelApiClient.instance.clearAccessToken();
        }
        final roleStr = _mapRole(data);

        return {
          "id": data['id'],
          "name": data['name'],
          "role": roleStr,
          "accessToken": accessToken,
        };
      }
      return null;
    } catch (e) {
      debugPrint("❌ Lỗi gọi API Google Login: ${_parseError(e)}");
      rethrow;
    }
  }

  String _mapRole(dynamic data) {
    return AppRole.fromApi(
      roleCode: data['roleCode'],
      roleId: data['roleId'] ?? data['role'],
    ).storageValue;
  }
}
