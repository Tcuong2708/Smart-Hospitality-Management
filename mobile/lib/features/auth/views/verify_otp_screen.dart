import 'dart:async';
import 'package:flutter/material.dart';
import 'package:may_hotel_app/features/auth/models/account_model.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';
import 'package:may_hotel_app/core/utils/responsive.dart';
import 'package:may_hotel_app/features/auth/views/login_screen.dart';

class VerifyOtpScreen extends StatefulWidget {
  final AccountModel account;
  const VerifyOtpScreen({Key? key, required this.account}) : super(key: key);

  @override
  State<VerifyOtpScreen> createState() => _VerifyOtpScreenState();
}

class _VerifyOtpScreenState extends State<VerifyOtpScreen> {
  final _otpController = TextEditingController();
  final _apiProvider = HotelApiProvider();
  bool _isLoading = false;
  int _start = 60;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _startTimer();
  }

  void _startTimer() {
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_start == 0) {
        setState(() => timer.cancel());
      } else {
        setState(() => _start--);
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    _otpController.dispose();
    super.dispose();
  }
// LOGIC XÁC THỰC VÀ LƯU TÀI KHOẢN
  Future<void> _handleVerify() async {
    if (_otpController.text.length < 6) {
      _showSnackBar("Vui lòng nhập đủ 6 số!", Colors.orange);
      return;
    }

    setState(() => _isLoading = true);

    try {
      final isOtpValid = await _apiProvider.verifyOTP(widget.account.email, _otpController.text);

      if (isOtpValid) {
        _showSnackBar("OTP chính xác! Đang tạo tài khoản...", Colors.blue);

        final isRegistered = await _apiProvider.register(widget.account.toJson());

        debugPrint("KẾT QUẢ ĐĂNG KÝ: $isRegistered");

        if (isRegistered && mounted) {
          _showSnackBar("Đăng ký thành công!", Colors.green);

          await Future.delayed(const Duration(seconds: 1));

          if (mounted) {
            Navigator.pushAndRemoveUntil(
              context,
              MaterialPageRoute(builder: (context) => LoginScreen()), // Nhớ import LoginScreen
                  (route) => false, // Xóa sạch lịch sử các trang trước đó
            );
          }
        } else {
          _showSnackBar("Lưu tài khoản thất bại hoặc phản hồi từ Server chậm.", Colors.red);
        }
      } else {
        _showSnackBar("Mã OTP không đúng hoặc đã hết hạn.", Colors.red);
      }
    } catch (e) {
      debugPrint("LỖI XÁC THỰC: $e");
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
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(title: Text("Xác thực OTP", style: TextStyle(fontSize: Responsive.sp(context, 20)))),
      body: SingleChildScrollView(
        padding: EdgeInsets.symmetric(horizontal: Responsive.width(context) * 0.1),
        child: Column(
          children: [
            SizedBox(height: Responsive.height(context) * 0.05),
            Text("Mã xác thực đã được gửi đến:", style: TextStyle(color: Colors.grey)),
            Text(
              widget.account.email, // Dùng email từ trong object account
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: Responsive.sp(context, 16), color: colorScheme.primary),
            ),
            const SizedBox(height: 40),
            TextField(
              controller: _otpController,
              keyboardType: TextInputType.number,
              textAlign: TextAlign.center,
              maxLength: 6,
              style: TextStyle(fontSize: Responsive.sp(context, 28), letterSpacing: 10, fontWeight: FontWeight.bold),
              decoration: InputDecoration(
                hintText: "000000",
                counterText: "",
                filled: true,
                fillColor: colorScheme.surfaceVariant.withOpacity(0.3),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(15), borderSide: BorderSide.none),
              ),
            ),
            const SizedBox(height: 20),
            Text(_start > 0 ? "Gửi lại mã sau ${_start}s" : "Bạn có thể gửi lại mã ngay"),
            SizedBox(height: Responsive.height(context) * 0.1),
            SizedBox(
              width: double.infinity,
              height: 55,
              child: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: colorScheme.primary,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
                ),
                onPressed: _isLoading ? null : _handleVerify,
                child: _isLoading
                    ? const CircularProgressIndicator(color: Colors.white)
                    : const Text("Xác nhận", style: TextStyle(color: Colors.white, fontSize: 18)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}