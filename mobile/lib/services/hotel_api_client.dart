import 'dart:io';

import 'package:dio/dio.dart';
import 'package:dio/io.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class HotelApiClient {
  static const String _tokenKey = 'mobile_access_token';

  HotelApiClient._() {
    final baseUrl = dotenv.env['BASE_URL']?.trim();
    if (baseUrl == null || baseUrl.isEmpty) {
      throw StateError('BASE_URL chưa được cấu hình cho backend Java.');
    }

    _dio = Dio(
      BaseOptions(
        baseUrl: baseUrl,
        connectTimeout: const Duration(seconds: 10),
        receiveTimeout: const Duration(seconds: 10),
        sendTimeout: const Duration(seconds: 10),
        headers: const {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
        },
      ),
    );

    if (!kIsWeb && _dio.httpClientAdapter is IOHttpClientAdapter) {
      (_dio.httpClientAdapter as IOHttpClientAdapter).createHttpClient = () {
        final client = HttpClient();
        client.badCertificateCallback =
            (X509Certificate cert, String host, int port) => true;
        return client;
      };
    }

    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          options.extra['requestStartedAt'] = DateTime.now();
          _accessToken ??= await _secureStorage.read(key: _tokenKey);
          if (_accessToken != null && _accessToken!.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $_accessToken';
          }
          handler.next(options);
        },
        onResponse: (response, handler) {
          _logDuration(response.requestOptions, response.statusCode);
          handler.next(response);
        },
        onError: (error, handler) {
          _logDuration(
            error.requestOptions,
            error.response?.statusCode,
            failed: true,
          );
          handler.next(error);
        },
      ),
    );

    debugPrint('May Hotel API client kết nối tới backend: $baseUrl');
  }

  static final HotelApiClient instance = HotelApiClient._();

  late final Dio _dio;
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();
  String? _accessToken;

  Dio get dio => _dio;

  Future<void> setAccessToken(String token) async {
    _accessToken = token;
    await _secureStorage.write(key: _tokenKey, value: token);
  }

  Future<void> clearAccessToken() async {
    _accessToken = null;
    await _secureStorage.delete(key: _tokenKey);
  }

  void _logDuration(
    RequestOptions options,
    int? statusCode, {
    bool failed = false,
  }) {
    final startedAt = options.extra['requestStartedAt'];
    if (startedAt is! DateTime) return;

    final elapsed = DateTime.now().difference(startedAt).inMilliseconds;
    final outcome = failed ? 'failed' : 'completed';
    debugPrint(
      '[API] $outcome ${options.method} ${options.uri} '
      'status=${statusCode ?? '-'} duration=${elapsed}ms',
    );
  }
}
