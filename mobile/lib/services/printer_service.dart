import 'dart:typed_data';
import 'package:blue_thermal_printer/blue_thermal_printer.dart';
import 'package:flutter/material.dart';

class PrinterService {
  final BlueThermalPrinter bluetooth = BlueThermalPrinter.instance;

  Future<List<BluetoothDevice>> getDevices() async {
    List<BluetoothDevice> devices = [];
    try {
      devices = await bluetooth.getBondedDevices();
    } catch (e) {
      debugPrint("Lỗi lấy danh sách thiết bị Bluetooth: $e");
    }
    return devices;
  }

  Future<bool> connect(BluetoothDevice device) async {
    try {
      bool? isConnected = await bluetooth.isConnected;
      if (isConnected == true) {
        return true;
      }
      return await bluetooth.connect(device) ?? false;
    } catch (e) {
      debugPrint("Lỗi kết nối máy in: $e");
      return false;
    }
  }

  Future<void> disconnect() async {
    try {
      await bluetooth.disconnect();
    } catch (e) {
      debugPrint("Lỗi ngắt kết nối: $e");
    }
  }

  // Hàm in Hóa đơn dạng văn bản thường (mất dấu) làm fallback
  String _formatRow(String col1, String col2, String col3) {
    int w1 = 12; 
    int w2 = 4;
    int w3 = 14; 
    
    String c1 = col1.padRight(w1);
    if (c1.length > w1) {
      c1 = c1.substring(0, w1 - 1) + " ";
    }
    
    String c2 = col2.padLeft(w2);
    String c3 = col3.padLeft(w3);
    
    return "$c1 $c2 $c3";
  }

  Future<void> printReceipt({
    required List<Map<String, dynamic>> services,
    required double totalAmount,
  }) async {
    bool? isConnected = await bluetooth.isConnected;
    if (isConnected != true) {
      debugPrint("Máy in chưa kết nối!");
      return;
    }

    try {
      bluetooth.printCustom("MAY HOTEL", 3, 1);
      bluetooth.printNewLine();
      
      bluetooth.printCustom("Dia chi: test, Tay Thanh, TP.HCM", 0, 1);
      bluetooth.printCustom("Hotline: 0123456789", 0, 1);
      bluetooth.printNewLine();
      
      bluetooth.printCustom("HOA DON", 2, 1);
      bluetooth.printNewLine();

      bluetooth.printCustom(_formatRow("Ten DV", "SL", "T.Tien"), 1, 0);
      bluetooth.printCustom("--------------------------------", 0, 1);

      for (var item in services) {
        String name = _removeDiacritics(item['name'] ?? "Dich vu");
        String qty = item['qty'].toString();
        String price = item['totalPrice'].toString();
        bluetooth.printCustom(_formatRow(name, qty, price), 0, 0);
      }

      bluetooth.printNewLine();
      bluetooth.printCustom("Thanh tien: ${totalAmount.toStringAsFixed(0)} VND", 1, 1);
      bluetooth.printNewLine();

      bluetooth.printCustom("Cam on quy khach da su dung dich vu!", 0, 1);
      bluetooth.printNewLine();
      bluetooth.printNewLine();
      bluetooth.paperCut();

    } catch (e) {
      debugPrint("Lỗi khi in hóa đơn: $e");
    }
  }
  
  // Hàm loại bỏ dấu Tiếng Việt
  String _removeDiacritics(String str) {
    const withDiacritics = 'àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ';
    const withoutDiacritics = 'aaaaaaaaaaaaaaaaaeeeeeeeeeeeiiiiiooooooooooooooooouuuuuuuuuuuyyyyydAAAAAAAAAAAAAAAAAEEEEEEEEEEEIIIIIOOOOOOOOOOOOOOOOOUUUUUUUUUUUYYYYYD';
    for (int i = 0; i < withDiacritics.length; i++) {
      str = str.replaceAll(withDiacritics[i], withoutDiacritics[i]);
    }
    return str;
  }

  // 🚀 IN ẢNH BIÊN LAI (Giữ nguyên dấu Tiếng Việt 100%)
  Future<void> printReceiptImage(Uint8List imageBytes) async {
    bool? isConnected = await bluetooth.isConnected;
    if (isConnected != true) {
      debugPrint("Máy in chưa kết nối!");
      return;
    }

    try {
      // In ảnh được chụp từ UI (giữ nguyên được 100% Font Tiếng Việt)
      bluetooth.printImageBytes(imageBytes);
      bluetooth.printNewLine();
      bluetooth.printNewLine();
      bluetooth.paperCut();
    } catch (e) {
      debugPrint("Lỗi khi in hóa đơn bằng ảnh: $e");
    }
  }
}
