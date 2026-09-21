import 'package:flutter/material.dart';
import 'package:flutter/services.dart'; // 🎯 BỔ SUNG: Điều phối màu sắc icon pin/đồng hồ hệ thống
import 'package:may_hotel_app/services/hotel_api_provider.dart';
import 'package:may_hotel_app/features/profile/views/payment_method_screen.dart';
import 'package:may_hotel_app/features/services/views/service_management_screen.dart';
import '../../../core/utils/responsive.dart'; // 📐 Tối ưu kích thước co giãn màn hình

class BookingScreen extends StatefulWidget {
  final String roomTitle;
  final String roomPrice;
  final String imageUrl;
  final int maPhong;

  const BookingScreen({
    super.key,
    required this.roomTitle,
    required this.roomPrice,
    required this.imageUrl,
    required this.maPhong,
  });

  @override
  State<BookingScreen> createState() => _BookingScreenState();
}

class _BookingScreenState extends State<BookingScreen> {
  DateTimeRange? _selectedDateRange;
  final _nameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _nationalityController = TextEditingController(text: "Việt Nam");
  final _addressController = TextEditingController();
  final HotelApiProvider _apiProvider = HotelApiProvider();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final bool isDarkMode = theme.brightness == Brightness.dark;

    int nights = _selectedDateRange?.duration.inDays ?? 0;
    double total = nights * double.parse(widget.roomPrice);

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      // 🎯 Đfont ĐỒNG BỘ APP BAR & THANH TRẠNG THÁI HỆ THỐNG
      appBar: AppBar(
        backgroundColor: colorScheme.surface,
        elevation: 0.5,
        centerTitle: true,
        leading: BackButton(color: colorScheme.onSurface),
        title: Text(
          "Thông tin đặt phòng",
          style: TextStyle(
            color: colorScheme.onSurface,
            fontWeight: FontWeight.bold,
            fontSize: Responsive.sp(context, 18),
          ),
        ),
        systemOverlayStyle: SystemUiOverlayStyle(
          statusBarColor: Colors.transparent,
          statusBarIconBrightness: isDarkMode ? Brightness.light : Brightness.dark,
          statusBarBrightness: isDarkMode ? Brightness.dark : Brightness.light,
        ),
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.all(Responsive.sp(context, 20)),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildRoomSummary(colorScheme, isDarkMode),
            Divider(height: 40, color: colorScheme.outlineVariant),

            Text(
              "Thời gian lưu trú",
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: Responsive.sp(context, 16), color: colorScheme.onSurface),
            ),
            const SizedBox(height: 12),
            _buildDatePicker(context, colorScheme, isDarkMode),

            const SizedBox(height: 30),

