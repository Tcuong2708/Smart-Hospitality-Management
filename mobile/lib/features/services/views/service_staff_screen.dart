import 'dart:async';
import 'dart:typed_data';

import 'package:blue_thermal_printer/blue_thermal_printer.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:image/image.dart' as img;
import 'package:may_hotel_app/services/hotel_api_client.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:screenshot/screenshot.dart';

class ServiceStaffScreen extends StatefulWidget {
  const ServiceStaffScreen({super.key});

  @override
  State<ServiceStaffScreen> createState() => _ServiceStaffScreenState();
}

class _ServiceStaffScreenState extends State<ServiceStaffScreen> {
  static const Color _navy = Color(0xFF102F4D);
  static const Color _gold = Color(0xFFD5AA19);

  final Dio _dio = HotelApiClient.instance.dio;
  final BlueThermalPrinter _bluetooth = BlueThermalPrinter.instance;
  final ScreenshotController _screenshotController = ScreenshotController();

  late Future<List<_ServiceRequest>> _requests;
  Timer? _pollTimer;
  String _filter = 'ACTIVE';
  bool _isUpdating = false;
  List<BluetoothDevice> _devices = [];
  BluetoothDevice? _selectedDevice;
  bool _isConnected = false;

  @override
  void initState() {
    super.initState();
    _requests = _loadRequests();
    _initPrinter();
    _pollTimer = Timer.periodic(
      const Duration(seconds: 15),
      (_) => _refreshSilently(),
    );
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    super.dispose();
  }

  Future<void> _refreshSilently() async {
    if (!mounted || _isUpdating) return;
    setState(() => _requests = _loadRequests());
  }

  Future<List<_ServiceRequest>> _loadRequests() async {
    final response = await _dio.get('/api/v1/mobile/service-staff/tickets');
    final raw = response.data is Map ? response.data['data'] : null;
    if (raw is! List) return [];

    final grouped = <String, List<Map<String, dynamic>>>{};
    for (final rawItem in raw) {
      if (rawItem is! Map) continue;
      final item = Map<String, dynamic>.from(rawItem);
      final ticketId = item['id']?.toString() ?? '';
      final groupId = item['requestGroupId']?.toString().trim();
      final key = groupId != null && groupId.isNotEmpty
          ? groupId
          : 'LEGACY-$ticketId';
      grouped.putIfAbsent(key, () => []).add(item);
    }

    return grouped.entries
        .map((entry) => _ServiceRequest.fromTickets(entry.key, entry.value))
        .toList()
      ..sort((a, b) => b.sortValue.compareTo(a.sortValue));
  }

  Future<void> _initPrinter() async {
    await [
      Permission.bluetoothScan,
      Permission.bluetoothConnect,
      Permission.location,
    ].request();
    try {
      final devices = await _bluetooth.getBondedDevices();
      final connected = await _bluetooth.isConnected == true;
      if (!mounted) return;
      setState(() {
        _devices = devices;
        _isConnected = connected;
        for (final device in devices) {
          if ((device.name ?? '').toUpperCase().contains('RPP')) {
            _selectedDevice = device;
            break;
          }
        }
      });
    } catch (error) {
      debugPrint('Không thể tải máy in Bluetooth: $error');
    }
  }

  Future<void> _togglePrinter() async {
    if (_isConnected) {
      await _bluetooth.disconnect();
      if (mounted) setState(() => _isConnected = false);
      return;
    }
    if (_selectedDevice == null) {
      _showMessage('Vui lòng chọn máy in nhiệt.', isError: true);
      return;
    }
    try {
      await _bluetooth.connect(_selectedDevice!);
      if (!mounted) return;
      setState(() => _isConnected = true);
      _showMessage('Đã kết nối máy in nhiệt.');
    } catch (_) {
      if (mounted) setState(() => _isConnected = false);
      _showMessage('Không thể kết nối máy in.', isError: true);
    }
  }

  Future<void> _updateRequestStatus(
    _ServiceRequest request,
    String status,
  ) async {
    if (_isUpdating) return;
    setState(() => _isUpdating = true);
    try {
      for (final ticket in request.items) {
        final id = int.tryParse(ticket['id']?.toString() ?? '');
        if (id == null) continue;
        await _dio.patch(
          '/api/v1/mobile/service-staff/tickets/$id/status',
          data: {'status': status},
        );
      }
      if (!mounted) return;
      setState(() => _requests = _loadRequests());
      _showMessage('Đã cập nhật trạng thái phiếu dịch vụ.');
    } on DioException catch (error) {
      _showMessage(_apiError(error), isError: true);
    } finally {
      if (mounted) setState(() => _isUpdating = false);
    }
  }

