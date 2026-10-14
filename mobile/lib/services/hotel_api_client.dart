import 'dart:io';

import 'package:dio/dio.dart';
import 'package:dio/io.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

class HotelApiClient {
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
        onRequest: (options, handler) {
          options.extra['requestStartedAt'] = DateTime.now();
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

  Dio get dio => _dio;

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
