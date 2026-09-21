import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:local_auth/local_auth.dart';
import '../../../core/utils/responsive.dart';
import '../../home/views/main_screen.dart';
import 'register_screen.dart';
import 'forgot_password_screen.dart';
import 'package:may_hotel_app/main.dart';
import 'package:may_hotel_app/services/auth_state_service.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../services/auth_api_service.dart';
import 'package:google_sign_in/google_sign_in.dart' as gauth;

class LoginScreen extends StatefulWidget {
  const LoginScreen({Key? key}) : super(key: key);

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final LocalAuthentication _localAuth = LocalAuthentication();
  final AuthApiService _apiService = AuthApiService();

  bool _isPasswordObscured = true;
  bool _isLoading = false;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _handleLoginProcess() async {
    String usernameInput = _emailController.text.trim();
    String passwordInput = _passwordController.text.trim();

    if (usernameInput.isEmpty || passwordInput.isEmpty) {
      _showCustomSnackBar("Vui lòng nhập đầy đủ thông tin", Colors.orange);
      return;
    }

    // Validation: Username must be email or phone (10 digits starting with 0)
    final usernameRegex = RegExp(r'^([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}|0[0-9]{9})$');
    if (!usernameRegex.hasMatch(usernameInput)) {
      _showCustomSnackBar("Vui lòng nhập đúng định dạng Email hoặc Số điện thoại", Colors.orange);
      return;
    }

    // Validation: Password must be at least 6 characters
    if (passwordInput.length < 6) {
      _showCustomSnackBar("Mật khẩu phải chứa ít nhất 6 ký tự", Colors.orange);
      return;
    }

    setState(() => _isLoading = true);

    try {
      final loginResult = await _apiService.login(usernameInput, passwordInput);
      
      if (loginResult != null && mounted) {
        String role = loginResult['role'].toString();
        String username = loginResult['name']?.toString() ?? usernameInput;

        // Lưu credentials cho sinh trắc học
        final prefs = await SharedPreferences.getInstance();
        await prefs.setString('saved_username', usernameInput);
        await prefs.setString('saved_password', passwordInput);

        AuthStateService().login(role, username);
        _showCustomSnackBar("Đăng nhập thành công với quyền $role!", Colors.green);
        Navigator.pop(context);
      } else {
        _showCustomSnackBar("Thông tin đăng nhập không chính xác!", Colors.red);
      }
    } catch (e) {
      _showCustomSnackBar("Lỗi kết nối Server: $e", Colors.red);
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _handleGoogleLogin() async {
    setState(() => _isLoading = true);
    try {
      final gauth.GoogleSignIn googleSignIn = gauth.GoogleSignIn(
        serverClientId: '894387322753-mj60o5rne2c3k4sdi38mhn6r68fs7iqs.apps.googleusercontent.com',
        scopes: ['email', 'profile'],
      );
      final gauth.GoogleSignInAccount? googleUser = await googleSignIn.signIn();
      if (googleUser == null) {
        setState(() => _isLoading = false);
        return;
      }
      final gauth.GoogleSignInAuthentication googleAuth = await googleUser.authentication;
      final String? idToken = googleAuth.idToken;
      if (idToken == null) {
        _showCustomSnackBar("Không thể lấy Token xác thực từ Google!", Colors.red);
        setState(() => _isLoading = false);
        return;
      }
      final loginResult = await _apiService.loginWithGoogle(idToken);
      if (loginResult != null && mounted) {
        String role = loginResult['role'].toString();
        String name = loginResult['name']?.toString() ?? googleUser.displayName ?? "Khách Google";
        AuthStateService().login(role, name);
        _showCustomSnackBar("Đăng nhập Google thành công!", Colors.green);
        Navigator.pop(context);
      } else {
        _showCustomSnackBar("Đăng nhập Google thất bại!", Colors.red);
      }
    } catch (e) {
      _showCustomSnackBar("Lỗi xác thực Google OAuth2: $e", Colors.red);
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _handleBiometricLogin() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      bool isBiometricEnabled = prefs.getBool('isBiometricEnabled') ?? false;
      if (!isBiometricEnabled) {
        _showCustomSnackBar("Bạn chưa bật tính năng này trong Cài đặt!", Colors.orange);
        return;
      }
      String? savedUsername = prefs.getString('saved_username');
      String? savedPassword = prefs.getString('saved_password');
      if (savedUsername == null || savedPassword == null) {
        _showCustomSnackBar("Chưa có dữ liệu tài khoản! Vui lòng đăng nhập bằng mật khẩu 1 lần.", Colors.orange);
        return;
      }
      final bool canCheck = await _localAuth.canCheckBiometrics;
      final bool isSupported = canCheck || await _localAuth.isDeviceSupported();
      if (!isSupported) {
        _showCustomSnackBar("Thiết bị chưa hỗ trợ hoặc chưa cài PIN/Vân tay!", Colors.red);
        return;
      }
      bool didAuthenticate = await _localAuth.authenticate(
        localizedReason: 'Quét vân tay hoặc khuôn mặt để vào May Hotel',
      );
      if (!didAuthenticate) {
        _showCustomSnackBar("Xác thực thất bại hoặc đã Hủy!", Colors.red);
        return;
      }

      setState(() => _isLoading = true);
      final loginResult = await _apiService.login(savedUsername, savedPassword);
      if (loginResult != null && mounted) {
        String role = loginResult['role'].toString();
        String name = loginResult['name']?.toString() ?? savedUsername;
        AuthStateService().login(role, name);
        _showCustomSnackBar("Đăng nhập sinh trắc học thành công!", Colors.green);
        Navigator.pop(context);
      } else {
        _showCustomSnackBar("Lỗi hệ thống hoặc sai thông tin đăng nhập!", Colors.red);
      }
    } catch (e) {
      _showCustomSnackBar("Lỗi hệ thống: $e", Colors.red);
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  void _showCustomSnackBar(String message, Color color) {
    ScaffoldMessenger.of(context).removeCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message, style: const TextStyle(fontWeight: FontWeight.w500)),
        backgroundColor: color,
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<ThemeMode>(
      valueListenable: themeNotifier,
      builder: (context, currentMode, child) {
        final theme = Theme.of(context);
        final colorScheme = theme.colorScheme;
        final bool isDarkMode = currentMode == ThemeMode.dark;

        return Scaffold(
          backgroundColor: theme.scaffoldBackgroundColor,
          appBar: AppBar(
            backgroundColor: Colors.transparent,
            elevation: 0,
            systemOverlayStyle: SystemUiOverlayStyle(
              statusBarColor: Colors.transparent,
              statusBarIconBrightness: isDarkMode ? Brightness.light : Brightness.dark,
              statusBarBrightness: isDarkMode ? Brightness.dark : Brightness.light,
            ),
            actions: [
              Padding(
                padding: const EdgeInsets.only(right: 16.0, top: 8.0),
                child: Container(
                  decoration: BoxDecoration(
                    color: colorScheme.surface,
                    shape: BoxShape.circle,
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: isDarkMode ? 0.3 : 0.06),
                        blurRadius: 8,
                        offset: const Offset(0, 3),
                      )
                    ],
                  ),
                  child: IconButton(
                    icon: Icon(
                      isDarkMode ? Icons.dark_mode_rounded : Icons.light_mode_rounded,
                      color: isDarkMode ? Colors.amber : colorScheme.primary,
                      size: 22,
                    ),
                    onPressed: () {
                      themeNotifier.value = isDarkMode ? ThemeMode.light : ThemeMode.dark;
                    },
                    tooltip: "Chuyển đổi giao diện Sáng/Tối",
                  ),
                ),
              ),
            ],
          ),
          body: SafeArea(
            child: SingleChildScrollView(
              padding: EdgeInsets.symmetric(horizontal: Responsive.width(context) * 0.08),
              child: Column(
                children: [
                  SizedBox(height: Responsive.height(context) * 0.02),
                  Text(
                    "May Hotel",
                    style: TextStyle(
                      fontSize: Responsive.sp(context, 35),
                      fontWeight: FontWeight.bold,
                      letterSpacing: 1.5,
                      color: isDarkMode ? Colors.amber : colorScheme.primary,
                    ),
                  ),
                  Text(
                    "Đăng nhập để trải nghiệm dịch vụ",
                    style: TextStyle(color: colorScheme.onSurface.withValues(alpha: 0.6)),
                  ),
                  const SizedBox(height: 30),
                  _buildTextField(context, hint: "Email hoặc Số điện thoại", icon: Icons.person_outline, controller: _emailController),
                  SizedBox(height: Responsive.sp(context, 20)),
                  _buildTextField(context, hint: "Mật khẩu", icon: Icons.lock_outline, isPassword: true, controller: _passwordController),
                  Align(
                    alignment: Alignment.centerRight,
                    child: TextButton(
                      onPressed: () {
                        Navigator.push(context, MaterialPageRoute(builder: (context) => const ForgotPasswordScreen()));
                      },
                      child: Text("Quên mật khẩu?", style: TextStyle(color: isDarkMode ? Colors.amber : colorScheme.primary, fontWeight: FontWeight.bold)),
                    ),
                  ),
                  SizedBox(height: Responsive.sp(context, 10)),
                  Row(
                    children: [
                      Expanded(child: _buildPrimaryButton(context, "Đăng nhập", _isLoading ? () {} : _handleLoginProcess)),
                      SizedBox(width: Responsive.sp(context, 12)),
                      Container(
                        height: Responsive.sp(context, 55),
                        width: Responsive.sp(context, 55),
                        decoration: BoxDecoration(
                          color: colorScheme.surface,
                          borderRadius: BorderRadius.circular(15),
                          boxShadow: [
                            BoxShadow(color: Colors.black.withValues(alpha: isDarkMode ? 0.3 : 0.05), blurRadius: 10, offset: const Offset(0, 4))
                          ],
                        ),
                        child: IconButton(
                          icon: Icon(Icons.fingerprint_rounded, size: Responsive.sp(context, 32), color: isDarkMode ? Colors.amber : colorScheme.primary),
                          onPressed: _isLoading ? null : _handleBiometricLogin,
                          tooltip: "Đăng nhập nhanh bằng sinh trắc học",
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: Responsive.sp(context, 25)),
                  Row(
                    children: [
                      Expanded(child: Divider(color: colorScheme.outlineVariant)),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 15),
                        child: Text("Hoặc", style: TextStyle(color: colorScheme.onSurface.withValues(alpha: 0.5), fontSize: Responsive.sp(context, 14))),
                      ),
                      Expanded(child: Divider(color: colorScheme.outlineVariant)),
                    ],
                  ),
                  SizedBox(height: Responsive.sp(context, 25)),
                  _buildSocialButton(context, "Đăng nhập với Google", Icons.g_mobiledata),
                  SizedBox(height: Responsive.sp(context, 30)),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text("Chưa có tài khoản? ", style: TextStyle(color: colorScheme.onSurface)),
                      GestureDetector(
                        onTap: () {
                          Navigator.push(context, MaterialPageRoute(builder: (context) => const RegisterScreen()));
                        },
                        child: Text("Đăng ký ngay", style: TextStyle(color: isDarkMode ? Colors.amber : colorScheme.primary, fontWeight: FontWeight.bold)),
                      ),
                    ],
                  ),
                  SizedBox(height: Responsive.sp(context, 20)),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildTextField(BuildContext context, {required String hint, required IconData icon, bool isPassword = false, required TextEditingController controller}) {
    final colorScheme = Theme.of(context).colorScheme;
    final bool isDark = Theme.of(context).brightness == Brightness.dark;
    return Container(
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(Responsive.sp(context, 20)),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: isDark ? 0.3 : 0.05), blurRadius: 10, offset: const Offset(0, 5))],
      ),
      child: TextField(
        controller: controller,
        obscureText: isPassword ? _isPasswordObscured : false,
        style: TextStyle(color: colorScheme.onSurface),
        decoration: InputDecoration(
          prefixIcon: Icon(icon, color: colorScheme.onSurface.withValues(alpha: 0.5)),
          suffixIcon: isPassword ? IconButton(icon: Icon(_isPasswordObscured ? Icons.visibility_off_outlined : Icons.visibility_outlined, color: colorScheme.onSurface.withValues(alpha: 0.5)), onPressed: () => setState(() => _isPasswordObscured = !_isPasswordObscured)) : null,
          hintText: hint,
          hintStyle: TextStyle(color: colorScheme.onSurface.withValues(alpha: 0.4), fontSize: 14),
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(vertical: 15),
        ),
      ),
    );
  }

  Widget _buildPrimaryButton(BuildContext context, String text, VoidCallback onPressed) {
    final colorScheme = Theme.of(context).colorScheme;
    final bool isDark = Theme.of(context).brightness == Brightness.dark;
    return SizedBox(
      width: double.infinity,
      height: Responsive.sp(context, 55),
      child: ElevatedButton(
        onPressed: _isLoading ? null : onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: isDark ? Colors.amber : colorScheme.primary,
          disabledBackgroundColor: colorScheme.primary.withValues(alpha: 0.6),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Responsive.sp(context, 20))),
          elevation: 4,
        ),
        child: _isLoading ? const SizedBox(height: 22, width: 22, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5)) : Text(text, style: TextStyle(color: isDark ? Colors.black : Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
      ),
    );
  }

  Widget _buildSocialButton(BuildContext context, String text, IconData icon) {
    final colorScheme = Theme.of(context).colorScheme;
    return SizedBox(
      width: double.infinity,
      height: Responsive.sp(context, 55),
      child: OutlinedButton.icon(
        onPressed: _isLoading ? null : _handleGoogleLogin,
        icon: _isLoading ? const SizedBox.shrink() : Icon(icon, color: Colors.red, size: Responsive.sp(context, 35)),
        label: _isLoading ? const SizedBox(height: 22, width: 22, child: CircularProgressIndicator(strokeWidth: 2.5, valueColor: AlwaysStoppedAnimation<Color>(Colors.red))) : Text(text, style: TextStyle(color: colorScheme.onSurface, fontWeight: FontWeight.w600, fontSize: 15)),
        style: OutlinedButton.styleFrom(side: BorderSide(color: colorScheme.outlineVariant), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Responsive.sp(context, 20)))),
      ),
    );
  }
}
