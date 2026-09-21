import 'dart:io';
import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:dio/io.dart';
import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:may_hotel_app/features/admin/models/staff_model.dart';
import 'package:may_hotel_app/models/user_model.dart';
import '../models/room_model.dart';

class HotelApiProvider {
  late Dio _dio;

  HotelApiProvider() {
    // Đọc Base URL từ file môi trường cấu hình của hệ thống
    final String baseUrl = dotenv.env['BASE_URL'] ?? "https://localhost:44321";

    _dio = Dio(BaseOptions(
      baseUrl: baseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 10),
      headers: {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "ngrok-skip-browser-warning": "true",
      },
    ));

    // GIỮ NGUYÊN PHẦN BYPASS SSL KHI TEST LOCAL CỦA CƯỜNG
    (_dio.httpClientAdapter as IOHttpClientAdapter).createHttpClient = () {
      final client = HttpClient();
      client.badCertificateCallback = (X509Certificate cert, String host, int port) => true;
      return client;
    };

    debugPrint("May Hotel API Provider đang kết nối tới: $baseUrl");
  }

  // =========================================================================
  // PHÂN HỆ: QUẢN LÝ NHÂN SỰ ĐỒNG BỘ STAFF_MODEL
  // =========================================================================

  Future<List<StaffModel>> getStaffs() async {
    try {
      final response = await _dio.get("/api/admin/staff");
      if (response.statusCode == 200) {
        List data = response.data;
        return data.map((item) => StaffModel.fromJson(item)).toList();
      }
      return [];
    } catch (e) {
      _handleError("Lỗi tải danh sách nhân sự", e);
      return [];
    }
  }

  Future<bool> saveStaff(StaffModel staff, {required bool isEdit}) async {
    try {
      Response response;
      if (isEdit) {
        response = await _dio.put(
          "/api/admin/staff/${staff.maNV}",
          data: staff.toJson(),
        );
      } else {
        response = await _dio.post(
          "/api/admin/staff",
          data: staff.toJson(),
        );
      }
      return response.statusCode == 200;
    } catch (e) {
      _handleError("Lỗi xử lý lưu dữ liệu nhân sự", e);
      return false;
    }
  }

  // =========================================================================
  // PHÂN HỆ: QUẢN LÝ NGƯỜI DÙNG / KHÁCH HÀNG (ĐỒNG BỘ ĐƯỜNG DẪN CỦA CƯỜNG)
  // =========================================================================

  Future<List<UserModel>> getUsers() async {
    try {
      final response = await _dio.get("/api/admin/users");
      if (response.statusCode == 200) {
        List data = response.data;
        return data.map((item) => UserModel.fromJson(item)).toList();
      }
      return [];
    } catch (e) {
      _handleError("Lỗi tải danh sách người dùng", e);
      return [];
    }
  }

  Future<bool> saveUser(UserModel user, {required bool isEdit}) async {
    try {
      Response response;
      if (isEdit) {
        response = await _dio.put("/api/admin/users/${user.idTaiKhoan}", data: user.toJson());
      } else {
        response = await _dio.post("/api/admin/users", data: user.toJson());
      }
      return response.statusCode == 200;
    } catch (e) {
      _handleError("Lỗi lưu người dùng", e);
      return false;
    }
  }

  // =========================================================================
  // PHÂN HỆ: LỄ TÂN & RECEPTION CORES
  // =========================================================================

  Future<List<RoomModel>> getAllRooms() async {
    try {
      final response = await _dio.get("/api/v1/mobile/rooms");
      if (response.statusCode == 200) {
        List data = response.data['data'] ?? [];
        return data.map((json) => RoomModel.fromJson(json)).toList();
      }
      return [];
    } catch (e) {
      debugPrint("Lỗi tải phòng: $e");
      return [];
    }
  }

  Future<List<RoomModel>> getRoomTypes() async {
    try {
      final response = await _dio.get("/api/v1/mobile/room-types");
      if (response.statusCode == 200) {
        List data = response.data['data'] ?? [];
        return data.map((json) => RoomModel.fromJson(json)).toList();
      }
      return [];
    } catch (e) {
      debugPrint("Lỗi tải loại phòng: $e");
      return [];
    }
  }

  Future<bool> processCheckOut(int roomId, double surcharge) async {
    try {
      final response = await _dio.post(
        "/api/reception/checkout/$roomId",
        data: {"surcharge": surcharge},
      );
      return response.statusCode == 200;
    } catch (e) {
      _handleError("Lỗi Check-out", e);
      return false;
    }
  }

  Future<bool> updateRoomStatus(int roomId, int status) async {
    try {
      final response = await _dio.put(
        "/api/reception/update-status/$roomId",
        data: {"status": status},
      );
      return response.statusCode == 200;
    } catch (e) {
      _handleError("Lỗi Update Status", e);
      return false;
    }
  }

  Future<List<dynamic>> getTransactionHistory(int maKH) async {
    try {
      // API Java Mobile bắt buộc dùng số điện thoại thay vì maKH
      // Tạm thời truyền số điện thoại mặc định (sau này có thể móc từ AuthStateService)
      final response = await _dio.get("/api/v1/mobile/bookings/history", queryParameters: {"phone": "0123456789"});
      if (response.statusCode == 200) {
        return response.data['data'] ?? [];
      }
      return [];
    } catch (e) {
      _handleError("Lỗi bốc lịch sử phòng đặt", e);
      return [];
    }
  }

  Future<bool> postCheckIn({
    required int roomId,
    required String name,
    required String phone,
    required double price,
    required String checkInDate,
    required String checkOutDate,
    required double totalPrice,
  }) async {
    try {
      final response = await _dio.post(
        "/api/reception/checkin",
        data: {
          "roomId": roomId,
          "customerName": name,
          "customerPhone": phone,
          "price": price,
        },
      );
      return response.statusCode == 200;
    } catch (e) {
      _handleError("Lỗi Check-in", e);
      return false;
    }
  }

  Future<Response?> postCheckOut(int roomId, double surcharge) async {
    try {
      return await _dio.post(
        "/api/reception/checkout/$roomId",
        data: {"surcharge": surcharge},
      );
    } catch (e) {
      debugPrint("Lỗi Check-out API: $e");
      return null;
    }
  }

  // =========================================================================
  // PHÂN HỆ: BAN QUẢN TRỊ (CRUD ROOMS ĐỘC QUYỀN)
  // =========================================================================

  Future<List<RoomModel>> getAdminRooms() async {
    final res = await _dio.get("/api/admin/rooms");
    return (res.data as List).map((e) => RoomModel.fromJson(e)).toList();
  }

  Future<bool> addRoom(Map<String, dynamic> data) async {
    final res = await _dio.post("/api/admin/rooms", data: data);
    return res.statusCode == 200;
  }

  Future<bool> updateRoom(int id, Map<String, dynamic> data) async {
    final res = await _dio.put("/api/admin/rooms/$id", data: data);
    return res.statusCode == 200;
  }

  Future<bool> deleteRoom(int id) async {
    final res = await _dio.delete("/api/admin/rooms/$id");
    return res.statusCode == 200;
  }

  // =========================================================================
  // PHÂN HỆ: ĐĂNG KÝ & BẢO MẬT OTP HỆ THỐNG
  // =========================================================================

  Future<bool> register(Map<String, dynamic> data) async {
    try {
      debugPrint("📤 DỮ LIỆU GỬI ĐI ĐĂNG KÝ: $data");
      final response = await _dio.post("/api/auth/register", data: data);
      return response.statusCode == 200;
    } catch (e) {
      _handleError("Lỗi Đăng ký", e);
      return false;
    }
  }

  Future<bool> sendOTP(String email) async {
    try {
      final response = await _dio.post("/api/auth/send-otp", data: {"Email": email});
      return response.statusCode == 200;
    } catch (e) {
      _handleError("Lỗi gửi OTP", e);
      return false;
    }
  }

  Future<bool> verifyOTP(String email, String code) async {
    try {
      final response = await _dio.post("/api/auth/verify-otp", data: {"Email": email, "OTPCode": code});
      return response.statusCode == 200;
    } catch (e) {
      _handleError("Lỗi xác thực OTP", e);
      return false;
    }
  }

  // =========================================================================
  // PHÂN HỆ: LIÊN THÔNG CHATBOT AI & FACE MATCH
  // =========================================================================

  Future<String> sendChatMessage(String message) async {
    try {
      final response = await _dio.post(
        "/api/reception/chatbot",
        data: {"message": message},
      );
      if (response.statusCode == 200) {
        return response.data['reply'] ?? response.data['message'] ?? "Xin lỗi, tôi chưa hiểu ý bạn.";
      }
      return "Hệ thống đang bận, bạn vui lòng thử lại sau!";
    } catch (e) {
      _handleError("Lỗi kết nối Chatbot AI", e);
      return "Không thể kết nối đến trợ lý ảo. Vui lòng kiểm tra mạng!";
    }
  }

  // Gọi trực tiếp lên Python AI Server (FastAPI qua Ngrok)
  Future<Map<String, dynamic>?> verifyFaceNFC(File nfcImage, File selfieImage) async {
    try {
      // Đọc AI_URL từ môi trường, ví dụ: https://abcd.ngrok.io
      final String aiBaseUrl = dotenv.env['AI_URL'] ?? "http://localhost:8000";
      
      Dio aiDio = Dio(BaseOptions(baseUrl: aiBaseUrl, connectTimeout: const Duration(seconds: 30)));

      FormData formData = FormData.fromMap({
        "nfc_image": await MultipartFile.fromFile(nfcImage.path, filename: "nfc.jpg"),
        "selfie_image": await MultipartFile.fromFile(selfieImage.path, filename: "selfie.jpg"),
      });

      final response = await aiDio.post("/api/v1/ai/verify-face-nfc", data: formData);
      
      if (response.statusCode == 200) {
        return response.data;
      }
      return null;
    } catch (e) {
      debugPrint("Lỗi gọi AI Server verify-face-nfc: $e");
      return {"status": "error", "message": e.toString()};
    }
  }

  // =========================================================================
  // 🌟 🌟 🌟 ĐÃ CHUYỂN ĐỔI SANG DIO: PHÂN HỆ ĐẶT PHÒNG KHÁCH SẠN 🌟 🌟 🌟
  // =========================================================================

  Future<Map<String, dynamic>> createBooking({
    required int maPhong,
    required int maKH,
    required DateTime ngayNhanPhong,
    required DateTime ngayTraPhong,
    required int soNguoi,
    String? ghiChu,
  }) async {
    try {
      final body = {
        'maPhong': maPhong,
        'maKH': maKH,
        'ngayNhanPhong': ngayNhanPhong.toIso8601String(),
        'ngayTraPhong': ngayTraPhong.toIso8601String(),
        'soNguoi': soNguoi,
        'ghiChu': ghiChu,
      };

      print('📤 createBooking body gửi qua Dio: $body');
      final res = await _dio.post('/api/booking/add', data: body);
      print('📥 createBooking response từ C#: ${res.data}');

      if (res.statusCode == 200) {
        final data = res.data;
        return {
          'bookingId': data['maHD'] ?? 0,
          'totalAmount': (data['tongTien'] ?? 0).toDouble(),
          'success': data['success'] ?? true,
          'maPhong': maPhong,
          'soNgay': ngayTraPhong.difference(ngayNhanPhong).inDays,
        };
      }
      throw Exception('Create booking failed status code: ${res.statusCode}');
    } catch (e) {
      _handleError("Create booking error", e);
      throw Exception('Create booking error: $e');
    }
  }

  // =========================================================================
  // 🌟 🌟 🌟 ĐÃ CHUYỂN ĐỔI SANG DIO: PHÂN HỆ DỊCH VỤ (SERVICE) 🌟 🌟 🌟
  // =========================================================================

  Future<List<Map<String, dynamic>>> getAllServices() async {
    try {
      final res = await _dio.get('/api/booking/services');
      if (res.statusCode == 200) {
        final List<dynamic> data = res.data;
        return data.cast<Map<String, dynamic>>();
      }
      throw Exception('Get services failed status code: ${res.statusCode}');
    } catch (e) {
      _handleError("Get services error", e);
      throw Exception('Get services error: $e');
    }
  }

  Future<Map<String, dynamic>> addServiceToBooking({
    required int bookingId,
    required int maDichVu,
    required int soLuong,
    required double donGia,
  }) async {
    try {
      final body = {'maDichVu': maDichVu, 'soLuong': soLuong, 'donGia': donGia};

      print('📤 addService body gửi qua Dio: $body');
      final res = await _dio.post('/api/booking/$bookingId/service', data: body);
      print('📥 addService response từ C#: ${res.data}');

      if (res.statusCode == 200) {
        final data = res.data;
        return {
          'success': data['success'] ?? true,
          'message': data['message'] ?? 'Thêm dịch vụ thành công',
          'serviceAmount': (data['serviceAmount'] ?? 0).toDouble(),
          'totalAmount': (data['totalAmount'] ?? 0).toDouble(),
        };
      }
      throw Exception('Add service failed status code: ${res.statusCode}');
    } catch (e) {
      _handleError("Add service error", e);
      throw Exception('Add service error: $e');
    }
  }

  // =========================================================================
  // 🌟 🌟 🌟 ĐÃ CHUYỂN ĐỔI SANG DIO: PHÂN HỆ THANH TOÁN (PAYMENT) 🌟 🌟 🌟
  // =========================================================================

  Future<bool> confirmPayment(int bookingId, double totalAmount, int paymentMethodId) async {
    try {
      final body = {
        'totalAmount': totalAmount,
        'paymentMethodId': paymentMethodId,
      };

      print('confirmPayment body gửi qua Dio: $body');
      final res = await _dio.post('/api/booking/confirm-payment/$bookingId', data: body);
      print('confirmPayment response từ C#: ${res.data}');

      return res.statusCode == 200;
    } catch (e) {
      _handleError("Confirm payment error", e);
      return false;
    }
  }

  // =========================================================================
  // TẦNG GÁC CỔNG LOG: PHÂN TÍCH LỖI DIỆN RỘNG (SMART LOGGING)
  // =========================================================================
  void _handleError(String title, dynamic e) {
    if (e is DioException) {
      final responseData = e.response?.data;
      final statusCode = e.response?.statusCode;

      print("--- PHÂN TÍCH LỖI API MẠNG ---");
      print("Chức năng nghẽn: $title");
      print("Mã trạng thái phản hồi: $statusCode");

      if (responseData is String) {
        print("THÔNG BÁO TỪ SERVER JAVA: $responseData");
      } else if (responseData is Map) {
        print("MÃ LỖI JSON CHI TIẾT: $responseData");
        if (responseData.containsKey('message')) {
          print("Nội dung báo lỗi: ${responseData['message']}");
        }
      } else {
        print("Cấu trúc lỗi không xác định: $responseData");
      }
      print("--------------------------------");
    } else {
      debugPrint("LỖI HỆ THỐNG: ${e.toString()}");
    }
  }

  Future<Map<String, dynamic>?> getUserProfile(String username) async {
    try {
      // Server Java hiện tại chưa có API Profile cho Mobile, giả lập trả về thành công để tránh lỗi 404
      return {
        "success": true,
        "username": username,
        "fullName": "Khách hàng May Hotel",
        "phone": "0123456789",
        "email": "guest@mayhotel.com"
      };
    } catch (e) {
      debugPrint("Lỗi gọi API lấy Profile: $e");
      return null;
    }
  }

  Future<bool> updateUserProfile(String username, Map<String, dynamic> updateData) async {
    try {
      final bodyPayload = {
        "username": username,
        ...updateData,
      };

      print('📤 Dữ liệu cập nhật Profile gửi qua Dio: $bodyPayload');

      final response = await _dio.post("/api/auth/update-profile", data: bodyPayload);

      print('📥 Phản hồi cập nhật từ JAVA: ${response.data}');

      if (response.statusCode == 200) {
        return true;
      }
      return false;
    } catch (e) {
      // Gọi tầng xử lý lỗi thông minh để bốc tách log lỗi 400 rõ ràng
      _handleError("Lỗi gọi API cập nhật Profile", e);
      return false;
    }
  }

}