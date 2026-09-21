import 'package:flutter/material.dart';
import 'package:may_hotel_app/features/auth/models/account_model.dart';
import 'package:may_hotel_app/features/auth/views/verify_otp_screen.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';
import '../../../core/utils/responsive.dart';
import 'package:flutter/services.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({Key? key}) : super(key: key);

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  // 1. Khai báo các Controller
  final _nameController = TextEditingController();
  final _userController = TextEditingController();
  final _emailController = TextEditingController();
  final _phoneController = TextEditingController();
  final _addressController = TextEditingController();
  final _nationalityController = TextEditingController(text: "Việt Nam");
  final _passController = TextEditingController();
  final _confirmPassController = TextEditingController();

  final _apiProvider = HotelApiProvider();
  bool _isLoading = false;

  @override
  void dispose() {
    _nameController.dispose();
    _userController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    _addressController.dispose();
    _nationalityController.dispose();
    _passController.dispose();
    _confirmPassController.dispose();
    super.dispose();
  }

  // 2. Logic: Gửi mã OTP và truyền Dữ liệu Account sang trang Verify
  Future<void> _handleRegisterRequest() async {
    // Kiểm tra validation cơ bản
    if (_userController.text.isEmpty || _emailController.text.isEmpty || _passController.text.isEmpty || _nameController.text.isEmpty) {
      _showSnackBar("Vui lòng điền đầy đủ các thông tin bắt buộc!", Colors.orange);
      return;
    }

    if (_passController.text != _confirmPassController.text) {
      _showSnackBar("Mật khẩu xác nhận không khớp!", Colors.red);
      return;
    }

    setState(() => _isLoading = true);

    // GOM TOÀN BỘ DỮ LIỆU VÀO MODEL
    final account = AccountModel(
      tenDangNhap: _userController.text,
      matKhau: _passController.text,
      hoTen: _nameController.text,
      email: _emailController.text,
      soDienThoai: _phoneController.text,
      diaChi: _addressController.text,
      quocTich: _nationalityController.text,
      roleID: 3, // Mặc định là Khách hàng
      trangThai: true,
    );

    try {
      // Gọi API gửi mã OTP về email
      final bool isSent = await _apiProvider.sendOTP(account.email);

      if (isSent && mounted) {
        _showSnackBar("Mã OTP đã được gửi đến email của bạn!", Colors.green);

        // CHUYỂN SANG TRANG OTP VÀ TRUYỀN BIẾN account SANG
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => VerifyOtpScreen(account: account),
          ),
        );
      } else {
        _showSnackBar("Không thể gửi mã OTP. Vui lòng kiểm tra lại email!", Colors.red);
      }
    } catch (e) {
      _showSnackBar("Lỗi kết nối hệ thống!", Colors.red);
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showSnackBar(String msg, Color color) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(msg),
      backgroundColor: color,
      behavior: SnackBarBehavior.floating,
    ));
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
              Text(
                "Đăng ký tài khoản",
                style: TextStyle(
                    fontSize: Responsive.sp(context, 26),
                    fontWeight: FontWeight.bold,
                    color: colorScheme.primary
                ),
              ),
              const SizedBox(height: 10),
              Text(
                "Vui lòng nhập thông tin để nhận mã xác thực OTP",
                style: TextStyle(fontSize: Responsive.sp(context, 14), color: Colors.grey),
              ),
              const SizedBox(height: 30),

              _buildField(context, "Họ và tên *", Icons.person_outline, _nameController),
              const SizedBox(height: 15),
              _buildField(context, "Tên đăng nhập *", Icons.account_circle_outlined, _userController),
              const SizedBox(height: 15),
              _buildField(context, "Email *", Icons.email_outlined, _emailController, type: TextInputType.emailAddress),
              const SizedBox(height: 15),
              _buildField(context, "Số điện thoại", Icons.phone_android_outlined, _phoneController, type: TextInputType.phone),
              const SizedBox(height: 15),
              _buildField(context, "Địa chỉ", Icons.location_on_outlined, _addressController),
              const SizedBox(height: 15),
              _buildField(context, "Mật khẩu *", Icons.lock_outline, _passController, isPass: true),
              const SizedBox(height: 15),
              _buildField(context, "Xác nhận mật khẩu *", Icons.lock_reset_outlined, _confirmPassController, isPass: true),

              const SizedBox(height: 40),
              _buildRegButton(),
              const SizedBox(height: 30),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildField(BuildContext context, String hint, IconData icon, TextEditingController controller, {bool isPass = false, TextInputType type = TextInputType.text}) {
    final colorScheme = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(15),
        boxShadow: [
          BoxShadow(
              color: Colors.black.withOpacity(isDark ? 0.3 : 0.05),
              blurRadius: 10,
              offset: const Offset(0, 5)
          )
        ],
      ),
      child: TextField(
        controller: controller,
        obscureText: isPass,
        keyboardType: type,
        style: TextStyle(color: colorScheme.onSurface),
        decoration: InputDecoration(
          prefixIcon: Icon(icon, color: colorScheme.primary, size: 22),
          hintText: hint,
          hintStyle: TextStyle(color: colorScheme.onSurface.withOpacity(0.4), fontSize: Responsive.sp(context, 14)),
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(vertical: 15, horizontal: 10),
        ),
      ),
    );
  }

  Widget _buildRegButton() {
    return SizedBox(
      width: double.infinity,
      height: 55,
      child: ElevatedButton(
        style: ElevatedButton.styleFrom(
          backgroundColor: Theme.of(context).colorScheme.primary,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
          elevation: 2,
        ),
        onPressed: _isLoading ? null : _handleRegisterRequest,
        child: _isLoading
            ? const CircularProgressIndicator(color: Colors.white)
            : Text(
          "Gửi mã xác nhận",
          style: TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.bold,
              fontSize: Responsive.sp(context, 16)
          ),
        ),
      ),
    );
  }
}