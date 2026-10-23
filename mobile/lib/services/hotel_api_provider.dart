import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:may_hotel_app/features/admin/models/staff_model.dart';
import 'package:may_hotel_app/models/user_model.dart';
import '../models/room_model.dart';
import 'hotel_api_client.dart';
import 'room_search_cache.dart';

class HotelApiProvider {
  final Dio _dio = HotelApiClient.instance.dio;
  final String _chatSessionId =
      'mobile-${DateTime.now().microsecondsSinceEpoch}';
  static final Map<String, Future<List<dynamic>>> _inFlightSearches =
      <String, Future<List<dynamic>>>{};
  static Dio? _aiDio;
  static String? _aiDioBaseUrl;

  HotelApiProvider();

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
        response = await _dio.post("/api/admin/staff", data: staff.toJson());
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
      final response = await _dio.get("/api/v1/mobile/admin/users");
      if (response.statusCode == 200) {
        final payload = Map<String, dynamic>.from(response.data as Map);
        final List data = payload['data'] as List? ?? const [];
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
        response = await _dio.put(
          "/api/v1/mobile/admin/users/${user.idTaiKhoan}",
          data: user.toJson(),
        );
      } else {
        response = await _dio.post(
          "/api/v1/mobile/admin/users",
          data: user.toJson(),
        );
      }
      return response.statusCode == 200;
    } catch (e) {
      _handleError("Lỗi lưu người dùng", e);
      return false;
    }
  }

  Future<bool> toggleUserStatus(int userId) async {
    try {
      final response = await _dio.patch(
        "/api/v1/mobile/admin/users/$userId/status",
      );
      return response.statusCode == 200;
    } catch (e) {
      _handleError("Lỗi đổi trạng thái tài khoản", e);
      return false;
    }
  }

  Future<bool> deleteUser(int userId) async {
    try {
      final response = await _dio.delete("/api/v1/mobile/admin/users/$userId");
      return response.statusCode == 200;
    } catch (e) {
      _handleError("Lỗi xóa tài khoản", e);
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

  Future<List<dynamic>> getTransactionHistory(int userId) async {
    try {
      final response = await _dio.get(
        "/api/v1/mobile/booking/history",
        queryParameters: {"userId": userId},
      );
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
      final response = await _dio.post(
        "/api/v1/mobile/auth/register",
        data: data,
      );
      return response.statusCode == 200;
    } catch (e) {
      _handleError("Lỗi Đăng ký", e);
      return false;
    }
  }

  // ==========================================
  // API: TRA CỨU PHÒNG TRỐNG (PHẦN 2.1)
  // ==========================================
  Future<List<dynamic>> searchRooms(
    DateTime ngayNhan,
    DateTime ngayTra,
    int adults,
    int children, {
    CancelToken? cancelToken,
  }) async {
    final startDate = DateTime(ngayNhan.year, ngayNhan.month, ngayNhan.day);
    final endDate = DateTime(ngayTra.year, ngayTra.month, ngayTra.day);
    final cacheKey =
        '${_formatDate(startDate)}|${_formatDate(endDate)}|$adults|$children';
    final cachedResults = RoomSearchCache.instance.read(cacheKey);
    if (cachedResults != null) return cachedResults;

    final existingRequest = _inFlightSearches[cacheKey];
    if (existingRequest != null) return existingRequest;

    final request = _searchRoomsFromApi(
      startDate,
      endDate,
      adults,
      children,
      cacheKey,
      cancelToken,
    );
    _inFlightSearches[cacheKey] = request;

    try {
      return await request;
    } finally {
      if (identical(_inFlightSearches[cacheKey], request)) {
        _inFlightSearches.remove(cacheKey);
      }
    }
  }

  Future<List<dynamic>> _searchRoomsFromApi(
    DateTime ngayNhan,
    DateTime ngayTra,
    int adults,
    int children,
    String cacheKey,
    CancelToken? cancelToken,
  ) async {
    try {
      final response = await _dio.get(
        '/api/v1/mobile/rooms/search',
        queryParameters: {
          'ngayNhan': _formatApiDateTime(ngayNhan),
          'ngayTra': _formatApiDateTime(ngayTra),
          'adults': adults,
          'children': children,
        },
        cancelToken: cancelToken,
      );
      if (response.statusCode != 200) return [];

      final data = response.data;
      final results = data is Map && data['data'] is List
          ? List<dynamic>.from(data['data'] as List)
          : <dynamic>[];
      RoomSearchCache.instance.write(cacheKey, results);
      return results;
    } catch (e) {
      _handleError('Lỗi tra cứu phòng', e);
      return [];
    }
  }

  String _formatDate(DateTime date) {
    final month = date.month.toString().padLeft(2, '0');
    final day = date.day.toString().padLeft(2, '0');
    return '${date.year}-$month-$day';
  }

  String _formatApiDateTime(DateTime date) {
    return DateTime.utc(date.year, date.month, date.day).toIso8601String();
  }

  // ==========================================
  // API: CÁC LUỒNG MOBILE MỚI (2.2 - 2.6)
  // ==========================================

  Future<Map<String, dynamic>?> submitCheckInAI(
    int bookingId,
    String cccdData,
    int guestIndex, {
    required bool faceMatched,
    required double faceSimilarity,
  }) async {
    try {
      final response = await _dio.post(
        "/api/v1/mobile/booking/checkin",
        data: {
          "bookingId": bookingId,
          "cccdData": cccdData,
          "guestIndex": guestIndex,
          "faceMatched": faceMatched,
          "faceSimilarity": faceSimilarity,
        },
      );
      if (response.statusCode == 200) {
        return response.data['data'];
      }
      return null;
    } catch (e) {
      _handleError("Lỗi Check-in AI", e);
      return null;
    }
  }

  Future<bool> submitCheckOut(int bookingId) async {
    try {
      final response = await _dio.post(
        "/api/v1/mobile/booking/checkout",
        data: {"bookingId": bookingId},
      );
      return response.statusCode == 200;
    } catch (e) {
      _handleError("Lỗi Check-out", e);
      return false;
    }
  }

  Future<Map<String, dynamic>?> orderMobileService(
    int bookingId,
    int serviceId,
    int quantity,
  ) async {
    try {
      final response = await _dio.post(
        "/api/v1/mobile/services/order",
        data: {
          "bookingId": bookingId,
          "serviceId": serviceId,
          "quantity": quantity,
        },
      );
      if (response.statusCode == 200) {
        return response.data['data']; // Trả về mock hoá đơn 58mm
      }
      return null;
    } catch (e) {
      _handleError("Lỗi Đặt dịch vụ", e);
      return null;
    }
  }

  Future<Map<String, dynamic>?> orderMobileServices(
    int bookingId,
    List<Map<String, dynamic>> items,
  ) async {
    try {
      final response = await _dio.post(
        "/api/v1/mobile/services/order-batch",
        data: {"bookingId": bookingId, "items": items},
      );
      if (response.statusCode == 200 && response.data['success'] == true) {
        return Map<String, dynamic>.from(response.data['data'] as Map);
      }
      return null;
    } on DioException catch (e) {
      if (e.response?.statusCode == 404) {
        return _orderMobileServicesWithLegacyApi(bookingId, items);
      }
      _handleError("Lỗi đặt nhiều dịch vụ", e);
      throw HotelApiException(
        _extractApiMessage(e, "Không thể đặt các dịch vụ."),
      );
    } catch (e) {
      _handleError("Lỗi đặt nhiều dịch vụ", e);
      throw HotelApiException("Không thể đặt các dịch vụ: $e");
    }
  }

  Future<Map<String, dynamic>> _orderMobileServicesWithLegacyApi(
    int bookingId,
    List<Map<String, dynamic>> items,
  ) async {
    final receiptItems = <Map<String, dynamic>>[];
    double serviceTotal = 0;
    double invoiceTotal = 0;
    Map<String, dynamic>? receiptTemplate;

    for (var index = 0; index < items.length; index++) {
      final item = items[index];
      try {
        final response = await _dio.post(
          "/api/v1/mobile/services/order",
          data: {
            "bookingId": bookingId,
            "serviceId": item['serviceId'],
            "quantity": item['quantity'],
          },
        );
        final payload = Map<String, dynamic>.from(response.data as Map);
        if (response.statusCode != 200 || payload['success'] != true) {
          throw HotelApiException(
            payload['message']?.toString() ?? "Đặt dịch vụ thất bại.",
          );
        }

        final data = Map<String, dynamic>.from(payload['data'] as Map);
        receiptTemplate ??= data;
        final quantity = int.tryParse(data['quantity']?.toString() ?? '') ?? 0;
        final unitPrice =
            double.tryParse(data['unitPrice']?.toString() ?? '') ?? 0;
        final lineTotal =
            double.tryParse(data['totalPrice']?.toString() ?? '') ??
            unitPrice * quantity;
        serviceTotal += lineTotal;
        invoiceTotal =
            double.tryParse(data['totalAmount']?.toString() ?? '') ??
            invoiceTotal + lineTotal;
        receiptItems.add({
          'serviceId': item['serviceId'],
          'serviceName': data['serviceName']?.toString() ?? 'Dịch vụ',
          'quantity': quantity,
          'unitPrice': unitPrice,
          'lineTotal': lineTotal,
        });
      } on DioException catch (e) {
        final accepted = receiptItems.length;
        final prefix = accepted > 0
            ? "Đã ghi nhận $accepted/${items.length} dịch vụ. "
            : "";
        throw HotelApiException(
          "$prefix${_extractApiMessage(e, 'Không thể đặt dịch vụ.')}",
        );
      }
    }

    final template = receiptTemplate ?? const <String, dynamic>{};
    return {
      'title': template['title'] ?? 'HÓA ĐƠN DỊCH VỤ',
      'hotelName': template['hotelName'] ?? 'MAY HOTEL',
      'address': template['address'] ?? 'Tây Thạnh, TP.HCM',
      'hotline': template['hotline'] ?? '0123456789',
      'bookingId': bookingId,
      'items': receiptItems,
      'serviceTotal': serviceTotal,
      'invoiceTotal': invoiceTotal,
      'totalAmount': invoiceTotal,
      'message': template['message'] ?? 'Cảm ơn quý khách đã sử dụng dịch vụ!',
    };
  }

  String _extractApiMessage(dynamic error, String fallback) {
    if (error is HotelApiException) return error.message;
    if (error is DioException) {
      final data = error.response?.data;
      if (data is Map &&
          data['message']?.toString().trim().isNotEmpty == true) {
        return data['message'].toString();
      }
    }
    return fallback;
  }

  Future<List<dynamic>> getServices() async {
    try {
      final response = await _dio.get("/api/v1/mobile/services");
      if (response.statusCode == 200) {
        return response.data['data'] ?? [];
      }
      return [];
    } catch (e) {
      _handleError("Lỗi tải danh sách dịch vụ", e);
      return [];
    }
  }

  Future<bool> sendOTP(String email) async {
    try {
      final response = await _dio.post(
        "/api/v1/mobile/auth/send-otp",
        data: {"Email": email},
      );
      return response.statusCode == 200;
    } catch (e) {
      _handleError("Lỗi gửi OTP", e);
      return false;
    }
  }

  Future<bool> verifyOTP(String email, String code) async {
    try {
      final response = await _dio.post(
        "/api/v1/mobile/auth/verify-otp",
        data: {"Email": email, "OTPCode": code},
      );
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
        data: {"message": message, "user_id": _chatSessionId},
      );
      if (response.statusCode == 200) {
        return response.data['response'] ??
            response.data['reply'] ??
            response.data['answer'] ??
            response.data['message'] ??
            "Xin lỗi, tôi chưa hiểu ý bạn.";
      }
      return "Hệ thống đang bận, bạn vui lòng thử lại sau!";
    } catch (e) {
      _handleError("Lỗi kết nối Chatbot AI", e);
      return "Không thể kết nối đến trợ lý ảo. Vui lòng kiểm tra mạng!";
    }
  }

  // Gọi trực tiếp lên Python AI Server (FastAPI) để verify face (NFC)
  Future<Map<String, dynamic>?> verifyFaceNFC(
    File nfcImage,
    File selfieImage,
  ) async {
    try {
      final aiDio = _getAiDio();

      FormData formData = FormData.fromMap({
        "nfc_image": await MultipartFile.fromFile(
          nfcImage.path,
          filename: "nfc.jpg",
        ),
        "selfie_image": await MultipartFile.fromFile(
          selfieImage.path,
          filename: "selfie.jpg",
        ),
      });

      final response = await aiDio.post(
        "/api/v1/ai/verify-face-nfc",
        data: formData,
      );

      if (response.statusCode == 200) {
        return response.data;
      }
      return null;
    } catch (e) {
      debugPrint("Lỗi gọi AI Server verify-face-nfc: $e");
      return {"status": "error", "message": e.toString()};
    }
  }

  // Gọi API verify-cccd (YOLO + VietOCR + FaceMatch)
  Future<Map<String, dynamic>?> verifyCccdWithSelfie(
    File cccdImage,
    File selfieImage,
  ) async {
    try {
      final aiDio = _getAiDio();

      FormData formData = FormData.fromMap({
        "cccd_image": await MultipartFile.fromFile(
          cccdImage.path,
          filename: "cccd.jpg",
        ),
        "selfie_image": await MultipartFile.fromFile(
          selfieImage.path,
          filename: "selfie.jpg",
        ),
      });

      final response = await aiDio.post(
        "/api/v1/ai/verify-cccd",
        data: formData,
      );

      if (response.statusCode == 200) {
        return response.data;
      }
      return null;
    } catch (e) {
      debugPrint("Lỗi gọi AI Server verify-cccd: $e");
      return {"status": "error", "message": e.toString()};
    }
  }

  // =========================================================================
  // 🌟 🌟 🌟 ĐÃ CHUYỂN ĐỔI SANG DIO: PHÂN HỆ ĐẶT PHÒNG KHÁCH SẠN 🌟 🌟 🌟
  // =========================================================================

  Future<Map<String, dynamic>> createBooking({
    required int maPhong,
    required int userId,
    required DateTime ngayNhanPhong,
    required DateTime ngayTraPhong,
    required int soNguoi,
    String? ghiChu,
    String? customerName,
    String? customerPhone,
  }) async {
    try {
      final body = {
        'userId': userId,
        'roomTypeId': maPhong, // Mobile truyền ID loại phòng qua widget.maPhong
        // Gửi ngày thuần để không bị lùi một ngày khi đổi từ UTC+7 sang UTC.
        'ngayNhan': _formatDate(ngayNhanPhong),
        'ngayTra': _formatDate(ngayTraPhong),
        'soNguoi': soNguoi,
        'ghiChu': ghiChu,
        'customerName': customerName,
        'customerPhone': customerPhone,
      };

      print('📤 createBooking body gửi qua Dio: $body');
      final res = await _dio.post('/api/v1/mobile/booking/add', data: body);
      print('📥 createBooking response từ Java: ${res.data}');

      if (res.statusCode == 200) {
        final data = res
            .data['data']; // Vì backend trả về ApiResponse.success(..., data)
        RoomSearchCache.instance.invalidate();
        return {
          'bookingId': data['bookingId'] ?? 0,
          'totalAmount': (data['total'] ?? 0).toDouble(),
          'success': true,
          'maPhong': data['roomId'] ?? maPhong,
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
      final res = await _dio.get('/api/v1/mobile/services');
      if (res.statusCode == 200) {
        final Map<String, dynamic> responseData = res.data;
        if (responseData['data'] != null) {
          final List<dynamic> dataList = responseData['data'];
          return dataList.cast<Map<String, dynamic>>();
        }
        return [];
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
      final body = {
        'bookingId': bookingId,
        'serviceId': maDichVu,
        'quantity': soLuong,
        'price': donGia,
      };

      print('📤 addService body gửi qua Dio: $body');
      final res = await _dio.post('/api/v1/mobile/services/order', data: body);
      print('📥 addService response từ Java: ${res.data}');

      if (res.statusCode == 200) {
        final responseData = Map<String, dynamic>.from(res.data as Map);
        final data = responseData['data'] is Map
            ? Map<String, dynamic>.from(responseData['data'] as Map)
            : <String, dynamic>{};
        return {
          'success': responseData['success'] ?? true,
          'message': responseData['message'] ?? 'Thêm dịch vụ thành công',
          'serviceAmount':
              (data['totalPrice'] as num?)?.toDouble() ??
              (donGia * soLuong).toDouble(),
          'totalAmount': (data['totalAmount'] as num?)?.toDouble() ?? 0.0,
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

  Future<Map<String, dynamic>?> confirmPayment(
    int bookingId,
    double totalAmount,
    int paymentMethodId,
  ) async {
    try {
      final body = {
        'bookingId': bookingId,
        'totalAmount': totalAmount,
        'paymentMethodId': paymentMethodId,
      };

      print('confirmPayment body gửi qua Dio: $body');
      final res = await _dio.post(
        '/api/v1/mobile/booking/confirm-payment',
        data: body,
      );
      print('confirmPayment response từ Java: ${res.data}');

      if (res.statusCode == 200 && res.data is Map) {
        final envelope = Map<String, dynamic>.from(res.data as Map);
        return Map<String, dynamic>.from(
          envelope['data'] as Map? ?? const <String, dynamic>{},
        );
      }
      return null;
    } catch (e) {
      _handleError("Confirm payment error", e);
      return null;
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

  Future<Map<String, dynamic>?> getUserProfile(int userId) async {
    try {
      final response = await _dio.get("/api/v1/profile/$userId");
      if (response.statusCode == 200 && response.data is Map) {
        final payload = Map<String, dynamic>.from(response.data as Map);
        if (payload['data'] is Map) {
          return Map<String, dynamic>.from(payload['data'] as Map);
        }
      }
      return null;
    } catch (e) {
      debugPrint("Lỗi gọi API lấy Profile: $e");
      rethrow;
    }
  }

  Future<Map<String, dynamic>?> updateUserProfile(
    int userId,
    Map<String, dynamic> updateData,
  ) async {
    try {
      final response = await _dio.patch(
        "/api/v1/profile/$userId",
        data: updateData,
      );
      if (response.statusCode == 200 && response.data is Map) {
        final payload = Map<String, dynamic>.from(response.data as Map);
        if (payload['data'] is Map) {
          return Map<String, dynamic>.from(payload['data'] as Map);
        }
      }
      return null;
    } catch (e) {
      _handleError("Lỗi gọi API cập nhật Profile", e);
      rethrow;
    }
  }

  Future<Map<String, dynamic>?> getCheckInVerificationStatus(
    int bookingId,
  ) async {
    try {
      final response = await _dio.get(
        "/api/v1/mobile/booking/$bookingId/verification-status",
      );
      if (response.statusCode == 200 && response.data is Map) {
        final payload = Map<String, dynamic>.from(response.data as Map);
        if (payload['data'] is Map) {
          return Map<String, dynamic>.from(payload['data'] as Map);
        }
      }
      return null;
    } catch (e) {
      _handleError("Lỗi tải tiến độ xác thực nhận phòng", e);
      return null;
    }
  }

  Dio _getAiDio() {
    final configured = dotenv.env['AI_URL']?.trim();
    final baseUrl = configured == null || configured.isEmpty
        ? 'http://135.149.8.115:5000'
        : configured;
    if (_aiDio == null || _aiDioBaseUrl != baseUrl) {
      _aiDioBaseUrl = baseUrl;
      _aiDio = Dio(
        BaseOptions(
          baseUrl: baseUrl,
          connectTimeout: const Duration(seconds: 8),
          sendTimeout: const Duration(seconds: 25),
          receiveTimeout: const Duration(seconds: 45),
          responseType: ResponseType.json,
          headers: const {'Accept': 'application/json'},
        ),
      );
    }
    return _aiDio!;
  }
}

class HotelApiException implements Exception {
  final String message;

  const HotelApiException(this.message);

  @override
  String toString() => message;
}
