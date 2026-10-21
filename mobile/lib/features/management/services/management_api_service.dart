import 'package:dio/dio.dart';

import '../../../services/hotel_api_client.dart';

class ManagementApiService {
  final Dio _dio = HotelApiClient.instance.dio;

  Future<Map<String, dynamic>> getManagerDashboard() =>
      _getMap('/api/v1/mobile/manager/dashboard');

  Future<List<Map<String, dynamic>>> getManagerResource(String resource) async {
    final response = await _dio.get(
      '/api/v1/mobile/manager/resources/$resource',
    );
    final payload = Map<String, dynamic>.from(response.data as Map);
    final items = payload['data'] as List? ?? const [];
    return items
        .whereType<Map>()
        .map((item) => Map<String, dynamic>.from(item))
        .toList();
  }

  Future<Map<String, dynamic>> getAdminDashboard() =>
      _getMap('/api/v1/mobile/admin/dashboard');

  Future<Map<String, dynamic>> getBackupStatus() =>
      _getMap('/api/v1/mobile/admin/backup-status');

  Future<void> saveManagerResource(
    String resource,
    Map<String, dynamic> data, {
    int? id,
  }) async {
    final path = '/api/v1/mobile/manager/$resource${id == null ? '' : '/$id'}';
    if (resource == 'reviews') {
      if (id == null) throw ArgumentError('Đánh giá phải có mã bản ghi.');
      await _dio.patch(path, data: data);
    } else if (id == null) {
      await _dio.post(path, data: data);
    } else {
      await _dio.put(path, data: data);
    }
  }

  Future<void> deleteManagerResource(String resource, int id) async {
    await _dio.delete('/api/v1/mobile/manager/$resource/$id');
  }

  Future<Map<String, dynamic>> getLoyaltyPolicy() =>
      _getMap('/api/v1/mobile/manager/loyalty-policy');

  Future<void> updateLoyaltyPolicy(Map<String, dynamic> data) async {
    await _dio.put('/api/v1/mobile/manager/loyalty-policy', data: data);
  }

  Future<Map<String, dynamic>> _getMap(String path) async {
    final response = await _dio.get(path);
    final payload = Map<String, dynamic>.from(response.data as Map);
    return Map<String, dynamic>.from(payload['data'] as Map? ?? const {});
  }
}
