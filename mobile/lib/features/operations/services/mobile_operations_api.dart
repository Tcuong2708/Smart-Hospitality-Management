import 'dart:io';

import 'package:dio/dio.dart';

import '../../../services/hotel_api_client.dart';

class MobileOperationsApi {
  final Dio _dio = HotelApiClient.instance.dio;

  Future<Map<String, dynamic>> getLoyalty() =>
      _getMap('/api/v1/mobile/operations/customer/loyalty');

  Future<Map<String, dynamic>> previewCancellation(int bookingId) async {
    return _postMap(
      '/api/v1/mobile/operations/customer/bookings/$bookingId/cancel',
      {'confirmed': false},
    );
  }

  Future<Map<String, dynamic>> cancelBooking(
    int bookingId, {
    String? reason,
  }) async {
    return _postMap(
      '/api/v1/mobile/operations/customer/bookings/$bookingId/cancel',
      {'confirmed': true, 'reason': reason},
    );
  }

  Future<void> submitReview(
    int bookingId, {
    required int rating,
    String? content,
  }) async {
    await _dio.post(
      '/api/v1/mobile/operations/customer/bookings/$bookingId/reviews',
      data: {'rating': rating, 'content': content},
    );
  }

  Future<List<Map<String, dynamic>>> getReceptionBookings({
    required bool active,
  }) => _getList(
    '/api/v1/mobile/operations/reception/bookings',
    query: {'view': active ? 'active' : 'pending'},
  );

  Future<List<Map<String, dynamic>>> getReceptionRoomTypes() =>
      _getList('/api/v1/mobile/operations/reception/room-types');

  Future<List<Map<String, dynamic>>> getReceptionAvailableRooms({
    required int roomTypeId,
    required DateTime checkIn,
    required DateTime checkOut,
  }) => _getList(
    '/api/v1/mobile/operations/reception/available-rooms',
    query: {
      'roomTypeId': roomTypeId,
      'checkIn': _date(checkIn),
      'checkOut': _date(checkOut),
    },
  );

  Future<Map<String, dynamic>> createDirectBooking({
    required String guestName,
    required String phone,
    required int roomId,
    required DateTime checkIn,
    required DateTime checkOut,
    required int guestCount,
    String? note,
  }) => _postMap('/api/v1/mobile/operations/reception/direct-bookings', {
    'guestName': guestName,
    'phone': phone,
    'roomId': roomId,
    'checkIn': _date(checkIn),
    'checkOut': _date(checkOut),
    'guestCount': guestCount,
    'note': note,
  });

  Future<List<Map<String, dynamic>>> getAvailableRooms(
    int bookingId,
  ) => _getList(
    '/api/v1/mobile/operations/reception/bookings/$bookingId/available-rooms',
  );

  Future<List<Map<String, dynamic>>> getChangeRoomOptions(
    int bookingId,
  ) => _getList(
    '/api/v1/mobile/operations/reception/bookings/$bookingId/change-room-options',
  );

  Future<void> assignRoom(int bookingId, int roomId) async {
    await _dio.post(
      '/api/v1/mobile/operations/reception/bookings/$bookingId/assign-room',
      data: {'roomId': roomId},
    );
  }

  Future<Map<String, dynamic>> submitManualVerification(
    int bookingId, {
    required int guestIndex,
    required String fullName,
    required String documentNumber,
    required String nationality,
    DateTime? dob,
    required String reason,
  }) => _postMap(
    '/api/v1/mobile/operations/reception/bookings/$bookingId/manual-verification',
    {
      'guestIndex': guestIndex,
      'fullName': fullName,
      'documentNumber': documentNumber,
      'nationality': nationality,
      'dob': _date(dob),
      'reason': reason,
    },
  );

  Future<void> checkout(
    int bookingId, {
    double surcharge = 0,
    String? damageNote,
  }) async {
    await _dio.post(
      '/api/v1/mobile/operations/reception/bookings/$bookingId/checkout',
      data: {'surcharge': surcharge, 'damageNote': damageNote},
    );
  }

  Future<void> changeRoom(
    int bookingId, {
    required int roomId,
    required String reason,
  }) async {
    await _dio.post(
      '/api/v1/mobile/operations/reception/bookings/$bookingId/change-room',
      data: {'roomId': roomId, 'reason': reason},
    );
  }

  Future<List<Map<String, dynamic>>> getInvoices({
    String? query,
    String? status,
  }) => _getList(
    '/api/v1/mobile/operations/reception/invoices',
    query: {'query': query, 'status': status},
  );

  Future<Map<String, dynamic>> payCash(
    int invoiceId, {
    required double amountReceived,
    required bool earnPoints,
  }) => _postMap(
    '/api/v1/mobile/operations/reception/invoices/$invoiceId/cash-payment',
    {'amountReceived': amountReceived, 'earnPoints': earnPoints},
  );

  Future<List<Map<String, dynamic>>> getNoShowBookings() =>
      _getList('/api/v1/mobile/operations/reception/no-show');

  Future<void> approveLateArrival(int bookingId, String reason) async {
    await _dio.patch(
      '/api/v1/mobile/operations/reception/bookings/$bookingId/late-arrival',
      data: {'reason': reason},
    );
  }

  Future<Map<String, dynamic>> getRevenueReport({
    DateTime? from,
    DateTime? to,
  }) => _getMap(
    '/api/v1/mobile/operations/reports/revenue',
    query: {'from': _date(from), 'to': _date(to)},
  );

  Future<File> downloadRevenueCsv({DateTime? from, DateTime? to}) async {
    final response = await _dio.get<List<int>>(
      '/api/v1/mobile/operations/reports/revenue.csv',
      queryParameters: {'from': _date(from), 'to': _date(to)},
      options: Options(responseType: ResponseType.bytes),
    );
    final suffix = DateTime.now().millisecondsSinceEpoch;
    final file = File(
      '${Directory.systemTemp.path}/may-hotel-revenue-$suffix.csv',
    );
    await file.writeAsBytes(response.data ?? const <int>[]);
    return file;
  }

  Future<Map<String, dynamic>> _getMap(
    String path, {
    Map<String, dynamic>? query,
  }) async {
    final response = await _dio.get(path, queryParameters: _clean(query));
    final envelope = Map<String, dynamic>.from(response.data as Map);
    return Map<String, dynamic>.from(envelope['data'] as Map? ?? const {});
  }

  Future<Map<String, dynamic>> _postMap(
    String path,
    Map<String, dynamic> data,
  ) async {
    final response = await _dio.post(path, data: data);
    final envelope = Map<String, dynamic>.from(response.data as Map);
    return Map<String, dynamic>.from(envelope['data'] as Map? ?? const {});
  }

  Future<List<Map<String, dynamic>>> _getList(
    String path, {
    Map<String, dynamic>? query,
  }) async {
    final response = await _dio.get(path, queryParameters: _clean(query));
    final envelope = Map<String, dynamic>.from(response.data as Map);
    final values = envelope['data'] as List? ?? const [];
    return values
        .whereType<Map>()
        .map((value) => Map<String, dynamic>.from(value))
        .toList();
  }

  Map<String, dynamic>? _clean(Map<String, dynamic>? values) {
    if (values == null) return null;
    return Map<String, dynamic>.fromEntries(
      values.entries.where((entry) => entry.value != null && entry.value != ''),
    );
  }

  String? _date(DateTime? value) {
    if (value == null) return null;
    return '${value.year.toString().padLeft(4, '0')}-'
        '${value.month.toString().padLeft(2, '0')}-'
        '${value.day.toString().padLeft(2, '0')}';
  }
}
