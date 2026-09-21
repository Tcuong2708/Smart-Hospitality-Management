import 'package:flutter/material.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/utils/responsive.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';

class PaymentMethodScreen extends StatefulWidget {
  final int bookingId;
  final double totalAmount;

  const PaymentMethodScreen({
    Key? key,
    required this.bookingId,
    required this.totalAmount
  }) : super(key: key);

  @override
  State<PaymentMethodScreen> createState() => _PaymentMethodScreenState();
}

class _PaymentMethodScreenState extends State<PaymentMethodScreen> {
  final HotelApiProvider _apiProvider = HotelApiProvider();
  // Biến lưu ID phương thức đang được khách chọn
  int? _selectedMethodId;
  bool _isProcessing = false;

  // Hàm xử lý khi nhấn nút Xác nhận thanh toán
  void _handlePayment() async {
    if (_selectedMethodId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Vui lòng chọn một phương thức thanh toán!")),
      );
      return;
    }

    setState(() => _isProcessing = true);

    bool success = false;
    try {
      // Liên thông gọi API kết toán sang Backend C#
      success = await _apiProvider.confirmPayment(
        widget.bookingId,
        widget.totalAmount,
        _selectedMethodId!,
      );
    } catch (_) {
      success = false;
    }

    setState(() => _isProcessing = false);

    if (success) {
      _showSuccessDialog();
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Lỗi kết nối Server. Vui lòng thử lại!")),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final bool isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      appBar: AppBar(
        title: const Text("Phương thức thanh toán", style: TextStyle(fontWeight: FontWeight.bold)),
        elevation: 0.5,
      ),
      body: Stack(
        children: [
          SingleChildScrollView(
            padding: EdgeInsets.all(Responsive.sp(context, 20)),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _buildSectionTitle(context, "Thẻ tín dụng / Ghi nợ"),
                _buildPaymentCard(context, "Visa / Mastercard / JCB", "**** 4242", Icons.credit_card, Colors.blueAccent, 1),

                const SizedBox(height: 25),
                _buildSectionTitle(context, "Ví điện tử & Nền tảng số"),

                // Cường giữ nguyên các icon đã bổ sung ở đây, chỉ thêm ID (số cuối)
                _buildWalletItem(context, "MoMo", "https://developers.momo.vn/v3/assets/images/MOMO-Logo-App-6262c3743a290ef02396a24ea2b66c35.png", Colors.pink, 2),
                _buildWalletItem(context, "ZaloPay", "https://play-lh.googleusercontent.com/rvpjYf8EqoCsOFG7IpSpzgmKt820BZpWDpCW2u6gaIAFsfHOIqvk8eGXhgvJLLymIlo0rqzWUjeyG_JqevNogA=w240-h480-rw", Colors.blue, 3),
                _buildWalletItem(context, "Google Pay", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQUkmJppezLENkmpeJ5Ldh8x1Ez5J_MIf_qww&s", Colors.redAccent, 4),
                _buildWalletItem(context, "PayPal", Icons.paypal, isDark ? Colors.white : Colors.black, 5),
                _buildWalletItem(context, "Shopee Pay", "https://play-lh.googleusercontent.com/H7Ja21f7Q66xICkTSzWzjR3E9IB_2YQUbt0xlHtFdXSdUOdbOqQxxCVxiA73mm8heA", Colors.deepOrange, 6),
                _buildWalletItem(context, "Apple Pay", Icons.apple, isDark ? Colors.white : Colors.black, 7),

                const SizedBox(height: 30),
                _buildSectionTitle(context, "Khác"),
                _buildWalletItem(context, "Thanh toán khi nhận phòng (COD)", Icons.hotel_class, Colors.orange, 8),

                const SizedBox(height: 40),
                // Nút bấm thực hiện gọi API
                SizedBox(
                  width: double.infinity,
                  height: 55,
                  child: ElevatedButton(
                    onPressed: _isProcessing ? null : _handlePayment,
                    style: ElevatedButton.styleFrom(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15))),
                    child: _isProcessing
                        ? const CircularProgressIndicator(color: Colors.white)
                        : const Text("XÁC NHẬN & THANH TOÁN", style: TextStyle(fontWeight: FontWeight.bold)),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // Cập nhật lại Widget để có hiệu ứng chọn (Selection)
  Widget _buildWalletItem(BuildContext context, String name, dynamic leading, Color brandColor, int id) {
    final colorScheme = Theme.of(context).colorScheme;
    bool isSelected = _selectedMethodId == id;

    return GestureDetector(
      onTap: () => setState(() => _selectedMethodId = id),
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        decoration: BoxDecoration(
          color: colorScheme.surface,
          borderRadius: BorderRadius.circular(15),
          border: Border.all(
            color: isSelected ? brandColor : colorScheme.outlineVariant.withOpacity(0.5),
            width: isSelected ? 2 : 1,
          ),
        ),
        child: ListTile(
          leading: Container(
            width: 45, height: 45,
            alignment: Alignment.center,
            decoration: BoxDecoration(color: brandColor.withOpacity(0.1), borderRadius: BorderRadius.circular(10)),
            child: _buildLeadingIcon(leading, brandColor),
          ),
          title: Text(name, style: TextStyle(color: colorScheme.onSurface, fontWeight: isSelected ? FontWeight.bold : FontWeight.w500)),
          trailing: Icon(
              isSelected ? Icons.check_circle : Icons.radio_button_off,
              color: isSelected ? brandColor : colorScheme.onSurface.withOpacity(0.3)
          ),
        ),
      ),
    );
  }

  // Hàm helper giữ nguyên logic xử lý Icon/Image của Cường
  Widget _buildLeadingIcon(dynamic leading, Color brandColor) {
    if (leading is IconData) return Icon(leading, color: brandColor, size: 30);
    if (leading is String && leading.startsWith('http')) return Image.network(leading, width: 30, height: 30, fit: BoxFit.contain);
    return Icon(Icons.account_balance_wallet, color: brandColor, size: 30);
  }

  // Giữ nguyên Card Visa của Cường nhưng thêm logic chọn
  Widget _buildPaymentCard(BuildContext context, String title, String subtitle, IconData icon, Color cardColor, int id) {
    bool isSelected = _selectedMethodId == id;
    return GestureDetector(
      onTap: () => setState(() => _selectedMethodId = id),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          gradient: LinearGradient(colors: [cardColor, cardColor.withOpacity(0.7)]),
          borderRadius: BorderRadius.circular(20),
          border: isSelected ? Border.all(color: Colors.white, width: 3) : null,
        ),
        child: Row(
          children: [
            Icon(icon, color: Colors.white, size: 40),
            const SizedBox(width: 20),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                Text(subtitle, style: TextStyle(color: Colors.white.withOpacity(0.8))),
              ],
            ),
            const Spacer(),
            Icon(isSelected ? Icons.check_circle : Icons.circle_outlined, color: Colors.white),
          ],
        ),
      ),
    );
  }

  // --- CÁC WIDGET PHỤ TRỢ KHÁC CỦA CƯỜNG ---
  Widget _buildSectionTitle(BuildContext context, String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 15, left: 5),
      child: Text(title.toUpperCase(), style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5), fontWeight: FontWeight.bold, fontSize: 12, letterSpacing: 1.1)),
    );
  }

  void _showSuccessDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: const Icon(Icons.check_circle, color: Colors.green, size: 60),
        content: const Text("Thanh toán thành công! Phòng đã được chuyển sang trạng thái dọn dẹp.", textAlign: TextAlign.center),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).popUntil((route) => route.isFirst), child: const Text("HOÀN TẤT")),
        ],
      ),
    );
  }
}