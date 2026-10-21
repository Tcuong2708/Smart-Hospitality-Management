import 'package:flutter/material.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';

class ServiceOrderScreen extends StatefulWidget {
  final int bookingId;
  const ServiceOrderScreen({super.key, required this.bookingId});

  @override
  State<ServiceOrderScreen> createState() => _ServiceOrderScreenState();
}

class _ServiceOrderScreenState extends State<ServiceOrderScreen> {
  final HotelApiProvider _apiProvider = HotelApiProvider();

  List<dynamic> _services = [];
  final Map<int, Map<String, dynamic>> _cart = {};
  bool _isLoading = true;
  bool _isSubmittingOrder = false;

  @override
  void initState() {
    super.initState();
    _fetchServices();
  }

  Future<void> _fetchServices() async {
    final list = await _apiProvider.getServices();
    if (mounted) {
      setState(() {
        _services = list;
        _isLoading = false;
      });
    }
  }

  // Khách chỉ gửi yêu cầu; quyền xem trước và in thuộc nhân viên dịch vụ.
  void _orderService(dynamic service) {
    final int serviceId = int.tryParse(service['id']?.toString() ?? '') ?? 0;
    int qty = (_cart[serviceId]?['quantity'] as int?) ?? 1;
    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) {
          return AlertDialog(
            title: Text("Đặt: ${service['name']}"),
            content: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                IconButton(
                  icon: const Icon(Icons.remove_circle_outline),
                  onPressed: () {
                    if (qty > 1) setDialogState(() => qty--);
                  },
                ),
                Text(
                  "$qty",
                  style: const TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.add_circle_outline),
                  onPressed: () {
                    setDialogState(() => qty++);
                  },
                ),
              ],
            ),
            actions: [
              if (_cart.containsKey(serviceId))
                TextButton(
                  onPressed: () {
                    Navigator.pop(ctx);
                    setState(() => _cart.remove(serviceId));
                  },
                  child: const Text("Xóa khỏi giỏ"),
                ),
              TextButton(
                onPressed: () => Navigator.pop(ctx),
                child: const Text("Hủy"),
              ),
              ElevatedButton(
                onPressed: () async {
                  Navigator.pop(ctx);
                  if (serviceId <= 0) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text("Dịch vụ không hợp lệ")),
                    );
                    return;
                  }
                  setState(() {
                    _cart[serviceId] = {
                      'serviceId': serviceId,
                      'name': service['name']?.toString() ?? 'Dịch vụ',
                      'price':
                          double.tryParse(
                            service['price']?.toString() ?? '0',
                          ) ??
                          0.0,
                      'quantity': qty,
                    };
                  });
                },
                child: const Text("Thêm vào giỏ"),
              ),
            ],
          );
        },
      ),
    );
  }

  int get _cartQuantity => _cart.values.fold<int>(
    0,
    (total, item) => total + ((item['quantity'] as int?) ?? 0),
  );

  double get _cartTotal => _cart.values.fold<double>(
    0,
    (total, item) =>
        total +
        ((item['price'] as num?)?.toDouble() ?? 0.0) *
            ((item['quantity'] as int?) ?? 0),
  );

  Future<void> _processOrder() async {
    if (_cart.isEmpty || _isSubmittingOrder) return;
    setState(() => _isSubmittingOrder = true);
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => const Center(child: CircularProgressIndicator()),
    );

    final requestItems = _cart.values
        .map(
          (item) => <String, dynamic>{
            'serviceId': item['serviceId'],
            'quantity': item['quantity'],
          },
        )
        .toList();

    Map<String, dynamic>? receiptData;
    String? orderError;
    try {
      receiptData = await _apiProvider.orderMobileServices(
        widget.bookingId,
        requestItems,
      );
    } catch (error) {
      orderError = error.toString();
    }

    if (!mounted) return;
    Navigator.pop(context);
    setState(() => _isSubmittingOrder = false);

    if (receiptData == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(orderError ?? "Lỗi khi đặt các dịch vụ!"),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    setState(_cart.clear);
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        titlePadding: const EdgeInsets.fromLTRB(24, 24, 24, 8),
        contentPadding: const EdgeInsets.fromLTRB(24, 8, 24, 8),
        actionsPadding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
        title: const Row(
          children: [
            CircleAvatar(
              backgroundColor: Color(0xFFE8F5E9),
              child: Icon(Icons.check_rounded, color: Color(0xFF1B8D57)),
            ),
            SizedBox(width: 12),
            Expanded(child: Text('Đã gửi yêu cầu')),
          ],
        ),
        content: const Text(
          'Yêu cầu dịch vụ đã được chuyển đến quầy dịch vụ. '
          'Nhân viên sẽ tiếp nhận, xử lý và in hóa đơn nhiệt tại quầy.',
          style: TextStyle(fontSize: 16, height: 1.45),
        ),
        actions: [
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('Đã hiểu'),
          ),
        ],
      ),
    );
  }

  String _formatMoney(dynamic value) {
    final String digits = ((value as num?)?.round() ?? 0).toString();
    final buffer = StringBuffer();
    for (int i = 0; i < digits.length; i++) {
      if (i > 0 && (digits.length - i) % 3 == 0) buffer.write('.');
      buffer.write(digits[i]);
    }
    return buffer.toString();
  }

  Widget _buildCartBar(ColorScheme colorScheme) {
    if (_cart.isEmpty) return const SizedBox.shrink();
    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
        decoration: BoxDecoration(
          color: colorScheme.surface,
          boxShadow: const [
            BoxShadow(
              color: Colors.black12,
              blurRadius: 8,
              offset: Offset(0, -2),
            ),
          ],
        ),
        child: Row(
          children: [
            IconButton(
              tooltip: 'Xóa giỏ dịch vụ',
              onPressed: _isSubmittingOrder
                  ? null
                  : () => setState(_cart.clear),
              icon: const Icon(Icons.delete_outline),
            ),
            Expanded(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '${_cart.length} dịch vụ • $_cartQuantity sản phẩm',
                    style: const TextStyle(fontWeight: FontWeight.w600),
                  ),
                  Text(
                    '${_formatMoney(_cartTotal)} VNĐ',
                    style: TextStyle(
                      color: colorScheme.primary,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
            ),
            ElevatedButton.icon(
              onPressed: _isSubmittingOrder ? null : _processOrder,
              icon: const Icon(Icons.receipt_long),
              label: const Text('Xác nhận'),
            ),
          ],
        ),
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
        title: const Text("Đặt Dịch Vụ Khách Sạn"),
        backgroundColor: colorScheme.surface,
        foregroundColor: colorScheme.onSurface,
        elevation: 0.5,
      ),
      bottomNavigationBar: _buildCartBar(colorScheme),
      body: Column(
        children: [
          Container(
            margin: const EdgeInsets.fromLTRB(16, 12, 16, 0),
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: const Color(0xFFFFF8DF),
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: const Color(0xFFE2BD35)),
            ),
            child: const Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(
                  Icons.notifications_active_outlined,
                  color: Color(0xFFB38A00),
                ),
                SizedBox(width: 10),
                Expanded(
                  child: Text(
                    'Sau khi xác nhận, yêu cầu sẽ được gửi trực tiếp đến quầy dịch vụ. '
                    'Nhân viên phụ trách sẽ tiếp nhận và in hóa đơn nhiệt.',
                    style: TextStyle(height: 1.35, color: Color(0xFF594600)),
                  ),
                ),
              ],
            ),
          ),

          // DANH SÁCH DỊCH VỤ
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : GridView.builder(
                    padding: const EdgeInsets.all(16),
                    gridDelegate:
                        const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 2,
                          childAspectRatio: 0.78,
                          crossAxisSpacing: 15,
                          mainAxisSpacing: 15,
                        ),
                    itemCount: _services.length,
                    itemBuilder: (context, index) {
                      final srv = _services[index];
                      double price =
                          double.tryParse(srv['price']?.toString() ?? '0') ??
                          0.0;
                      final serviceId =
                          int.tryParse(srv['id']?.toString() ?? '') ?? 0;
                      final selectedQuantity =
                          (_cart[serviceId]?['quantity'] as int?) ?? 0;

                      return Card(
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(15),
                        ),
                        elevation: 3,
                        child: InkWell(
                          borderRadius: BorderRadius.circular(15),
                          onTap: () => _orderService(srv),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              Expanded(
                                child: ClipRRect(
                                  borderRadius: const BorderRadius.vertical(
                                    top: Radius.circular(15),
                                  ),
                                  child: Container(
                                    color: Colors.blue.withValues(alpha: 0.1),
                                    child: const Icon(
                                      Icons.room_service,
                                      size: 50,
                                      color: Colors.blue,
                                    ),
                                  ),
                                ),
                              ),
                              Padding(
                                padding: const EdgeInsets.all(12),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      srv['name'] ?? "Dịch vụ",
                                      style: const TextStyle(
                                        fontWeight: FontWeight.bold,
                                        fontSize: 16,
                                      ),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                    const SizedBox(height: 5),
                                    Text(
                                      "${price.toStringAsFixed(0)} VNĐ",
                                      style: TextStyle(
                                        color: colorScheme.primary,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                    if (selectedQuantity > 0) ...[
                                      const SizedBox(height: 4),
                                      Text(
                                        'Đã chọn: $selectedQuantity',
                                        style: const TextStyle(
                                          color: Colors.green,
                                          fontWeight: FontWeight.w600,
                                        ),
                                      ),
                                    ],
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}
