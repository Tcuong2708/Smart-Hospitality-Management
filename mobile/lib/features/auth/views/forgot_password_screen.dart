import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/utils/responsive.dart';
import '../services/auth_api_service.dart';


class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({Key? key}) : super(key: key);

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  final _apiService = AuthApiService();

  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _otpController = TextEditingController();
  final TextEditingController _newPasswordController = TextEditingController();

  bool _isOtpSent = false;
  bool _isLoading = false;
  bool _isPasswordObscured = true;

  @override
  void dispose() {
    _emailController.dispose();
    _otpController.dispose();
    _newPasswordController.dispose();
    super.dispose();
  }

  // 🔄 LUỒNG 1: GỬI MÃ OTP VÀO EMAIL KHÁCH HÀNG
  Future<void> _handleSendOtp() async {
    final email = _emailController.text.trim();
    if (email.isEmpty || !email.contains("@")) {
      _showSnackBar("Vui lòng nhập định dạng Email hợp lệ!", Colors.orange);
      return;
    }

    setState(() => _isLoading = true);

    try {
      final bool success = await _apiService.sendForgotPasswordOtp(email);

      if (success) {
        setState(() {
          _isOtpSent = true;
        });
        _showSnackBar("Mã OTP đã được phóng vào Email của bạn. Vui lòng kiểm tra!", Colors.green);
      } else {
        _showSnackBar("Không thể gửi mã yêu cầu. Kiểm tra lại hệ thống!", Colors.red);
      }
    } catch (e) {
      debugPrint("Lỗi gửi OTP: $e");
      _showSnackBar("Lỗi: ${e.toString().replaceAll("Exception:", "")}", Colors.red);
    } finally {
      setState(() => _isLoading = false);
    }
  }

  // 🔄 LUỒNG 2: XÁC THỰC MÃ OTP VÀ ĐỔI MẬT KHẨU MỚI TOÀN DIỆN
  Future<void> _handleResetPassword() async {
    final email = _emailController.text.trim();
    final otpCode = _otpController.text.trim();
    final newPassword = _newPasswordController.text.trim();

    if (otpCode.isEmpty || newPassword.length < 6) {
      _showSnackBar("Mã OTP không được để trống và Mật khẩu mới phải từ 6 ký tự!", Colors.orange);
      return;
    }

    setState(() => _isLoading = true);

    try {
      final bool success = await _apiService.resetPassword(email, otpCode, newPassword);

      if (success) {
        _showSnackBar("🟢 Đặt lại mật khẩu thành công! Hãy đăng nhập lại bằng mật khẩu mới.", Colors.green);
        if (mounted) {
          Navigator.pop(context);
        }
      } else {
        _showSnackBar("Xác thực thất bại! Vui lòng kiểm tra lại mã OTP.", Colors.red);
      }
    } catch (e) {
      debugPrint("Lỗi reset mật khẩu: $e");
      String errorMsg = "Đã xảy ra lỗi khi đặt lại mật khẩu.";

      try {
        final dynamic response = (e as dynamic).response;
        if (response != null && response.data != null) {
          final serverData = response.data;

          if (serverData is String && serverData.isNotEmpty) {
            errorMsg = serverData;
          } else if (serverData is Map) {
            errorMsg = serverData['message'] ?? serverData['Message'] ?? errorMsg;
          }
        } else {
          errorMsg = e.toString().replaceAll("Exception:", "");
        }
      } catch (_) {
        errorMsg = e.toString().replaceAll("Exception:", "");
      }

      _showSnackBar(errorMsg, Colors.red);
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  void _showSnackBar(String message, Color color) {
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
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: Icon(Icons.arrow_back_ios, color: colorScheme.onSurface),
          onPressed: () => Navigator.pop(context),
        ),
        systemOverlayStyle: SystemUiOverlayStyle(
          statusBarColor: Colors.transparent,
          statusBarIconBrightness: theme.brightness == Brightness.dark
              ? Brightness.light
              : Brightness.dark,
          statusBarBrightness: theme.brightness,
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: EdgeInsets.symmetric(horizontal: Responsive.width(context) * 0.08),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SizedBox(height: Responsive.sp(context, 15)),
              Text(
                _isOtpSent ? "Xác nhận OTP" : "Quên mật khẩu?",
                style: TextStyle(
                  fontSize: Responsive.sp(context, 28),
                  fontWeight: FontWeight.bold,
                  color: colorScheme.onSurface,
                ),
              ),
              SizedBox(height: Responsive.sp(context, 10)),
              Text(
                _isOtpSent
                    ? "Chúng tôi đã gửi mã về email ${_emailController.text}. Vui lòng nhập mã và thiết lập mật khẩu mới bên dưới."
                    : "Đừng lo lắng! Hãy nhập Email đã đăng ký, hệ thống May Hotel sẽ gửi mã xác nhận để đặt lại mật khẩu cho bạn.",
                style: TextStyle(color: colorScheme.onSurface.withOpacity(0.6), height: 1.5, fontSize: 14),
              ),

              SizedBox(height: Responsive.sp(context, 35)),

              if (!_isOtpSent) ...[
                _buildCustomTextField(
                  context,
                  controller: _emailController,
                  hint: "Nhập Email của bạn",
                  icon: Icons.email_outlined,
                  keyboardType: TextInputType.emailAddress,
                ),
                SizedBox(height: Responsive.sp(context, 40)),
                _buildActionButton(
                  context,
                  text: "Gửi mã xác nhận OTP",
                  onPressed: _handleSendOtp,
                ),
              ] else ...[
                _buildCustomTextField(
                  context,
                  controller: _otpController,
                  hint: "Nhập 6 số mã OTP nhận được",
                  icon: Icons.pin_outlined,
                  keyboardType: TextInputType.number,
                ),
                SizedBox(height: Responsive.sp(context, 16)),
                _buildCustomTextField(
                  context,
                  controller: _newPasswordController,
                  hint: "Nhập mật khẩu mới",
                  icon: Icons.lock_outline_rounded,
                  isPassword: true,
                ),

                Align(
                  alignment: Alignment.centerLeft,
                  child: TextButton.icon(
                    onPressed: _isLoading ? null : () => setState(() => _isOtpSent = false),
                    icon: const Icon(Icons.arrow_back, size: 14),
                    label: const Text("Thay đổi Email / Gửi lại mã", style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                  ),
                ),

                SizedBox(height: Responsive.sp(context, 25)),
                _buildActionButton(
                  context,
                  text: "Xác nhận đặt lại mật khẩu",
                  onPressed: _handleResetPassword,
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildCustomTextField(
      BuildContext context, {
        required TextEditingController controller,
        required String hint,
        required IconData icon,
        TextInputType keyboardType = TextInputType.text,
        bool isPassword = false,
      }) {
    final colorScheme = Theme.of(context).colorScheme;
    final bool isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(15),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(isDark ? 0.3 : 0.05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          )
        ],
      ),
      child: TextField(
        controller: controller,
        keyboardType: keyboardType,
        obscureText: isPassword ? _isPasswordObscured : false,
        style: TextStyle(color: colorScheme.onSurface),
        decoration: InputDecoration(
          prefixIcon: Icon(icon, color: colorScheme.primary.withOpacity(0.7), size: 22),
          suffixIcon: isPassword
              ? IconButton(
            icon: Icon(
              _isPasswordObscured ? Icons.visibility_off_outlined : Icons.visibility_outlined,
              color: colorScheme.onSurface.withOpacity(0.5),
            ),
            onPressed: () => setState(() => _isPasswordObscured = !_isPasswordObscured),
          )
              : null,
          hintText: hint,
          hintStyle: TextStyle(color: colorScheme.onSurface.withOpacity(0.4), fontSize: 14),
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(vertical: 16),
        ),
      ),
    );
  }

  Widget _buildActionButton(BuildContext context, {required String text, required VoidCallback onPressed}) {
    final colorScheme = Theme.of(context).colorScheme;

    return SizedBox(
      width: double.infinity,
      height: Responsive.sp(context, 55),
      child: ElevatedButton(
        style: ElevatedButton.styleFrom(
          backgroundColor: colorScheme.primary,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(Responsive.sp(context, 15)),
          ),
          elevation: 3,
        ),
        onPressed: _isLoading ? () {} : onPressed,
        child: _isLoading
            ? const SizedBox(
          height: 22,
          width: 22,
          child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5),
        )
            : Text(
          text,
          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16),
        ),
      ),
    );
  }
}