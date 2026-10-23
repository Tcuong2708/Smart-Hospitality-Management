import 'package:dio/dio.dart';

import '../../../services/hotel_api_client.dart';
import '../models/housekeeping_room.dart';

class HousekeepingApiService {
  final Dio _dio = HotelApiClient.instance.dio;

  Future<HousekeepingRoomSnapshot> getRooms() async {
    final response = await _dio.get('/api/v1/mobile/housekeeping/rooms');
    final rawData = response.data is Map ? response.data['data'] : null;
    final rawRooms = rawData is Map ? rawData['rooms'] : rawData;
    final rooms = rawRooms is List ? rawRooms : const [];
    final parsedRooms = rooms
        .whereType<Map>()
        .map((item) => HousekeepingRoom.fromJson(Map<String, dynamic>.from(item)))
        .toList();
    final hotelDate = rawData is Map
        ? DateTime.tryParse(rawData['hotelDate']?.toString() ?? '')
        : DateTime.tryParse(response.headers.value('x-hotel-date') ?? '');
    final generatedAt = rawData is Map
        ? DateTime.tryParse(rawData['generatedAt']?.toString() ?? '')
        : DateTime.tryParse(response.headers.value('x-generated-at') ?? '');
    return HousekeepingRoomSnapshot(
      rooms: parsedRooms,
      hotelDate: hotelDate ?? DateTime.now(),
      generatedAt: generatedAt,
    );
  }

  Future<HousekeepingRoom> updateStatus({
    required HousekeepingRoom room,
    required String status,
    String? statusNote,
  }) async {
    final response = await _dio.patch(
      '/api/v1/mobile/housekeeping/rooms/${room.id}/status',
      data: {
        'expectedStatus': room.status,
        'status': status,
        'statusNote': statusNote,
      },
    );
    final rawData = response.data is Map ? response.data['data'] : null;
    if (rawData is! Map) {
      throw StateError('Server không trả về trạng thái phòng mới');
    }
    return HousekeepingRoom.fromJson(Map<String, dynamic>.from(rawData));
  }

  static String errorMessage(Object error) {
    if (error is DioException) {
      final data = error.response?.data;
      if (data is Map && data['message'] != null) return data['message'].toString();
      if (error.response?.statusCode == 401) return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
      if (error.response?.statusCode == 403) return 'Tài khoản không có quyền cập nhật phòng.';
      return 'Không thể kết nối máy chủ (${error.response?.statusCode ?? 'mất kết nối'}).';
    }
    return error.toString().replaceFirst('Exception: ', '');
  }
}