  String _apiError(DioException error) {
    final data = error.response?.data;
    if (data is Map) {
      return (data['message'] ?? data['error'] ?? 'Không thể cập nhật phiếu.')
          .toString();
    }
    return 'Không thể cập nhật phiếu. Vui lòng thử lại.';
  }

  void _showMessage(String message, {bool isError = false}) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: isError ? Colors.red.shade700 : Colors.green.shade700,
      ),
    );
  }

  Future<void> _previewReceipt(_ServiceRequest request) async {
    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (_) => const Center(child: CircularProgressIndicator()),
    );
    try {
      final bytes = await _screenshotController.captureFromWidget(
        _build58mmReceipt(request),
        delay: const Duration(milliseconds: 100),
        pixelRatio: 1,
      );
      if (!mounted) return;
      Navigator.pop(context);
      _showPrintPreview(request, bytes);
    } catch (error) {
      if (mounted) Navigator.pop(context);
      _showMessage('Không thể tạo bản xem trước hóa đơn.', isError: true);
    }
  }

  void _showPrintPreview(_ServiceRequest request, Uint8List imageBytes) {
    showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
        title: const Row(
          children: [
            Icon(Icons.receipt_long_rounded, color: _gold),
            SizedBox(width: 10),
            Text('Xem trước hóa đơn 58 mm'),
          ],
        ),
        content: SizedBox(
          width: 420,
          child: SingleChildScrollView(
            child: DecoratedBox(
              decoration: BoxDecoration(
                border: Border.all(color: Colors.grey.shade300),
              ),
              child: Image.memory(imageBytes),
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('Đóng'),
          ),
          FilledButton.icon(
            style: FilledButton.styleFrom(backgroundColor: _navy),
            onPressed: _isConnected
                ? () async {
                    Navigator.pop(dialogContext);
                    await _printImage(imageBytes);
                    if (request.status == 'PENDING') {
                      await _updateRequestStatus(request, 'IN_PROGRESS');
                    }
                  }
                : null,
            icon: const Icon(Icons.print_rounded),
            label: Text(_isConnected ? 'In hóa đơn' : 'Chưa kết nối máy in'),
          ),
        ],
      ),
    );
  }

  Future<void> _printImage(Uint8List imageBytes) async {
    var printableBytes = imageBytes;
    final decodedImage = img.decodeImage(imageBytes);
    if (decodedImage != null) {
      for (final pixel in decodedImage) {
        final luminance = pixel.r * 0.299 + pixel.g * 0.587 + pixel.b * 0.114;
        final value = luminance > 200 ? 255 : 0;
        pixel.setRgb(value, value, value);
      }
      printableBytes = img.encodePng(decodedImage);
    }
    await _bluetooth.printImageBytes(printableBytes);
    _showMessage('Đã gửi hóa đơn đến máy in.');
  }

  String _formatMoney(dynamic value) {
    final number = value is num
        ? value.round()
        : (num.tryParse(value?.toString() ?? '0') ?? 0).round();
    final digits = number.toString();
    final buffer = StringBuffer();
    for (var index = 0; index < digits.length; index++) {
      if (index > 0 && (digits.length - index) % 3 == 0) buffer.write('.');
      buffer.write(digits[index]);
    }
    return buffer.toString();
  }

  Widget _receiptRow({
    required String name,
    required String quantity,
    required String amount,
    bool header = false,
  }) {
    final style = TextStyle(
      color: Colors.black,
      fontSize: header ? 15 : 16,
      fontWeight: header ? FontWeight.bold : FontWeight.normal,
    );
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(flex: 5, child: Text(name, style: style)),
        Expanded(
          flex: 1,
          child: Text(quantity, textAlign: TextAlign.center, style: style),
        ),
        Expanded(
          flex: 3,
          child: Text(amount, textAlign: TextAlign.right, style: style),
        ),
      ],
    );
  }

  Widget _build58mmReceipt(_ServiceRequest request) {
    return Material(
      color: Colors.white,
      child: Container(
        width: 384,
        color: Colors.white,
        padding: const EdgeInsets.fromLTRB(10, 20, 10, 0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            const Text(
              'MAY HOTEL',
              style: TextStyle(
                color: Colors.black,
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
            ),
            const Text(
              'Tây Thạnh, TP.HCM',
              style: TextStyle(color: Colors.black, fontSize: 16),
            ),
            const Text(
              'Hotline: 0123456789',
              style: TextStyle(color: Colors.black, fontSize: 16),
            ),
            const SizedBox(height: 10),
            const Text(
              '--------------------------------',
              style: TextStyle(color: Colors.black, fontSize: 20),
            ),
            const Text(
              'HÓA ĐƠN DỊCH VỤ',
              style: TextStyle(
                color: Colors.black,
                fontSize: 22,
                fontWeight: FontWeight.bold,
              ),
            ),
            if (request.invoiceId.isNotEmpty)
              Text(
                'Mã hóa đơn: #${request.invoiceId}',
                style: const TextStyle(color: Colors.black, fontSize: 14),
              ),
            if (request.roomName.isNotEmpty)
              Text(
                request.roomName,
                style: const TextStyle(color: Colors.black, fontSize: 14),
              ),
            const SizedBox(height: 15),
            _receiptRow(
              name: 'Tên dịch vụ',
              quantity: 'SL',
              amount: 'T.Tiền',
              header: true,
            ),
            const Divider(color: Colors.black, thickness: 1),
            ...request.items.map(
              (item) => Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: _receiptRow(
                  name: item['serviceName']?.toString() ?? 'Dịch vụ',
                  quantity: item['quantity']?.toString() ?? '1',
                  amount: _formatMoney(item['lineTotal']),
                ),
              ),
            ),
            const SizedBox(height: 10),
            const Text(
              '--------------------------------',
              style: TextStyle(color: Colors.black, fontSize: 20),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'TIỀN DỊCH VỤ:',
                  style: TextStyle(
                    color: Colors.black,
                    fontSize: 17,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                Text(
                  '${_formatMoney(request.serviceTotal)} VNĐ',
                  style: const TextStyle(
                    color: Colors.black,
                    fontSize: 17,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 22),
            const Text(
              'Xin cảm ơn quý khách!',
              style: TextStyle(
                color: Colors.black,
                fontSize: 17,
                fontStyle: FontStyle.italic,
              ),
            ),
            // Máy in 203 dpi cần xấp xỉ 160 dot cho 2 cm giấy chừa để xé.
            const SizedBox(height: 160),
          ],
        ),
      ),
    );
  }

  List<_ServiceRequest> _applyFilter(List<_ServiceRequest> requests) {
    if (_filter == 'ALL') return requests;
    if (_filter == 'ACTIVE') {
      return requests
          .where((request) => request.status != 'COMPLETED')
          .toList();
    }
    return requests.where((request) => request.status == _filter).toList();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF4F6F8),
      appBar: AppBar(
        backgroundColor: _navy,
        foregroundColor: Colors.white,
        title: const Text(
          'Quầy Dịch Vụ',
          style: TextStyle(fontWeight: FontWeight.w700),
        ),
        actions: [
          IconButton(
            tooltip: 'Làm mới',
            onPressed: _refreshSilently,
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      body: FutureBuilder<List<_ServiceRequest>>(
        future: _requests,
        builder: (context, snapshot) {
          final allRequests = snapshot.data ?? const <_ServiceRequest>[];
          final requests = _applyFilter(allRequests);
          return RefreshIndicator(
            onRefresh: _refreshSilently,
            child: CustomScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              slivers: [
                SliverToBoxAdapter(child: _buildHeader(allRequests)),
                SliverToBoxAdapter(child: _buildPrinterPanel()),
                SliverToBoxAdapter(child: _buildFilters()),
                if (snapshot.connectionState == ConnectionState.waiting &&
                    allRequests.isEmpty)
                  const SliverFillRemaining(
                    child: Center(child: CircularProgressIndicator()),
                  )
                else if (snapshot.hasError)
                  SliverFillRemaining(
                    child: _buildEmptyState(
                      Icons.cloud_off_rounded,
                      'Không thể tải yêu cầu dịch vụ',
                      'Kéo xuống để thử lại.',
                    ),
                  )
                else if (requests.isEmpty)
                  SliverFillRemaining(
                    child: _buildEmptyState(
                      Icons.room_service_outlined,
                      'Chưa có yêu cầu phù hợp',
                      'Danh sách tự động cập nhật mỗi 15 giây.',
                    ),
                  )
                else
                  SliverPadding(
                    padding: const EdgeInsets.fromLTRB(16, 4, 16, 28),
                    sliver: SliverList.separated(
                      itemCount: requests.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 12),
                      itemBuilder: (_, index) =>
                          _buildRequestCard(requests[index]),
                    ),
                  ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildHeader(List<_ServiceRequest> requests) {
    final pending = requests.where((item) => item.status == 'PENDING').length;
    final processing = requests
        .where((item) => item.status == 'IN_PROGRESS')
        .length;
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 16, 16, 10),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [_navy, Color(0xFF1E527B)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(20),
        boxShadow: const [
          BoxShadow(
            color: Colors.black12,
            blurRadius: 12,
            offset: Offset(0, 5),
          ),
        ],
      ),
      child: Row(
        children: [
          const CircleAvatar(
            radius: 26,
            backgroundColor: Color(0x33FFFFFF),
            child: Icon(Icons.room_service_rounded, color: _gold, size: 30),
          ),
          const SizedBox(width: 14),
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Yêu cầu tại phòng',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 19,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                SizedBox(height: 4),
                Text(
                  'Tiếp nhận · In phiếu · Hoàn tất',
                  style: TextStyle(color: Colors.white70),
                ),
              ],
            ),
          ),
          _countBadge('$pending', 'Chờ'),
          const SizedBox(width: 8),
          _countBadge('$processing', 'Đang làm'),
        ],
      ),
    );
  }

  Widget _countBadge(String count, String label) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        children: [
          Text(
            count,
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.bold,
              fontSize: 17,
            ),
          ),
          Text(
            label,
            style: const TextStyle(color: Colors.white70, fontSize: 11),
          ),
        ],
      ),
    );
  }

  Widget _buildPrinterPanel() {
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                Icons.print_rounded,
                color: _isConnected ? Colors.green : _navy,
              ),
              const SizedBox(width: 8),
              Text(
                _isConnected ? 'Máy in đã sẵn sàng' : 'Máy in hóa đơn nhiệt',
                style: const TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: DropdownButtonFormField<BluetoothDevice>(
                  key: ValueKey(_selectedDevice),
                  initialValue: _selectedDevice,
                  isExpanded: true,
                  decoration: const InputDecoration(
                    isDense: true,
                    border: OutlineInputBorder(),
                    hintText: 'Chọn máy in',
                  ),
                  items: _devices
                      .map(
                        (device) => DropdownMenuItem(
                          value: device,
                          child: Text(
                            device.name ?? 'Không rõ tên',
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      )
                      .toList(),
                  onChanged: _isConnected
                      ? null
                      : (device) => setState(() => _selectedDevice = device),
                ),
              ),
              const SizedBox(width: 10),
              FilledButton(
                style: FilledButton.styleFrom(
                  backgroundColor: _isConnected ? Colors.red.shade700 : _navy,
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 15,
                  ),
                ),
                onPressed: _togglePrinter,
                child: Text(_isConnected ? 'Ngắt' : 'Kết nối'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildFilters() {
    const filters = <String, String>{
      'ACTIVE': 'Cần xử lý',
      'PENDING': 'Chờ',
      'IN_PROGRESS': 'Đang làm',
      'COMPLETED': 'Hoàn tất',
      'ALL': 'Tất cả',
    };
    return SizedBox(
      height: 54,
      child: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 5),
        scrollDirection: Axis.horizontal,
        children: filters.entries
            .map(
              (entry) => Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ChoiceChip(
                  selected: _filter == entry.key,
                  selectedColor: _gold.withValues(alpha: 0.22),
                  label: Text(entry.value),
                  onSelected: (_) => setState(() => _filter = entry.key),
                ),
              ),
            )
            .toList(),
      ),
    );
  }

  Widget _buildRequestCard(_ServiceRequest request) {
    final statusColor = switch (request.status) {
      'COMPLETED' => Colors.green,
      'IN_PROGRESS' => Colors.orange.shade700,
      _ => Colors.red.shade700,
    };
    final statusLabel = switch (request.status) {
      'COMPLETED' => 'Hoàn tất',
      'IN_PROGRESS' => 'Đang xử lý',
      _ => 'Chờ tiếp nhận',
    };
    return Card(
      elevation: 1,
      color: Colors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: _navy.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Icon(
                    Icons.door_front_door_rounded,
                    color: _navy,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        request.roomName.isNotEmpty
                            ? request.roomName
                            : 'Đơn đặt phòng #${request.bookingId}',
                        style: const TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 17,
                          color: _navy,
                        ),
                      ),
                      if (request.guestName.isNotEmpty)
                        Text(
                          request.guestName,
                          style: TextStyle(color: Colors.grey.shade700),
                        ),
                      Text(
                        request.displayTime,
                        style: TextStyle(
                          color: Colors.grey.shade600,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 6,
                  ),
                  decoration: BoxDecoration(
                    color: statusColor.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    statusLabel,
                    style: TextStyle(
                      color: statusColor,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ],
            ),
            const Divider(height: 24),
            ...request.items.map(
              (item) => Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Row(
                  children: [
                    const Icon(
                      Icons.room_service_outlined,
                      size: 19,
                      color: _gold,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(item['serviceName']?.toString() ?? 'Dịch vụ'),
                    ),
                    Text(
                      'x${item['quantity'] ?? 1}',
                      style: const TextStyle(fontWeight: FontWeight.w600),
                    ),
                    const SizedBox(width: 12),
                    SizedBox(
                      width: 92,
                      child: Text(
                        '${_formatMoney(item['lineTotal'])} đ',
                        textAlign: TextAlign.right,
                        style: const TextStyle(fontWeight: FontWeight.w600),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            Row(
              children: [
                Text(
                  '${request.items.length} dịch vụ',
                  style: TextStyle(color: Colors.grey.shade600),
                ),
                const Spacer(),
                Text(
                  'Tiền dịch vụ: ${_formatMoney(request.serviceTotal)} đ',
                  style: const TextStyle(
                    color: _navy,
                    fontWeight: FontWeight.bold,
                    fontSize: 15,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                OutlinedButton.icon(
                  onPressed: () => _previewReceipt(request),
                  icon: const Icon(Icons.receipt_long_outlined),
                  label: const Text('Xem & in'),
                ),
                const Spacer(),
                if (request.status == 'PENDING')
                  FilledButton.icon(
                    style: FilledButton.styleFrom(backgroundColor: _navy),
                    onPressed: _isUpdating
                        ? null
                        : () => _updateRequestStatus(request, 'IN_PROGRESS'),
                    icon: const Icon(Icons.play_arrow_rounded),
                    label: const Text('Tiếp nhận'),
                  )
                else if (request.status == 'IN_PROGRESS')
                  FilledButton.icon(
                    style: FilledButton.styleFrom(
                      backgroundColor: Colors.green.shade700,
                    ),
                    onPressed: _isUpdating
                        ? null
                        : () => _updateRequestStatus(request, 'COMPLETED'),
                    icon: const Icon(Icons.check_rounded),
                    label: const Text('Hoàn tất'),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyState(IconData icon, String title, String subtitle) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 68, color: Colors.grey.shade400),
            const SizedBox(height: 12),
            Text(
              title,
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 17),
            ),
            const SizedBox(height: 5),
            Text(
              subtitle,
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey.shade600),
            ),
          ],
        ),
      ),
    );
  }
}

class _ServiceRequest {
  final String groupId;
  final String bookingId;
  final String invoiceId;
  final String roomName;
  final String guestName;
  final String status;
  final DateTime? orderedAt;
  final List<Map<String, dynamic>> items;

  const _ServiceRequest({
    required this.groupId,
    required this.bookingId,
    required this.invoiceId,
    required this.roomName,
    required this.guestName,
    required this.status,
    required this.orderedAt,
    required this.items,
  });

  factory _ServiceRequest.fromTickets(
    String groupId,
    List<Map<String, dynamic>> tickets,
  ) {
    final first = tickets.first;
    final statuses = tickets
        .map((item) => (item['status'] ?? 'PENDING').toString().toUpperCase())
        .toSet();
    final status = statuses.every((value) => value == 'COMPLETED')
        ? 'COMPLETED'
        : statuses.any((value) => value == 'IN_PROGRESS')
        ? 'IN_PROGRESS'
        : 'PENDING';
    return _ServiceRequest(
      groupId: groupId,
      bookingId: first['bookingId']?.toString() ?? '',
      invoiceId: first['invoiceId']?.toString() ?? '',
      roomName: first['roomName']?.toString() ?? '',
      guestName: first['guestName']?.toString() ?? '',
      status: status,
      orderedAt: DateTime.tryParse(first['orderedAt']?.toString() ?? ''),
      items: tickets,
    );
  }

  double get serviceTotal => items.fold<double>(0, (total, item) {
    final value = item['lineTotal'];
    return total +
        (value is num ? value.toDouble() : double.tryParse('$value') ?? 0);
  });

  int get sortValue {
    if (orderedAt != null) return orderedAt!.millisecondsSinceEpoch;
    return items
        .map((item) => int.tryParse(item['id']?.toString() ?? '') ?? 0)
        .fold<int>(0, (largest, id) => id > largest ? id : largest);
  }

  String get displayTime {
    final value = orderedAt;
    if (value == null) return 'Phiếu $groupId';
    final local = value.toLocal();
    String two(int number) => number.toString().padLeft(2, '0');
    return '${two(local.hour)}:${two(local.minute)} · '
        '${two(local.day)}/${two(local.month)}/${local.year}';
  }
}
