import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

class NotificationService {
  static final NotificationService _instance = NotificationService._internal();
  factory NotificationService() => _instance;
  NotificationService._internal();

  final FlutterLocalNotificationsPlugin _notificationsPlugin =
      FlutterLocalNotificationsPlugin();
  bool _initialized = false;

  Future<void> initNotification() async {
    if (_initialized) return;

    try {
      const AndroidInitializationSettings initializationSettingsAndroid =
          AndroidInitializationSettings('@mipmap/ic_launcher');

      const InitializationSettings initializationSettings =
          InitializationSettings(android: initializationSettingsAndroid);

      await _notificationsPlugin.initialize(settings: initializationSettings);
      _initialized = true;

      await _notificationsPlugin
          .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin
          >()
          ?.requestNotificationsPermission();
    } catch (error, stackTrace) {
      debugPrint('[Notification] Không thể khởi tạo: $error');
      debugPrintStack(stackTrace: stackTrace);
    }
  }

  Future<void> showBookingNotification({
    required int id,
    required String hotelName,
    required String roomType,
  }) async {
    if (!_initialized) {
      await initNotification();
    }
    if (!_initialized) return;

    const AndroidNotificationDetails androidPlatformChannelSpecifics =
        AndroidNotificationDetails(
          'may_hotel_booking_channel',
          'Đặt Phòng May Hotel',
          channelDescription:
              'Thông báo xác nhận trạng thái đặt phòng khách sạn',
          importance: Importance.max,
          priority: Priority.high,
          playSound: true,
        );

    const NotificationDetails platformChannelSpecifics = NotificationDetails(
      android: androidPlatformChannelSpecifics,
    );

    try {
      await _notificationsPlugin.show(
        id: id,
        title: '🎉 Đặt phòng thành công!',
        body:
            'Cảm ơn bạn đã chọn $hotelName. Phòng $roomType của bạn đã được xác nhận.',
        notificationDetails: platformChannelSpecifics,
      );
    } catch (error, stackTrace) {
      debugPrint('[Notification] Không thể hiển thị thông báo: $error');
      debugPrintStack(stackTrace: stackTrace);
    }
  }
}
