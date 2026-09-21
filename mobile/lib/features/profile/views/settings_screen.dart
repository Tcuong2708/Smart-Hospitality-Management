import 'package:flutter/foundation.dart'; // Thêm để kiểm tra kIsWeb
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:local_auth/local_auth.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/utils/responsive.dart';
import '../../../../main.dart';
import 'package:may_hotel_app/features/auth/services/auth_api_service.dart';
import 'package:may_hotel_app/features/auth/views/login_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:may_hotel_app/services/auth_state_service.dart';
import 'package:may_hotel_app/services/language_service.dart';

class SettingsScreen extends StatefulWidget {
  final String username;
  const SettingsScreen({Key? key, required this.username}) : super(key: key);

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  final LocalAuthentication auth = LocalAuthentication();
  final _apiService = AuthApiService();

  bool _isNotificationEnabled = true;
  bool _isBiometricEnabled = false;
  bool _isDeleting = false;

  @override
  void initState() {
    super.initState();
    _loadBiometricStatus();
  }

  Future<void> _loadBiometricStatus() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _isBiometricEnabled = prefs.getBool('isBiometricEnabled') ?? false;
    });
  }

  Future<void> _toggleBiometric(bool newValue) async {
    // 🚀 KIỂM TRA WEB: Không hỗ trợ local_auth trên Web theo cách thông thường
    if (kIsWeb) {
      _showErrorSnackBar("Tính năng Đăng nhập nhanh hiện chỉ hỗ trợ trên ứng dụng Mobile.");
      return;
    }

    final authState = AuthStateService().currentAuth;
    if (!authState.isLoggedIn) {
      _showErrorSnackBar("Vui lòng đăng nhập trước khi bật tính năng này!");
      Navigator.push(context, MaterialPageRoute(builder: (context) => const LoginScreen()));
      return;
    }

    if (!newValue) {
      setState(() => _isBiometricEnabled = false);
      _showSuccessSnackBar("🔴 Đã tắt tính năng đăng nhập nhanh.");
      return;
    }

    try {
      final bool canCheckBiometrics = kIsWeb ? false : await auth.canCheckBiometrics;
      final bool isHardwareAvailable = kIsWeb ? false : (canCheckBiometrics || await auth.isDeviceSupported());

      if (!isHardwareAvailable) {
        _showErrorSnackBar("Thiết bị này không hỗ trợ hoặc chưa cài đặt mật khẩu hệ thống.");
        return;
      }

      final bool didAuthenticate = await auth.authenticate(
        localizedReason: 'Xác thực để kích hoạt đăng nhập nhanh May Hotel',
        biometricOnly: false,
      );

      if (didAuthenticate) {
        setState(() => _isBiometricEnabled = true);
        final prefs = await SharedPreferences.getInstance();
        await prefs.setBool('isBiometricEnabled', true);
        _showSuccessSnackBar("🟢 Đã kích hoạt Đăng nhập nhanh hệ thống thành công!");
      }
    } on PlatformException catch (e) {
      _showErrorSnackBar("Không thể xác thực: ${e.message}");
    }
  }

  void _showDeleteAccountDialog() {
    final theme = Theme.of(context);
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext dialogContext) {
        return AlertDialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          title: const Row(
            children: [
              Icon(Icons.warning_amber_rounded, color: Colors.red, size: 28),
              SizedBox(width: 10),
              Text("Xóa tài khoản?", style: TextStyle(fontWeight: FontWeight.bold)),
            ],
          ),
          content: Text(
            "Tài khoản '@${widget.username}' sẽ bị xóa vĩnh viễn. Bạn có chắc chắn?",
            style: const TextStyle(fontSize: 14, height: 1.4),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext),
              child: Text("Hủy bỏ", style: TextStyle(color: theme.colorScheme.onSurface.withValues(alpha: 0.6), fontWeight: FontWeight.bold)),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: Colors.red, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12))),
              onPressed: () {
                Navigator.pop(dialogContext);
                _executeDeleteAccountProcess();
              },
              child: const Text("Xóa vĩnh viễn", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
            ),
          ],
        );
      },
    );
  }

  Future<void> _executeDeleteAccountProcess() async {
    setState(() => _isDeleting = true);
    try {
      final bool success = await _apiService.deleteAccount(widget.username);
      if (success && mounted) {
        _showSuccessSnackBar("Tài khoản đã được xóa hoàn toàn.");
        AuthStateService().logout();
        Navigator.pop(context);
      }
    } catch (e) {
      _showErrorSnackBar("Lỗi xóa tài khoản.");
    } finally {
      if (mounted) setState(() => _isDeleting = false);
    }
  }

  void _showErrorSnackBar(String msg) {
    ScaffoldMessenger.of(context).removeCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("⚠️ $msg"), backgroundColor: Colors.red, behavior: SnackBarBehavior.floating));
  }

  void _showSuccessSnackBar(String msg) {
    ScaffoldMessenger.of(context).removeCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg), backgroundColor: Colors.green, behavior: SnackBarBehavior.floating, duration: const Duration(seconds: 2)));
  }

  void _showLanguageDialog(BuildContext context) {
    final langService = LanguageService();
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Text(langService.translate('language')),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _buildLanguageOption(context, "Tiếng Việt", AppLanguage.vi, langService),
            _buildLanguageOption(context, "English", AppLanguage.en, langService),
          ],
        ),
      ),
    );
  }

  Widget _buildLanguageOption(BuildContext context, String label, AppLanguage lang, LanguageService service) {
    bool isSelected = service.currentLanguage == lang;
    return ListTile(
      title: Text(label, style: TextStyle(fontWeight: isSelected ? FontWeight.bold : FontWeight.normal)),
      trailing: isSelected ? const Icon(Icons.check, color: Colors.green) : null,
      onTap: () {
        service.changeLanguage(lang);
        Navigator.pop(context);
        _showSuccessSnackBar(lang == AppLanguage.vi ? "Đã đổi sang Tiếng Việt" : "Language changed to English");
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: Listenable.merge([AuthStateService(), LanguageService()]),
      builder: (context, _) {
        final bool isLoggedIn = AuthStateService().currentAuth.isLoggedIn;
        final langService = LanguageService();

        return ValueListenableBuilder<ThemeMode>(
          valueListenable: themeNotifier,
          builder: (context, currentMode, child) {
            final theme = Theme.of(context);
            final colorScheme = theme.colorScheme;
            final bool isDarkMode = currentMode == ThemeMode.dark;
            final Color textColor = colorScheme.onSurface;

            return Scaffold(
              backgroundColor: theme.scaffoldBackgroundColor,
              appBar: AppBar(
                backgroundColor: colorScheme.surface,
                elevation: 0.5,
                leading: BackButton(color: textColor),
                title: Text(langService.translate('settings'), style: TextStyle(color: textColor, fontWeight: FontWeight.bold, fontSize: Responsive.sp(context, 20))),
              ),
              body: _isDeleting
                  ? const Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          CircularProgressIndicator(color: Colors.red),
                          SizedBox(height: 15),
                          Text("Hệ thống đang tiến hành xóa dữ liệu tài khoản...", style: TextStyle(fontWeight: FontWeight.bold, color: Colors.red)),
                        ],
                      ),
                    )
                  : ListView(
                      padding: EdgeInsets.all(Responsive.sp(context, 20)),
                      children: [
                        _buildSectionTitle(context, langService.translate('interface')),
                        SwitchListTile(
                          secondary: Icon(isDarkMode ? Icons.dark_mode : Icons.light_mode, color: isDarkMode ? Colors.amber : colorScheme.primary),
                          title: Text(langService.translate('dark_mode'), style: TextStyle(color: textColor, fontWeight: FontWeight.w500)),
                          subtitle: Text("Tiết kiệm pin và bảo vệ mắt", style: TextStyle(color: textColor.withValues(alpha: 0.6), fontSize: 13)),
                          value: isDarkMode,
                          activeColor: colorScheme.primary,
                          onChanged: (bool value) => themeNotifier.value = value ? ThemeMode.dark : ThemeMode.light,
                        ),
                        const Divider(height: 30),
                        _buildSectionTitle(context, langService.translate('security_biometric')),
                        _buildSwitchTile(context, "Thông báo đẩy", "Nhận thông tin cập nhật", _isNotificationEnabled ? Icons.notifications_active_outlined : Icons.notifications_off_outlined, _isNotificationEnabled, (bool val) => setState(() => _isNotificationEnabled = val)),
                        
                        // 🚀 CHỈ HIỆN ĐĂNG NHẬP NHANH TRÊN MOBILE
                        if (!kIsWeb)
                          _buildSwitchTile(context, "Đăng nhập nhanh hệ thống", "Hỗ trợ Vân tay, Khuôn mặt", Icons.shield_outlined, _isBiometricEnabled, _toggleBiometric),

                        const Divider(height: 30),
                        _buildSectionTitle(context, langService.translate('language')),
                        ListTile(
                          leading: Icon(Icons.language, color: isDarkMode ? Colors.white70 : colorScheme.primary),
                          title: Text(langService.translate('app_language'), style: TextStyle(color: textColor, fontWeight: FontWeight.w500)),
                          trailing: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(
                                langService.currentLanguage == AppLanguage.vi ? "Tiếng Việt" : "English", 
                                style: TextStyle(color: isDarkMode ? Colors.white60 : colorScheme.primary, fontWeight: FontWeight.bold, fontSize: Responsive.sp(context, 14))
                              ),
                              const SizedBox(width: 5),
                              Icon(Icons.arrow_forward_ios, size: 14, color: textColor.withValues(alpha: 0.3)),
                            ],
                          ),
                          onTap: () => _showLanguageDialog(context),
                        ),
                        if (isLoggedIn) ...[
                          const Divider(height: 30),
                          _buildSectionTitle(context, langService.translate('danger_zone')),
                          const SizedBox(height: 5),
                          Container(
                            decoration: BoxDecoration(color: Colors.red.withValues(alpha: 0.06), borderRadius: BorderRadius.circular(Responsive.sp(context, 20)), border: Border.all(color: Colors.red.withValues(alpha: 0.2))),
                            child: ListTile(
                              leading: const Icon(Icons.no_accounts_rounded, color: Colors.red),
                              title: Text(langService.translate('delete_account'), style: const TextStyle(color: Colors.red, fontWeight: FontWeight.bold)),
                              subtitle: const Text("Hủy toàn bộ hồ sơ tại May Hotel", style: TextStyle(color: Colors.red, fontSize: 12)),
                              trailing: const Icon(Icons.arrow_forward_ios_rounded, size: 14, color: Colors.red),
                              onTap: _showDeleteAccountDialog,
                            ),
                          ),
                        ],
                        const SizedBox(height: 40),
                        Center(child: Text("May Hotel v1.0.0", style: TextStyle(color: textColor.withValues(alpha: 0.3), fontSize: 12))),
                      ],
                    ),
            );
          },
        );
      },
    );
  }

  Widget _buildSwitchTile(BuildContext context, String title, String subtitle, IconData icon, bool val, Function(bool) onChanged) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final bool isDark = theme.brightness == Brightness.dark;
    return SwitchListTile(
      secondary: Icon(icon, color: isDark ? Colors.white70 : colorScheme.primary),
      title: Text(title, style: TextStyle(color: colorScheme.onSurface, fontWeight: FontWeight.w500)),
      subtitle: Text(subtitle, style: TextStyle(color: colorScheme.onSurface.withValues(alpha: 0.6), fontSize: 12)),
      value: val,
      onChanged: onChanged,
      activeColor: colorScheme.primary,
    );
  }

  Widget _buildSectionTitle(BuildContext context, String title) {
    final Color textColor = Theme.of(context).colorScheme.onSurface;
    return Padding(padding: const EdgeInsets.only(bottom: 10, left: 5), child: Text(title.toUpperCase(), style: TextStyle(color: textColor.withValues(alpha: 0.5), fontWeight: FontWeight.bold, fontSize: 12, letterSpacing: 1.2)));
  }
}
