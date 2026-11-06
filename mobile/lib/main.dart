import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:may_hotel_app/services/notification_service.dart';
import 'package:may_hotel_app/services/auth_state_service.dart';
import 'package:may_hotel_app/services/device_security_service.dart';
import 'package:may_hotel_app/services/language_service.dart';
import 'core/constants/app_colors.dart';
import 'features/home/views/main_screen.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

final ValueNotifier<ThemeMode> themeNotifier = ValueNotifier(ThemeMode.light);

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  try {
    await dotenv.load(fileName: ".env");
    debugPrint("Đã load file .env thành công!");
  } catch (e) {
    debugPrint("Không tìm thấy file .env: $e");
  }

  if (kDebugMode &&
      dotenv.env['ALLOW_BAD_CERTIFICATES']?.trim().toLowerCase() == 'true') {
    HttpOverrides.global = MyHttpOverrides();
  }

  runApp(const MayHotelApp());

  // Plugin thông báo là chức năng bổ trợ. Khởi tạo sau lần dựng giao diện
  // đầu tiên để máy ảo không bị màn hình trắng nếu plugin phản hồi chậm.
  WidgetsBinding.instance.addPostFrameCallback((_) {
    unawaited(NotificationService().initNotification());
  });
}

class MayHotelApp extends StatefulWidget {
  const MayHotelApp({super.key});

  @override
  State<MayHotelApp> createState() => _MayHotelAppState();
}

class _MayHotelAppState extends State<MayHotelApp> {
  late final Future<String?> _securityCheck;

  @override
  void initState() {
    super.initState();
    _securityCheck = _checkSecurity();
  }

