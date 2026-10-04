import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:screenshot/screenshot.dart';
import 'package:blue_thermal_printer/blue_thermal_printer.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:image/image.dart' as img;
import 'package:may_hotel_app/services/hotel_api_provider.dart';
import '../../../core/utils/responsive.dart';

class ServiceOrderScreen extends StatefulWidget {
  final int bookingId;
  const ServiceOrderScreen({Key? key, required this.bookingId}) : super(key: key);

  @override
  State<ServiceOrderScreen> createState() => _ServiceOrderScreenState();
}

class _ServiceOrderScreenState extends State<ServiceOrderScreen> {
  final HotelApiProvider _apiProvider = HotelApiProvider();
  final ScreenshotController _screenshotController = ScreenshotController();
  final BlueThermalPrinter _bluetooth = BlueThermalPrinter.instance;

  List<dynamic> _services = [];
  bool _isLoading = true;
  
  // Trạng thái Bluetooth
  List<BluetoothDevice> _devices = [];
  BluetoothDevice? _selectedDevice;
  bool _isConnected = false;

  @override
  void initState() {
    super.initState();
    _fetchServices();
    _initPrinter();
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

  Future<void> _initPrinter() async {
    await [Permission.bluetoothScan, Permission.bluetoothConnect, Permission.location].request();
    try {
      List<BluetoothDevice> devices = await _bluetooth.getBondedDevices();
      if (mounted) {
        setState(() {
          _devices = devices;
          try {
            _selectedDevice = _devices.firstWhere((d) => (d.name ?? '').toUpperCase().contains('RPP'));
          } catch (e) {
            // Không tìm thấy RPP mặc định
          }
        });
      }
    } catch (e) {
      debugPrint("Lỗi quét Bluetooth: $e");
    }
  }

  void _connectPrinter() async {
    if (_selectedDevice == null) return;
    bool? isConnected = await _bluetooth.isConnected;
    if (isConnected != true) {
      try {
        await _bluetooth.connect(_selectedDevice!);
        setState(() => _isConnected = true);
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Kết nối máy in thành công!'), backgroundColor: Colors.green));
      } catch (e) {
        setState(() => _isConnected = false);
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Lỗi kết nối máy in!'), backgroundColor: Colors.red));
      }
    }
  }

  void _disconnectPrinter() async {
    await _bluetooth.disconnect();
    setState(() => _isConnected = false);
  }

  // Luồng: Chọn Dịch vụ -> Điền SL -> Gọi API -> Nhận Mock Hóa đơn -> Chụp Screenshot -> Xem trước bản in -> In
  void _orderService(dynamic service) {
    int qty = 1;
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
                  onPressed: () { if (qty > 1) setDialogState(() => qty--); },
                ),
                Text("$qty", style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                IconButton(
                  icon: const Icon(Icons.add_circle_outline),
                  onPressed: () { setDialogState(() => qty++); },
                ),
              ],
            ),
            actions: [
              TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("Hủy")),
              ElevatedButton(
                onPressed: () async {
                  Navigator.pop(ctx);
                  _processOrder(service, qty);
                },
                child: const Text("Xác nhận Đặt"),
              ),
            ],
          );
        }
      ),
    );
  }

  Future<void> _processOrder(dynamic service, int quantity) async {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => const Center(child: CircularProgressIndicator()),
    );

    // 1. Gọi API lưu dịch vụ & nhận về thông tin hóa đơn
    final receiptData = await _apiProvider.orderMobileService(widget.bookingId, service['id'], quantity);
    
    if (!mounted) return;
    Navigator.pop(context); // Đóng loading

    if (receiptData == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Lỗi khi đặt dịch vụ!")));
      return;
    }

    // 2. Tạo UI Hóa Đơn 58mm (Không hiển thị ra màn hình, chỉ để chụp)
    Widget receiptWidget = _build58mmReceipt(receiptData);

    // 3. Chụp ảnh Hóa Đơn
    showDialog(context: context, barrierDismissible: false, builder: (_) => const Center(child: CircularProgressIndicator()));
    try {
      Uint8List imageBytes = await _screenshotController.captureFromWidget(
        receiptWidget,
        delay: const Duration(milliseconds: 100),
        pixelRatio: 1.0,
      );
      if (mounted) Navigator.pop(context); // Đóng chụp

      // 4. Mở cửa sổ Xem Trước Bản In (Kế thừa từ file máy in bạn gửi)
      _showPrintPreview(imageBytes);
    } catch (e) {
      if (mounted) Navigator.pop(context);
      debugPrint("Lỗi chụp hóa đơn: $e");
    }
  }

  // 📝 THIẾT KẾ GIAO DIỆN HÓA ĐƠN 58MM CỰC KỲ CHI TIẾT
  Widget _build58mmReceipt(Map<String, dynamic> data) {
    return Material(
      color: Colors.white,
      child: Container(
        width: 384, // Chuẩn độ phân giải bề ngang máy in 58mm (48mm = 384 dots)
        color: Colors.white,
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Text(data['hotelName'] ?? "MAY HOTEL", style: const TextStyle(color: Colors.black, fontSize: 24, fontWeight: FontWeight.bold)),
            const SizedBox(height: 5),
            Text(data['address'] ?? "Tây Thạnh, TP.HCM", style: const TextStyle(color: Colors.black, fontSize: 16)),
            Text("Hotline: ${data['hotline'] ?? '0123456789'}", style: const TextStyle(color: Colors.black, fontSize: 16)),
            const SizedBox(height: 10),
            const Text("--------------------------------", style: TextStyle(color: Colors.black, fontSize: 20)),
            const SizedBox(height: 10),
            Text(data['title'] ?? "HÓA ĐƠN DỊCH VỤ", style: const TextStyle(color: Colors.black, fontSize: 22, fontWeight: FontWeight.bold)),
            const SizedBox(height: 15),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: const [
                Text("Tên Dịch Vụ", style: TextStyle(color: Colors.black, fontSize: 16, fontWeight: FontWeight.bold)),
                Text("SL", style: TextStyle(color: Colors.black, fontSize: 16, fontWeight: FontWeight.bold)),
                Text("Thành Tiền", style: TextStyle(color: Colors.black, fontSize: 16, fontWeight: FontWeight.bold)),
              ],
            ),
            const SizedBox(height: 5),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(child: Text(data['serviceName'] ?? "", style: const TextStyle(color: Colors.black, fontSize: 18))),
                Text("${data['quantity']}", style: const TextStyle(color: Colors.black, fontSize: 18)),
                const SizedBox(width: 20),
                Text("${data['totalPrice']}", style: const TextStyle(color: Colors.black, fontSize: 18, fontWeight: FontWeight.bold)),
              ],
            ),
            const SizedBox(height: 15),
            const Text("--------------------------------", style: TextStyle(color: Colors.black, fontSize: 20)),
            const SizedBox(height: 10),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text("TỔNG CỘNG:", style: TextStyle(color: Colors.black, fontSize: 20, fontWeight: FontWeight.bold)),
                Text("${data['totalPrice']} VNĐ", style: const TextStyle(color: Colors.black, fontSize: 20, fontWeight: FontWeight.bold)),
              ],
            ),
            const SizedBox(height: 20),
            Text(data['message'] ?? "Xin cảm ơn quý khách!", style: const TextStyle(color: Colors.black, fontSize: 16, fontStyle: FontStyle.italic)),
            const SizedBox(height: 30),
          ],
        ),
      ),
    );
  }

  // 🖨️ XEM TRƯỚC VÀ IN
  void _showPrintPreview(Uint8List imageBytes) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Xem trước Hóa đơn', style: TextStyle(fontWeight: FontWeight.bold)),
        contentPadding: const EdgeInsets.all(16),
        content: SingleChildScrollView(
          child: Container(
            decoration: BoxDecoration(border: Border.all(color: Colors.grey.shade400)),
            child: Image.memory(imageBytes),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Đóng', style: TextStyle(color: Colors.grey))),
          ElevatedButton.icon(
            onPressed: () {
              Navigator.pop(ctx);
              _printImage(imageBytes);
            },
            icon: const Icon(Icons.print),
            label: const Text('IN NGAY'),
            style: ElevatedButton.styleFrom(backgroundColor: Colors.green, foregroundColor: Colors.white),
          ),
        ],
      ),
    );
  }

  void _printImage(Uint8List imageBytes) async {
    if (!_isConnected) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Vui lòng kết nối máy in Bluetooth trước!')));
      return;
    }
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Đang làm nét ảnh và in...')));

    img.Image? decodedImage = img.decodeImage(imageBytes);
    if (decodedImage != null) {
      // Xử lý đen trắng cho sắc nét máy in nhiệt
      for (var pixel in decodedImage) {
        num luminance = pixel.r * 0.299 + pixel.g * 0.587 + pixel.b * 0.114;
        if (luminance > 200) {
          pixel.setRgb(255, 255, 255);
        } else {
          pixel.setRgb(0, 0, 0);
        }
      }
      imageBytes = img.encodePng(decodedImage);
    }
    _bluetooth.printImageBytes(imageBytes);
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
      body: Column(
        children: [
          // KHỐI KẾT NỐI MÁY IN BLUETOOTH
          Container(
            color: colorScheme.surfaceVariant.withOpacity(0.3),
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              children: [
                Expanded(
                  child: DropdownButton<BluetoothDevice>(
                    isExpanded: true,
                    hint: const Text('Chọn máy in hóa đơn...'),
                    value: _selectedDevice,
                    items: _devices.map((e) => DropdownMenuItem(value: e, child: Text(e.name ?? 'Unknown'))).toList(),
                    onChanged: (device) => setState(() => _selectedDevice = device),
                  ),
                ),
                const SizedBox(width: 10),
                ElevatedButton(
                  onPressed: _isConnected ? _disconnectPrinter : _connectPrinter,
                  style: ElevatedButton.styleFrom(backgroundColor: _isConnected ? Colors.red : Colors.green),
                  child: Text(_isConnected ? 'Ngắt In' : 'Kết nối', style: const TextStyle(color: Colors.white)),
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
                    gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                      crossAxisCount: 2,
                      childAspectRatio: 0.85,
                      crossAxisSpacing: 15,
                      mainAxisSpacing: 15,
                    ),
                    itemCount: _services.length,
                    itemBuilder: (context, index) {
                      final srv = _services[index];
                      double price = double.tryParse(srv['price']?.toString() ?? '0') ?? 0.0;
                      
                      return Card(
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
                        elevation: 3,
                        child: InkWell(
                          borderRadius: BorderRadius.circular(15),
                          onTap: () => _orderService(srv),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              Expanded(
                                child: ClipRRect(
                                  borderRadius: const BorderRadius.vertical(top: Radius.circular(15)),
                                  child: Container(
                                    color: Colors.blue.withOpacity(0.1),
                                    child: const Icon(Icons.room_service, size: 50, color: Colors.blue),
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
                                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                    const SizedBox(height: 5),
                                    Text(
                                      "${price.toStringAsFixed(0)} VNĐ",
                                      style: TextStyle(color: colorScheme.primary, fontWeight: FontWeight.bold),
                                    ),
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
