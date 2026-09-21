import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:may_hotel_app/services/nfc_service.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';

class CccdCheckinScreen extends StatefulWidget {
  const CccdCheckinScreen({Key? key}) : super(key: key);

  @override
  State<CccdCheckinScreen> createState() => _CccdCheckinScreenState();
}

class _CccdCheckinScreenState extends State<CccdCheckinScreen> {
  final HotelApiProvider _apiProvider = HotelApiProvider();
  final ImagePicker _picker = ImagePicker();

  String _nfcData = "";
  File? _selfieImage;
  File? _nfcFaceImageMock; // Giả lập ảnh từ Chip NFC để test
  
  bool _isLoading = false;
  String _statusMessage = "Vui lòng chạm thẻ CCCD vào mặt lưng điện thoại.";

  // Bước 1: Quét NFC lấy dữ liệu văn bản
  void _scanNfc() async {
    setState(() {
      _isLoading = true;
      _statusMessage = "Đang chờ thẻ NFC...";
    });

    await NfcService.startCccdScanning(
      onSuccess: (text) {
        setState(() {
          _nfcData = text;
          _isLoading = false;
          _statusMessage = "Quét NFC thành công!\nTiếp theo: Quét khuôn mặt.";
        });
      },
      onError: (error) {
        setState(() {
          _isLoading = false;
          _statusMessage = "Lỗi NFC: $error";
        });
      },
    );
  }

  // Bước 2: Chụp ảnh Selfie
  Future<void> _takeSelfie() async {
    final XFile? photo = await _picker.pickImage(
      source: ImageSource.camera,
      preferredCameraDevice: CameraDevice.front,
    );
    if (photo != null) {
      setState(() {
        _selfieImage = File(photo.path);
        _statusMessage = "Đã chụp Selfie. Cần thêm ảnh CCCD để mô phỏng NFC.";
      });
    }
  }

  // Bước 3: (Giả lập) Chụp mặt trước CCCD làm ảnh NFC Face
  Future<void> _takeNfcFaceMock() async {
    final XFile? photo = await _picker.pickImage(
      source: ImageSource.camera,
    );
    if (photo != null) {
      setState(() {
        _nfcFaceImageMock = File(photo.path);
        _statusMessage = "Đã đủ dữ liệu. Sẵn sàng Xác thực AI.";
      });
    }
  }

  // Bước 4: Gọi AI Server xác thực
  Future<void> _verifyAi() async {
    if (_selfieImage == null || _nfcFaceImageMock == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vui lòng chụp đủ Selfie và thẻ!')),
      );
      return;
    }

    setState(() {
      _isLoading = true;
      _statusMessage = "AI đang xử lý so khớp sinh trắc học...";
    });

    final result = await _apiProvider.verifyFaceNFC(_nfcFaceImageMock!, _selfieImage!);

    setState(() {
      _isLoading = false;
    });

    if (result != null && result['status'] == 'success') {
      bool isMatch = result['is_match'];
      double similarity = result['similarity']?.toDouble() ?? 0.0;
      
      setState(() {
        if (isMatch) {
          _statusMessage = "✅ XÁC THỰC THÀNH CÔNG\nĐộ chính xác: $similarity%\nDữ liệu CCCD: $_nfcData";
        } else {
          _statusMessage = "❌ XÁC THỰC THẤT BẠI\nKhuôn mặt không khớp! ($similarity%)";
        }
      });
    } else {
      setState(() {
        _statusMessage = "Lỗi từ AI Server: ${result?['message'] ?? 'Không rõ lỗi'}";
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Xác thực CCCD Check-in"),
        backgroundColor: Colors.blueAccent,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Trạng thái hệ thống
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.blue.withOpacity(0.1),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.blueAccent),
              ),
              child: Text(
                _statusMessage,
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                textAlign: TextAlign.center,
              ),
            ),
            const SizedBox(height: 20),

            if (_isLoading)
              const Center(child: CircularProgressIndicator()),

            const SizedBox(height: 20),

            // Nút chức năng
            ElevatedButton.icon(
              icon: const Icon(Icons.nfc),
              label: const Text("1. Quét thẻ CCCD (NFC)"),
              style: ElevatedButton.styleFrom(padding: const EdgeInsets.all(16)),
              onPressed: _isLoading ? null : _scanNfc,
            ),
            const SizedBox(height: 12),
            
            ElevatedButton.icon(
              icon: const Icon(Icons.face),
              label: const Text("2. Quét khuôn mặt (Selfie)"),
              style: ElevatedButton.styleFrom(padding: const EdgeInsets.all(16)),
              onPressed: _isLoading ? null : _takeSelfie,
            ),
            const SizedBox(height: 12),

            ElevatedButton.icon(
              icon: const Icon(Icons.credit_card),
              label: const Text("3. Chụp CCCD (Mô phỏng ảnh NFC)"),
              style: ElevatedButton.styleFrom(padding: const EdgeInsets.all(16)),
              onPressed: _isLoading ? null : _takeNfcFaceMock,
            ),
            const SizedBox(height: 24),

            ElevatedButton(
              onPressed: (_selfieImage != null && _nfcFaceImageMock != null && !_isLoading) 
                  ? _verifyAi 
                  : null,
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.all(16),
                backgroundColor: Colors.green,
                foregroundColor: Colors.white,
              ),
              child: const Text("XÁC NHẬN & HOÀN TẤT", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            ),
            
            const SizedBox(height: 20),
            
            // Hiển thị ảnh Review
            Row(
              children: [
                Expanded(
                  child: _selfieImage != null
                      ? Image.file(_selfieImage!, height: 150, fit: BoxFit.cover)
                      : Container(height: 150, color: Colors.grey[200], child: const Center(child: Text("Chưa có Selfie"))),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: _nfcFaceImageMock != null
                      ? Image.file(_nfcFaceImageMock!, height: 150, fit: BoxFit.cover)
                      : Container(height: 150, color: Colors.grey[200], child: const Center(child: Text("Chưa có thẻ"))),
                ),
              ],
            )
          ],
        ),
      ),
    );
  }
}
