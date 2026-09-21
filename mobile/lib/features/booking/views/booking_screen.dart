import 'package:flutter/material.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';
import 'package:may_hotel_app/features/profile/views/payment_method_screen.dart';
import 'package:may_hotel_app/features/services/views/service_management_screen.dart';

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
    int nights = _selectedDateRange?.duration.inDays ?? 0;
    double total = nights * double.parse(widget.roomPrice);

    return Scaffold(
      appBar: AppBar(
        title: const Text("Thông tin đặt phòng"),
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildRoomSummary(),
            const Divider(height: 40),
            const Text(
              "Thời gian lưu trú",
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(height: 10),
            _buildDatePicker(),
            const SizedBox(height: 25),
            const Text(
              "Thông tin người đặt",
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(height: 15),
            _buildTextField(_nameController, "Họ và tên *", Icons.person),
            const SizedBox(height: 15),
            _buildTextField(
              _phoneController,
              "Số điện thoại *",
              Icons.phone,
              inputType: TextInputType.phone,
            ),
            const SizedBox(height: 15),
            _buildTextField(_nationalityController, "Quốc tịch", Icons.flag),
            const SizedBox(height: 15),
            _buildTextField(_addressController, "Địa chỉ", Icons.map, maxLines: 2),
          ],
        ),
      ),
      bottomNavigationBar: _buildBottomConfirm(nights, total),
    );
  }

  Widget _buildRoomSummary() {
    return Row(
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(15),
          child: Image.asset(
            widget.imageUrl,
            width: 100,
            height: 100,
            fit: BoxFit.cover,
          ),
        ),
        const SizedBox(width: 15),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                widget.roomTitle,
                style: const TextStyle(fontSize: 18, fontStyle: FontStyle.normal, fontWeight: FontWeight.bold),
              ),
              Text(
                "\$${widget.roomPrice}/night",
                style: const TextStyle(color: Colors.grey),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildDatePicker() {
    return InkWell(
      onTap: () async {
        final picked = await showDateRangePicker(
          context: context,
          firstDate: DateTime.now(),
          lastDate: DateTime.now().add(const Duration(days: 365)),
        );
        if (picked != null) setState(() => _selectedDateRange = picked);
      },
      child: Container(
        padding: const EdgeInsets.all(15),
        decoration: BoxDecoration(
          color: Colors.grey[100],
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          children: [
            const Icon(Icons.calendar_month, color: Colors.black54),
            const SizedBox(width: 15),
            Text(
              _selectedDateRange == null
                  ? "Chọn ngày Check-in - Check-out"
                  : "${_selectedDateRange!.start.day}/${_selectedDateRange!.start.month} - ${_selectedDateRange!.end.day}/${_selectedDateRange!.end.month}",
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTextField(
      TextEditingController controller,
      String label,
      IconData icon, {
        TextInputType inputType = TextInputType.text,
        int maxLines = 1,
      }) {
    return TextField(
      controller: controller,
      keyboardType: inputType,
      maxLines: maxLines,
      decoration: InputDecoration(
        labelText: label,
        prefixIcon: Icon(icon, size: 20),
        filled: true,
        fillColor: Colors.grey[50],
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: Colors.grey.shade300),
        ),
      ),
    );
  }

  Widget _buildBottomConfirm(int nights, double total) {
    return Container(
      padding: const EdgeInsets.all(25),
      decoration: const BoxDecoration(
        color: Colors.white,
        boxShadow: [BoxShadow(color: Colors.black12, blurRadius: 10, offset: Offset(0, -5))],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text("Tổng cộng ($nights đêm)", style: const TextStyle(color: Colors.grey)),
              Text(
                "\$${total.toStringAsFixed(2)}",
                style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colors.blue),
              ),
            ],
          ),
          ElevatedButton(
            onPressed: (nights > 0 && _nameController.text.isNotEmpty)
                ? () => _createBookingAndNavigate(total)
                : () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Vui lòng chọn ngày và nhập Họ và tên.')),
              );
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.black,
              padding: const EdgeInsets.symmetric(horizontal: 40, vertical: 15),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
            ),
            child: const Text("Xác nhận", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          ),
        ],
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
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vui lòng nhập họ tên và số điện thoại')),
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
      Navigator.pop(context); // Tắt loading spinner

      if (res['success'] == true) {
        // 🌟 ĐÃ SỬA CHÍ MẠNG: Đọc trường 'bookingId' và 'totalAmount' map từ Provider của Cường
        final int generatedBookingId = res['bookingId'] ?? 0;
        final double currentTotal = res['totalAmount'] ?? total;

        // Luân chuyển luồng mượt mà sang khay chọn dịch vụ
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
          SnackBar(content: Text(res['message']?.toString() ?? 'Tạo booking thất bại')),
        );
      }
    } catch (e) {
      if (context.mounted) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    }
  }
}