import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:blue_thermal_printer/blue_thermal_printer.dart';
import 'package:may_hotel_app/services/printer_service.dart';
import 'package:may_hotel_app/core/constants/app_colors.dart';

class ReceiptPrinterScreen extends StatefulWidget {
  final List<Map<String, dynamic>> dummyServices = const [
    {'name': 'Giặt ủi', 'qty': 1, 'totalPrice': 100000},
    {'name': 'Nước suối', 'qty': 2, 'totalPrice': 20000},
  ];
  final double dummyTotalAmount = 120000;

  const ReceiptPrinterScreen({Key? key}) : super(key: key);

  @override
  State<ReceiptPrinterScreen> createState() => _ReceiptPrinterScreenState();
}

class _ReceiptPrinterScreenState extends State<ReceiptPrinterScreen> {
  final PrinterService _printerService = PrinterService();
  final GlobalKey _receiptKey = GlobalKey(); // Khóa để chụp ảnh RepaintBoundary
  
  List<BluetoothDevice> _devices = [];
  BluetoothDevice? _selectedDevice;
  bool _connected = false;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _loadDevices();
  }

  Future<void> _loadDevices() async {
    setState(() => _isLoading = true);
    final devices = await _printerService.getDevices();
    setState(() {
      _devices = devices;
      _isLoading = false;
    });
  }

  Future<void> _connect() async {
    if (_selectedDevice == null) return;
    setState(() => _isLoading = true);
    
    bool connected = await _printerService.connect(_selectedDevice!);
    
    setState(() {
      _connected = connected;
      _isLoading = false;
    });

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(connected ? "Kết nối máy in thành công!" : "Không thể kết nối máy in.")),
      );
    }
  }

  Future<void> _disconnect() async {
    await _printerService.disconnect();
    setState(() {
      _connected = false;
    });
  }

  // Chụp ảnh Widget Hóa đơn để In qua Bluetooth
  Future<void> _printImageReceipt() async {
    if (!_connected) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Vui lòng kết nối máy in trước khi in!")),
      );
      return;
    }

    try {
      setState(() => _isLoading = true);
      
      // Chờ Widget render hoàn chỉnh
      await Future.delayed(const Duration(milliseconds: 200));

      RenderRepaintBoundary boundary = _receiptKey.currentContext!.findRenderObject() as RenderRepaintBoundary;
      ui.Image image = await boundary.toImage(pixelRatio: 2.0); // Tăng pixelRatio để chữ nét hơn khi in
      ByteData? byteData = await image.toByteData(format: ui.ImageByteFormat.png);
      Uint8List pngBytes = byteData!.buffer.asUint8List();

      await _printerService.printReceiptImage(pngBytes);

      setState(() => _isLoading = false);
    } catch (e) {
      setState(() => _isLoading = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text("Lỗi chụp ảnh hóa đơn: $e")),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Máy in Hóa đơn (Bluetooth)"),
        backgroundColor: AppColors.primary,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                "Danh sách máy in đã ghép nối:",
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
              ),
              const SizedBox(height: 10),
              
              if (_isLoading)
                const Center(child: CircularProgressIndicator())
              else if (_devices.isEmpty)
                const Text("Không tìm thấy máy in nào. Vui lòng ghép nối Bluetooth trước.")
              else
                DropdownButton<BluetoothDevice>(
                  isExpanded: true,
                  value: _selectedDevice,
                  hint: const Text("Chọn máy in (Ví dụ: MPT-II, POS-58)"),
                  items: _devices.map((device) {
                    return DropdownMenuItem(
                      value: device,
                      child: Text(device.name ?? "Unknown Device"),
                    );
                  }).toList(),
                  onChanged: (device) {
                    setState(() {
                      _selectedDevice = device;
                    });
                  },
                ),
                
              const SizedBox(height: 20),
              
              Row(
                children: [
                  Expanded(
                    child: ElevatedButton(
                      onPressed: _connected ? null : _connect,
                      style: ElevatedButton.styleFrom(backgroundColor: Colors.green, foregroundColor: Colors.white),
                      child: const Text("KẾT NỐI"),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: _connected ? _disconnect : null,
                      style: ElevatedButton.styleFrom(backgroundColor: Colors.red, foregroundColor: Colors.white),
                      child: const Text("NGẮT KẾT NỐI"),
                    ),
                  ),
                ],
              ),
              
              const Divider(height: 30, thickness: 2),
              
              ElevatedButton.icon(
                icon: const Icon(Icons.print),
                label: const Text("IN HÓA ĐƠN BẰNG ẢNH (GIỮ DẤU)"),
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.all(16),
                  backgroundColor: AppColors.primary,
                  foregroundColor: Colors.white,
                ),
                onPressed: _printImageReceipt,
              ),
              
              const SizedBox(height: 20),
              const Center(child: Text("Bản xem trước Hóa đơn (Sẽ được chụp làm ảnh gửi máy in):", style: TextStyle(fontWeight: FontWeight.bold, color: Colors.grey))),
              const SizedBox(height: 10),

              // Giao diện Hóa Đơn 58mm thu nhỏ bọc trong RepaintBoundary
              Center(
                child: RepaintBoundary(
                  key: _receiptKey,
                  child: Container(
                    width: 380, // Chiều rộng tương đối cho tỷ lệ 58mm
                    padding: const EdgeInsets.all(16.0),
                    color: Colors.white, // Nền trắng bắt buộc để xuất file ảnh rõ nét
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.center,
                      mainAxisSize: MainAxisSize.min, // Ôm sát nội dung
                      children: [
                        const Text("MAY HOTEL", style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.black)),
                        const SizedBox(height: 4),
                        const Text("Địa chỉ: test, Tây Thạnh, TP.HCM", style: TextStyle(fontSize: 16, color: Colors.black)),
                        const Text("Hotline: 0123456789", style: TextStyle(fontSize: 16, color: Colors.black)),
                        const SizedBox(height: 12),
                        const Text("HÓA ĐƠN", style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.black)),
                        const SizedBox(height: 12),
                        const Divider(color: Colors.black, thickness: 1),
                        Row(
                          children: const [
                            Expanded(flex: 3, child: Text("Tên DV", style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.black))),
                            Expanded(flex: 1, child: Text("SL", style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.black))),
                            Expanded(flex: 2, child: Text("T.Tiền", textAlign: TextAlign.right, style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.black))),
                          ],
                        ),
                        const Divider(color: Colors.black, thickness: 1),
                        ...widget.dummyServices.map((item) {
                          return Padding(
                            padding: const EdgeInsets.symmetric(vertical: 4.0),
                            child: Row(
                              children: [
                                Expanded(flex: 3, child: Text(item['name'], style: const TextStyle(fontSize: 16, color: Colors.black))),
                                Expanded(flex: 1, child: Text(item['qty'].toString(), style: const TextStyle(fontSize: 16, color: Colors.black))),
                                Expanded(flex: 2, child: Text(item['totalPrice'].toString(), textAlign: TextAlign.right, style: const TextStyle(fontSize: 16, color: Colors.black))),
                              ],
                            ),
                          );
                        }).toList(),
                        const Divider(color: Colors.black, thickness: 1),
                        const SizedBox(height: 8),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            const Text("Thành tiền:", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.black)),
                            Text("${widget.dummyTotalAmount.toStringAsFixed(0)} VNĐ", style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.black)),
                          ],
                        ),
                        const SizedBox(height: 16),
                        const Text("Cảm ơn quý khách đã sử dụng dịch vụ!", style: TextStyle(fontSize: 16, fontStyle: FontStyle.italic, color: Colors.black)),
                        const SizedBox(height: 10), // Padding đáy cho an toàn khi in
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
