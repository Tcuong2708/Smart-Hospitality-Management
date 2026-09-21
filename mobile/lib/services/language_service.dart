import 'package:flutter/material.dart';

enum AppLanguage { vi, en }

class LanguageService extends ChangeNotifier {
  static final LanguageService _instance = LanguageService._internal();
  factory LanguageService() => _instance;
  LanguageService._internal();

  AppLanguage _currentLanguage = AppLanguage.vi;

  AppLanguage get currentLanguage => _currentLanguage;

  void changeLanguage(AppLanguage lang) {
    _currentLanguage = lang;
    notifyListeners();
  }

  // Từ điển dịch thuật mini (Bản thô để demo toàn app)
  String translate(String key) {
    final translations = {
      // Common
      'settings': {'vi': 'Cài đặt', 'en': 'Settings'},
      'language': {'vi': 'Ngôn ngữ', 'en': 'Language'},
      'dark_mode': {'vi': 'Chế độ tối', 'en': 'Dark Mode'},
      'check_update': {'vi': 'Kiểm tra bản cập nhật', 'en': 'Check for Updates'},
      'logout': {'vi': 'Đăng xuất', 'en': 'Logout'},
      'login': {'vi': 'Đăng nhập', 'en': 'Login'},
      'guest': {'vi': 'Khách', 'en': 'Guest'},
      'profile': {'vi': 'Hồ sơ', 'en': 'Profile'},
      'home': {'vi': 'Trang chủ', 'en': 'Home'},
      'booked': {'vi': 'Đã đặt', 'en': 'Booked'},
      'discovery': {'vi': 'Khám phá', 'en': 'Discovery'},
      'chatbot': {'vi': 'ChatBot', 'en': 'ChatBot'},
      'me': {'vi': 'Tôi', 'en': 'Me'},
      'statistics': {'vi': 'Thống kê', 'en': 'Statistics'},
      'rooms': {'vi': 'Phòng', 'en': 'Rooms'},
      'admin': {'vi': 'Quản trị', 'en': 'Admin'},
      'personnel': {'vi': 'Nhân sự', 'en': 'Personnel'},
      'map': {'vi': 'Sơ đồ', 'en': 'Map'},
      'checkin': {'vi': 'Check-in', 'en': 'Check-in'},
      'services': {'vi': 'Dịch vụ', 'en': 'Services'},
      'internal': {'vi': 'Nội bộ', 'en': 'Internal'},
      
      // Settings specific
      'interface': {'vi': 'Giao diện', 'en': 'Interface'},
      'security_biometric': {'vi': 'Bảo mật & Sinh trắc học', 'en': 'Security & Biometrics'},
      'app_language': {'vi': 'Ngôn ngữ ứng dụng', 'en': 'App Language'},
      'danger_zone': {'vi': 'Vùng nguy hiểm', 'en': 'Danger Zone'},
      'delete_account': {'vi': 'Xóa tài khoản vĩnh viễn', 'en': 'Delete Account Permanently'},
      
      // Home specific
      'popular_hotel': {'vi': 'Phòng phổ biến', 'en': 'Popular Rooms'},
      'view_all': {'vi': 'Xem tất cả', 'en': 'View All'},
      'search_hint': {'vi': 'Tìm theo tên phòng...', 'en': 'Search by room name...'},
      'morning': {'vi': 'Chào buổi sáng 🌅', 'en': 'Good Morning 🌅'},
      'afternoon': {'vi': 'Chào buổi chiều ☀️', 'en': 'Good Afternoon ☀️'},
      'evening': {'vi': 'Chào buổi tối 🌙', 'en': 'Good Evening 🌙'},
      'not_logged_in': {'vi': 'Chưa đăng nhập', 'en': 'Not Logged In'},
      'tap_to_login': {'vi': 'Nhấn để đăng nhập tài khoản', 'en': 'Tap to Login'},
      'personal_info': {'vi': 'Thông tin cá nhân', 'en': 'Personal Information'},
      'help': {'vi': 'Trợ giúp', 'en': 'Help Center'},
      'login_now': {'vi': 'Đăng nhập ngay', 'en': 'Login Now'},
      'logged_out_msg': {'vi': 'Đã đăng xuất!', 'en': 'Logged Out!'},
      'system_update': {'vi': 'Cập nhật hệ thống', 'en': 'System Update'},
      'current_version': {'vi': 'Phiên bản hiện tại', 'en': 'Current Version'},
      'latest_version': {'vi': 'Đang là phiên bản mới nhất.', 'en': 'Up to date.'},
      'close': {'vi': 'Đóng', 'en': 'Close'},
      'night': {'vi': 'đêm', 'en': 'night'},
      'available_rooms': {'vi': 'Còn trống', 'en': 'Available'},
      'ready': {'vi': 'phòng sẵn sàng', 'en': 'rooms ready'},
      'out_of_rooms': {'vi': 'Hết phòng (Đang bận hoặc chờ dọn dẹp)', 'en': 'No Rooms (Occupied/Cleaning)'},
      'booking_required': {'vi': 'Vui lòng đăng nhập để thực hiện đặt phòng!', 'en': 'Please login to book a room!'},
    };

    if (!translations.containsKey(key)) return key;
    return translations[key]![_currentLanguage.name] ?? key;
  }
}