  Future<String?> _checkSecurity() async {
    // TRUSTED_DEVICE_FINGERPRINTS nhận tối đa hai SHA-256, phân cách bằng dấu
    // phẩy. Không dùng serial ADB (ví dụ RF8XB01P13T/RF8N32LPQCA) tại đây.
    final trustedFingerprints =
        dotenv.env['TRUSTED_DEVICE_FINGERPRINTS']?.trim() ??
        const String.fromEnvironment('TRUSTED_DEVICE_FINGERPRINTS');

    final result =
        await DeviceSecurityService(
          trustedDeviceFingerprints: trustedFingerprints,
          // Bản phát triển dùng được qua cáp. Ở bản release, chỉ thiết bị có
          // fingerprint trong danh sách trên được miễn kiểm tra USB debugging.
          // Root và bootloader vẫn luôn bị kiểm tra trên mọi thiết bị.
          allowUsbDebuggingForDevelopment: !kReleaseMode,
        ).check().timeout(
          const Duration(seconds: 10),
          onTimeout: () => const DeviceSecurityResult.blocked(
            DeviceSecurityIssue.verificationFailed,
          ),
        );
    return result.message;
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<String?>(
      future: _securityCheck,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const MaterialApp(
            debugShowCheckedModeBanner: false,
            home: Scaffold(
              body: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    CircularProgressIndicator(),
                    SizedBox(height: 16),
                    Text('Đang kiểm tra an toàn thiết bị...'),
                  ],
                ),
              ),
            ),
          );
        }

        // Lấy thông điệp lỗi bảo mật (nếu có)
        final String? warningMessage = snapshot.hasError
            ? const DeviceSecurityResult.blocked(
                DeviceSecurityIssue.verificationFailed,
              ).message
            : snapshot.data;

        if (warningMessage != null) {
          return MaterialApp(
            debugShowCheckedModeBanner: false,
            home: Scaffold(
              backgroundColor: Colors.black54,
              body: Center(
                child: Container(
                  margin: const EdgeInsets.symmetric(horizontal: 32.0),
                  padding: const EdgeInsets.all(24.0),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16.0),
                    boxShadow: const [
                      BoxShadow(
                        color: Colors.black26,
                        blurRadius: 10,
                        offset: Offset(0, 4),
                      ),
                    ],
                  ),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(
                        Icons.gpp_bad_rounded,
                        size: 64,
                        color: AppColors.primary,
                      ),
                      const SizedBox(height: 16),
                      const Text(
                        "Cảnh báo bảo mật",
                        style: TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                          color: Colors.black87,
                        ),
                      ),
                      const SizedBox(height: 12),
                      Text(
                        warningMessage,
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          fontSize: 16,
                          color: Colors.black87,
                          height: 1.5,
                        ),
                      ),
                      const SizedBox(height: 24),
                      SizedBox(
                        width: double.infinity,
                        height: 48,
                        child: ElevatedButton(
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AppColors.primary,
                            foregroundColor: Colors.white,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(8),
                            ),
                          ),
                          onPressed: () {
                            SystemNavigator.pop();
                          },
                          child: const Text(
                            "Thoát ứng dụng",
                            style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          );
        }

        // Trả về bình thường khi mở app
        return ListenableBuilder(
          listenable: Listenable.merge([AuthStateService(), LanguageService()]),
          builder: (context, _) {
            final authState = AuthStateService().currentAuth;

            return ValueListenableBuilder<ThemeMode>(
              valueListenable: themeNotifier,
              builder: (context, currentMode, _) {
                final bool isDark = currentMode == ThemeMode.dark;

                final style = SystemUiOverlayStyle(
                  statusBarColor: Colors.transparent,
                  statusBarIconBrightness: isDark
                      ? Brightness.light
                      : Brightness.dark,
                  systemNavigationBarColor: Colors.transparent,
                  systemNavigationBarIconBrightness: isDark
                      ? Brightness.light
                      : Brightness.dark,
                  systemNavigationBarContrastEnforced: false,
                );
                SystemChrome.setSystemUIOverlayStyle(style);
                SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);

                return AnnotatedRegion<SystemUiOverlayStyle>(
                  value: style,
                  child: MaterialApp(
                    debugShowCheckedModeBanner: false,
                    themeMode: currentMode,
                    theme: _buildTheme(Brightness.light),
                    darkTheme: _buildTheme(Brightness.dark),
                    home: MainScreen(
                      key: ValueKey(
                        '${authState.role}:${authState.userId ?? authState.username}:${authState.isLoggedIn}',
                      ),
                      role: authState.role,
                      username: authState.username,
                    ),
                  ),
                );
              },
            );
          },
        );
      },
    );
  }

  ThemeData _buildTheme(Brightness brightness) {
    final bool isDark = brightness == Brightness.dark;
    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      primaryColor: AppColors.primary,
      cardColor: isDark ? AppColors.cardBgDark : AppColors.cardBg,
      scaffoldBackgroundColor: isDark
          ? AppColors.primaryDark
          : AppColors.background,
      colorScheme: ColorScheme.fromSeed(
        seedColor: AppColors.primary,
        brightness: brightness,
        primary: isDark ? AppColors.accentGold : AppColors.primary,
        secondary: AppColors.accentGold,
        surface: isDark ? AppColors.cardBgDark : AppColors.cardBg,
        onSurface: isDark ? Colors.white : AppColors.primary,
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: isDark ? const Color(0xFF050B14) : AppColors.primary,
        foregroundColor: AppColors.accentGold,
        elevation: 0,
        centerTitle: true,
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: isDark ? Colors.white.withValues(alpha: 0.05) : Colors.white,
        labelStyle: TextStyle(
          color: isDark ? AppColors.accentGold : AppColors.primary,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(
            color: isDark ? Colors.white12 : Colors.grey.shade300,
          ),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: AppColors.accentGold, width: 2),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: isDark ? AppColors.accentGold : AppColors.primary,
          foregroundColor: isDark ? AppColors.primary : AppColors.accentGold,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
      ),
    );
  }
}

class MyHttpOverrides extends HttpOverrides {
  @override
  HttpClient createHttpClient(SecurityContext? context) {
    return super.createHttpClient(context)
      ..badCertificateCallback =
          (X509Certificate cert, String host, int port) => true;
  }
}
