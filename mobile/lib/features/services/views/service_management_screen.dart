import 'package:flutter/material.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';
import 'package:may_hotel_app/features/profile/views/payment_method_screen.dart';

class ServiceManagementScreen extends StatefulWidget {
  final int bookingId;
  final double initialAmount; // Nhận tổng tiền phòng gốc chuyển sang

  const ServiceManagementScreen({
    super.key,
    required this.bookingId,
    required this.initialAmount,
  });

  @override
  State<ServiceManagementScreen> createState() => _ServiceManagementScreenState();
}

class _ServiceManagementScreenState extends State<ServiceManagementScreen> {
  final HotelApiProvider _apiProvider = HotelApiProvider();
  final _quantityController = TextEditingController();

  late double _currentTotalAmount;
  late Future<List<Map<String, dynamic>>> _servicesFuture;
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    _currentTotalAmount = widget.initialAmount;
    // Gọi trực tiếp API lấy danh mục dịch vụ động từ SQL Server
    _servicesFuture = _apiProvider.getAllServices();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      appBar: AppBar(
        title: const Text("Dịch vụ & Tiện ích", style: TextStyle(fontWeight: FontWeight.bold)),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => PaymentMethodScreen(
                    bookingId: widget.bookingId,
                    totalAmount: _currentTotalAmount,
                  ),
                ),
              );
            },
            child: const Text("TIẾP TỤC", style: TextStyle(color: Colors.blue, fontWeight: FontWeight.bold)),
          )
        ],
      ),
      body: Column(
        children: [
          _buildTotalBanner(colorScheme),
          Expanded(
            child: FutureBuilder<List<Map<String, dynamic>>>(
              future: _servicesFuture,
              builder: (context, snapshot) {
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return const Center(child: CircularProgressIndicator());
                }
                if (snapshot.hasError) {
                  return Center(child: Text('Lỗi tải danh mục dịch vụ: ${snapshot.error}'));
                }
                final listServices = snapshot.data ?? [];
                if (listServices.isEmpty) {
                  return const Center(child: Text('Khách sạn hiện tại chưa mở dịch vụ phát sinh.'));
                }

                return ListView.builder(
                  padding: const EdgeInsets.all(20),
                  itemCount: listServices.length,
                  itemBuilder: (context, index) {
                    final item = listServices[index];

                    // 🌟 GIẢI PHÁP CHÍ MẠNG: Bộ gác cổng đa tầng quét sạch cả chữ HOA và chữ thường từ JSON C#
                    final int id = item['MaDV'] ?? item['maDV'] ?? item['MaDv'] ?? item['maDv'] ?? 0;
                    final String name = item['TenDV'] ?? item['tenDV'] ?? item['TenDv'] ?? item['tenDv'] ?? 'Dịch vụ chưa đặt tên';

                    // Ép kiểu num an toàn đề phòng C# trả về định dạng int hoặc double
                    final num rawPrice = item['GiaTien'] ?? item['giaTien'] ?? item['GiaTien'] ?? item['giaTien'] ?? 0;
                    final double price = rawPrice.toDouble();

                    return _buildDynamicServiceItem(context, id, name, price);
                  },
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTotalBanner(ColorScheme colorScheme) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(vertical: 15, horizontal: 20),
      color: colorScheme.primaryContainer.withOpacity(0.3),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          const Text("Tổng thanh toán hiện tại:", style: TextStyle(fontWeight: FontWeight.w600)),
          Text(
            "${_currentTotalAmount.toStringAsFixed(0)} VNĐ",
            style: TextStyle(color: colorScheme.primary, fontWeight: FontWeight.bold, fontSize: 18),
          ),
        ],
      ),
    );
  }

  Widget _buildDynamicServiceItem(BuildContext context, int id, String name, double price) {
    final colorScheme = Theme.of(context).colorScheme;
    return Card(
      margin: const EdgeInsets.only(bottom: 15),
      color: colorScheme.surface,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
      child: ListTile(
        leading: Icon(Icons.star_border_purple500_rounded, color: colorScheme.primary, size: 30),
        title: Text(name, style: const TextStyle(fontWeight: FontWeight.bold)),
        subtitle: Text(
          "${price.toStringAsFixed(0)} VNĐ",
          style: TextStyle(color: colorScheme.primary, fontWeight: FontWeight.w500),
        ),
        trailing: ElevatedButton(
          onPressed: () => _showOrderDialog(context, id, name),
          style: ElevatedButton.styleFrom(backgroundColor: colorScheme.primary),
          child: const Text("Đặt", style: TextStyle(color: Colors.white)),
        ),
      ),
    );
  }

  void _showOrderDialog(BuildContext context, int maDichVu, String serviceName) {
    _quantityController.clear();
    final colorScheme = Theme.of(context).colorScheme;

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => StatefulBuilder(
        builder: (context, setStateDialog) {
          return AlertDialog(
            backgroundColor: colorScheme.surface,
            title: Text("Đặt dịch vụ: $serviceName"),
            content: TextField(
              controller: _quantityController,
              keyboardType: TextInputType.number,
              decoration: InputDecoration(
                labelText: "Số lượng yêu cầu",
                prefixIcon: const Icon(Icons.add_shopping_cart),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
              ),
            ),
            actions: [
              TextButton(
                onPressed: _isSubmitting ? null : () => Navigator.pop(context),
                child: const Text("Hủy"),
              ),
              ElevatedButton(
                onPressed: _isSubmitting ? null : () => _submitService(context, maDichVu, setStateDialog),
                style: ElevatedButton.styleFrom(backgroundColor: colorScheme.primary),
                child: const Text("Xác nhận", style: TextStyle(color: Colors.white)),
              ),
            ],
          );
        },
      ),
    );
  }

  Future<void> _submitService(BuildContext context, int maDichVu, StateSetter setStateDialog) async {
    final soLuong = int.tryParse(_quantityController.text.trim()) ?? 0;
    if (soLuong <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Vui lòng nhập số lượng lớn hơn 0')));
      return;
    }

    setStateDialog(() => _isSubmitting = true);

    try {
      final res = await _apiProvider.addServiceToBooking(
        bookingId: widget.bookingId,
        maDichVu: maDichVu,
        soLuong: soLuong,
        donGia: 0.0,
      );

      if (res['success'] == true) {
        setState(() {
          _currentTotalAmount = (res['totalAmount'] as num).toDouble();
        });
        if (!context.mounted) return;
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Thêm dịch vụ thành công!')));
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(res['message']?.toString() ?? 'Thêm dịch vụ thất bại')),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    } finally {
      setStateDialog(() => _isSubmitting = false);
    }
  }
}