import 'dart:async';

import 'package:flutter/material.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';
import 'package:url_launcher/url_launcher.dart';

class PaymentMethodScreen extends StatefulWidget {
  final int bookingId;
  final int invoiceId;
  final double totalAmount;
  final double depositAmount;
  final double remainingAmount;

  const PaymentMethodScreen({
    super.key,
    required this.bookingId,
    this.invoiceId = 0,
    required this.totalAmount,
    this.depositAmount = 0,
    this.remainingAmount = 0,
  });

  @override
  State<PaymentMethodScreen> createState() => _PaymentMethodScreenState();
}

class _PaymentMethodScreenState extends State<PaymentMethodScreen> {
  final HotelApiProvider _api = HotelApiProvider();
  Timer? _pollTimer;
  bool _processing = false;
  String _status = 'PENDING';

  String _money(double value) =>
      '${value.round().toString().replaceAllMapped(RegExp(r'\B(?=(\d{3})+(?!\d))'), (m) => '.')} đ';

  Future<void> _openVnPay() async {
    if (widget.invoiceId <= 0) {
      _message('Không tìm thấy hóa đơn cọc của đơn đặt phòng.');
      return;
    }
    setState(() => _processing = true);
    try {
      final result = await _api.createVnPayDepositUrl(widget.invoiceId);
      final rawUrl = result['paymentUrl']?.toString();
      final uri = rawUrl == null ? null : Uri.tryParse(rawUrl);
      if (uri == null ||
          !await launchUrl(uri, mode: LaunchMode.externalApplication)) {
        throw Exception('Không thể mở cổng VNPay');
      }
      _startPolling();
    } catch (error) {
      _message('Không thể tạo giao dịch VNPay: $error');
    } finally {
      if (mounted) setState(() => _processing = false);
    }
  }

  void _startPolling() {
    _pollTimer?.cancel();
    _pollTimer = Timer.periodic(
      const Duration(seconds: 2),
      (_) => _pollStatus(),
    );
    _pollStatus();
  }

  Future<void> _pollStatus() async {
    try {
      final result = await _api.getDepositStatus(widget.invoiceId);
      if (!mounted) return;
      final status = result['depositStatus']?.toString() ?? 'PENDING';
      setState(() => _status = status);
      if (result['confirmed'] == true) {
        _pollTimer?.cancel();
        await showDialog<void>(
          context: context,
          barrierDismissible: false,
          builder: (dialogContext) => AlertDialog(
            icon: const Icon(Icons.check_circle, color: Colors.green, size: 54),
            title: const Text('Đặt cọc thành công'),
            content: const Text(
              'Đơn đặt phòng đã được xác nhận. Phần còn lại thanh toán tại khách sạn.',
              textAlign: TextAlign.center,
            ),
            actions: [
              FilledButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: const Text('Hoàn tất'),
              ),
            ],
          ),
        );
        if (mounted) Navigator.of(context).popUntil((route) => route.isFirst);
      } else if (status == 'EXPIRED') {
        _pollTimer?.cancel();
        _message('Đã hết 15 phút giữ chỗ. Vui lòng tạo lại đơn đặt phòng.');
      }
    } catch (_) {
      // Mất mạng tạm thời không được coi là thanh toán thất bại; lần poll sau sẽ thử lại.
    }
  }

  void _message(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..removeCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final deposit = widget.depositAmount > 0
        ? widget.depositAmount
        : widget.totalAmount * .10;
    final remaining = widget.remainingAmount > 0
        ? widget.remainingAmount
        : widget.totalAmount - deposit;
    return Scaffold(
      appBar: AppBar(title: const Text('Đặt cọc phòng')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          const Icon(
            Icons.verified_user_outlined,
            size: 64,
            color: Color(0xFFD4AF37),
          ),
          const SizedBox(height: 16),
          const Text(
            'Đặt cọc 10% qua VNPay',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 8),
          const Text(
            'Bạn có 15 phút để hoàn tất. Mã đặt phòng chỉ được xác nhận khi backend nhận callback hợp lệ từ VNPay.',
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 24),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                children: [
                  _row('Tổng giá trị đơn', _money(widget.totalAmount)),
                  const Divider(height: 26),
                  _row('Cọc VNPay (10%)', _money(deposit), emphasized: true),
                  const SizedBox(height: 12),
                  _row('Còn lại tại khách sạn', _money(remaining)),
                  const SizedBox(height: 12),
                  _row('Trạng thái', _status),
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),
          FilledButton.icon(
            onPressed: _processing ? null : _openVnPay,
            icon: _processing
                ? const SizedBox.square(
                    dimension: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.open_in_browser),
            label: const Padding(
              padding: EdgeInsets.symmetric(vertical: 15),
              child: Text('MỞ VNPAY ĐỂ ĐẶT CỌC'),
            ),
          ),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: _pollStatus,
            icon: const Icon(Icons.refresh),
            label: const Text('Tôi đã thanh toán – kiểm tra lại'),
          ),
        ],
      ),
    );
  }

  Widget _row(String label, String value, {bool emphasized = false}) {
    return Row(
      children: [
        Expanded(child: Text(label)),
        Text(
          value,
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: emphasized ? 20 : 15,
            color: emphasized ? const Color(0xFFD4AF37) : null,
          ),
        ),
      ],
    );
  }
}