            Text(
              "Thông tin người đặt",
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: Responsive.sp(context, 16), color: colorScheme.onSurface),
            ),
            const SizedBox(height: 15),
            _buildTextField(context, _nameController, "Họ và tên *", Icons.person),
            const SizedBox(height: 15),
            _buildTextField(
              context,
              _phoneController,
              "Số điện thoại *",
              Icons.phone_android_outlined,
              inputType: TextInputType.phone,
            ),
            const SizedBox(height: 15),
            _buildTextField(context, _nationalityController, "Quốc tịch", Icons.flag_outlined),
            const SizedBox(height: 15),
            _buildTextField(
              context,
              _addressController,
              "Địa chỉ",
              Icons.map_outlined,
              maxLines: 2,
            ),
          ],
        ),
      ),
      bottomNavigationBar: _buildBottomConfirm(context, colorScheme, isDarkMode, nights, total),
    );
  }

  Widget _buildRoomSummary(ColorScheme colorScheme, bool isDarkMode) {
    String cleanImageUrl = widget.imageUrl;
    if (cleanImageUrl.startsWith('assets/images/http')) {
      cleanImageUrl = cleanImageUrl.replaceFirst('assets/images/', '');
    }

    final bool isNetworkImage = cleanImageUrl.startsWith('http');
    String maskedRoomTitle = widget.roomTitle.replaceAll(RegExp(r'P\d+'), 'P***');

    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(15),
          child: isNetworkImage
              ? Image.network(
            cleanImageUrl,
            width: 90,
            height: 90,
            fit: BoxFit.cover,
            errorBuilder: (c, e, s) => Container(
              width: 90,
              height: 90,
              color: colorScheme.surfaceVariant,
              child: Icon(Icons.hotel, color: colorScheme.onSurfaceVariant),
            ),
          )
              : Image.asset(
            cleanImageUrl,
            width: 90,
            height: 90,
            fit: BoxFit.cover,
            errorBuilder: (c, e, s) => Container(
              width: 90,
              height: 90,
              color: colorScheme.surfaceVariant,
              child: Icon(Icons.hotel, color: colorScheme.onSurfaceVariant),
            ),
          ),
        ),
        const SizedBox(width: 15),

        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                maskedRoomTitle,
                style: TextStyle(fontSize: Responsive.sp(context, 17), fontWeight: FontWeight.bold, color: colorScheme.onSurface),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 6),
              Text(
                "${double.parse(widget.roomPrice).toStringAsFixed(0)} VNĐ/đêm",
                style: TextStyle(color: colorScheme.onSurface.withOpacity(0.6), fontWeight: FontWeight.w600, fontSize: 14),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildDatePicker(BuildContext context, ColorScheme colorScheme, bool isDarkMode) {
    return Container(
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(15),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(isDarkMode ? 0.3 : 0.05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          )
        ],
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(15),
        onTap: () async {
          final picked = await showDateRangePicker(
            context: context,
            firstDate: DateTime.now(),
            lastDate: DateTime.now().add(const Duration(days: 365)),
            builder: (context, child) {
              return Theme(
                data: Theme.of(context).copyWith(
                  colorScheme: colorScheme.copyWith(
                    primary: colorScheme.primary,
                    onPrimary: Colors.white,
                    surface: colorScheme.surface,
                    onSurface: colorScheme.onSurface,
                  ),
                ),
                child: child!,
              );
            },
          );
          if (picked != null) setState(() => _selectedDateRange = picked);
        },
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
          child: Row(
            children: [
              Icon(Icons.calendar_month_outlined, color: isDarkMode ? Colors.amber : colorScheme.primary, size: 22),
              const SizedBox(width: 15),
              Expanded(
                child: Text(
                  _selectedDateRange == null
                      ? "Chọn ngày Check-in - Check-out"
                      : "${_selectedDateRange!.start.day}/${_selectedDateRange!.start.month} - ${_selectedDateRange!.end.day}/${_selectedDateRange!.end.month}",
                  style: TextStyle(
                      color: _selectedDateRange == null ? colorScheme.onSurface.withOpacity(0.4) : colorScheme.onSurface,
                      fontWeight: _selectedDateRange == null ? FontWeight.normal : FontWeight.bold,
                      fontSize: 14
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTextField(BuildContext context, TextEditingController controller, String hint, IconData icon, {TextInputType inputType = TextInputType.text, int maxLines = 1}) {
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
        keyboardType: inputType,
        maxLines: maxLines,
        style: TextStyle(color: colorScheme.onSurface),
        decoration: InputDecoration(
          prefixIcon: Icon(icon, color: colorScheme.primary.withOpacity(0.7), size: 22),
          hintText: hint,
          hintStyle: TextStyle(color: colorScheme.onSurface.withOpacity(0.4), fontSize: 14),
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(vertical: 16, horizontal: 12),
        ),
      ),
    );
  }

  Widget _buildBottomConfirm(BuildContext context, ColorScheme colorScheme, bool isDarkMode, int nights, double total) {
    return Container(
      padding: EdgeInsets.all(Responsive.sp(context, 20)),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        boxShadow: [
          BoxShadow(
              color: Colors.black.withOpacity(isDarkMode ? 0.4 : 0.08),
              blurRadius: 12,
              offset: const Offset(0, -4)
          )
        ],
      ),
      child: SafeArea(
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text("Tổng cộng ($nights đêm)", style: TextStyle(color: colorScheme.onSurface.withOpacity(0.5), fontSize: 13, fontWeight: FontWeight.w500)),
                const SizedBox(height: 4),
                Text(
                  "${total.toStringAsFixed(0)} VNĐ",
                  style: TextStyle(
                      fontSize: Responsive.sp(context, 20),
                      fontWeight: FontWeight.bold,
                      color: isDarkMode ? Colors.amber : colorScheme.primary
                  ),
                ),
              ],
            ),
            ElevatedButton(
              onPressed: (nights > 0 && _nameController.text.isNotEmpty)
                  ? () => _createBookingAndNavigate(total)
                  : () {
                ScaffoldMessenger.of(context).removeCurrentSnackBar();
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: const Text('⚠️ Vui lòng chọn ngày và nhập Họ và tên.'),
                    backgroundColor: Colors.orange,
                    behavior: SnackBarBehavior.floating,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                );
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: colorScheme.primary,
                padding: EdgeInsets.symmetric(horizontal: Responsive.width(context) * 0.08, vertical: 15),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
                elevation: 3,
              ),
              child: const Text("Xác nhận", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _createBookingAndNavigate(double total) async {
    final nights = _selectedDateRange?.duration.inDays ?? 0;
    if (nights <= 0) return;

    final hoTen = _nameController.text.trim();
    final dienThoai = _phoneController.text.trim();
    final quocTich = _nationalityController.text.trim();
    final diaChi = _addressController.text.trim();

    if (hoTen.isEmpty || dienThoai.isEmpty) {
      ScaffoldMessenger.of(context).removeCurrentSnackBar();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text('⚠️ Vui lòng nhập họ tên và số điện thoại'),
          backgroundColor: Colors.orange,
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
      );
      return;
    }

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => const Center(child: CircularProgressIndicator()),
    );

    try {
      const int maKHHardcode = 3;
      final res = await _apiProvider.createBooking(
        maPhong: widget.maPhong,
        maKH: maKHHardcode,
        ngayNhanPhong: _selectedDateRange!.start,
        ngayTraPhong: _selectedDateRange!.end,
        soNguoi: 1,
        ghiChu: 'Dien thoai: $dienThoai, Quoc tich: $quocTich, Dia chi: $diaChi',
      );

      if (!context.mounted) return;
      Navigator.pop(context);

      if (res['success'] == true) {
        final int generatedBookingId = res['bookingId'] ?? 0;
        final double currentTotal = res['totalAmount'] ?? total;

        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => ServiceManagementScreen(
              bookingId: generatedBookingId,
              initialAmount: currentTotal,
            ),
          ),
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(res['message']?.toString() ?? 'Tạo booking thất bại'),
            backgroundColor: Colors.red,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    } catch (e) {
      if (context.mounted) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Lỗi: $e'),
            backgroundColor: Colors.red,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    }
  }
}