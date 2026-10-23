import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';
import 'dart:io';

class AiVisionVerificationScreen extends StatefulWidget {
  final int totalGuests;
  const AiVisionVerificationScreen({Key? key, required this.totalGuests})
    : super(key: key);

  @override
  State<AiVisionVerificationScreen> createState() =>
      _AiVisionVerificationScreenState();
}

class _AiVisionVerificationScreenState
    extends State<AiVisionVerificationScreen> {
  bool _isProcessing = false;
  bool _verified = false;
  String _rawAiResult = "";

  File? _frontImage;
  File? _backImage;
  File? _selfieImage;

  final ImagePicker _picker = ImagePicker();

  // 📸 HÀM CHỤP ẢNH CHUNG (MẶT TRƯỚC, MẶT SAU, SELFIE)
  Future<void> _takePicture(String type) async {
    try {
      final source = ImageSource.camera;
      final cameraDevice = type == 'selfie'
          ? CameraDevice.front
          : CameraDevice.rear;

      final XFile? photo = await _picker.pickImage(
        source: source,
        preferredCameraDevice: cameraDevice,
        imageQuality: type == 'front' ? 78 : 72,
        maxWidth: type == 'front' ? 1600 : 1280,
        maxHeight: type == 'front' ? 1200 : 1280,
      );

      if (photo != null) {
        setState(() {
          if (type == 'front')
            _frontImage = File(photo.path);
          else if (type == 'back')
            _backImage = File(photo.path);
          else if (type == 'selfie')
            _selfieImage = File(photo.path);
        });
      }
    } catch (e) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text("Lỗi khởi động máy ảnh: $e")));
    }
  }

  Future<void> _callAiVisionApi() async {
    if (_frontImage == null || _selfieImage == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text("Vui lòng chụp mặt trước CCCD và ảnh khuôn mặt!"),
        ),
      );
      return;
    }

    setState(() {
      _isProcessing = true;
      _rawAiResult =
          "🔄 Đang upload ảnh lên Server AI Colab để bóc tách OCR và so khớp khuôn mặt...";
    });

    final result = await HotelApiProvider().verifyCccdWithSelfie(
      _frontImage!,
      _selfieImage!,
    );
    if (!mounted) return;
    final face = result?['face_verification'];
    final matched =
        face is Map &&
        (face['verified'] == true ||
            face['matched'] == true ||
            face['is_match'] == true);
    setState(() {
      _isProcessing = false;
      _verified = result?['status'] == 'success' && matched;
      _rawAiResult = _verified
          ? 'Xác thực AI thành công\n${result.toString()}'
          : 'Không thể xác thực: ${result?['message'] ?? 'khuôn mặt không khớp'}';
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      appBar: AppBar(
        title: const Text(
          "AI Vision Verification",
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        backgroundColor: colorScheme.surface,
        foregroundColor: colorScheme.onSurface,
        elevation: 0.5,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              "Xác thực bằng AI Vision (Chụp ảnh CCCD)",
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 30),

            _buildCaptureBox(
              label: "Mặt trước CCCD",
              icon: Icons.camera_front,
              imageFile: _frontImage,
              onTap: () => _takePicture('front'),
            ),
            const SizedBox(height: 15),
            _buildCaptureBox(
              label: "Mặt sau CCCD",
              icon: Icons.camera_rear,
              imageFile: _backImage,
              onTap: () => _takePicture('back'),
            ),
            const SizedBox(height: 15),
            _buildCaptureBox(
              label: "Chụp ảnh khuôn mặt (Xác thực Liveness/Face Match)",
              icon: Icons.face_retouching_natural,
              imageFile: _selfieImage,
              onTap: () => _takePicture('selfie'),
            ),

            const SizedBox(height: 30),

            ElevatedButton.icon(
              onPressed: _isProcessing ? null : _callAiVisionApi,
              icon: const Icon(Icons.auto_awesome_rounded),
              label: const Text("Bắt đầu nhận diện AI"),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.purple,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 15),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(15),
                ),
              ),
            ),

            if (_rawAiResult.isNotEmpty) ...[
              const SizedBox(height: 30),
              const Text(
                "Dữ liệu thô từ AI:",
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 10),
              Container(
                padding: const EdgeInsets.all(15),
                decoration: BoxDecoration(
                  color: Colors.black.withOpacity(0.05),
                  borderRadius: BorderRadius.circular(15),
                  border: Border.all(color: Colors.grey.withOpacity(0.3)),
                ),
                child: Text(
                  _rawAiResult,
                  style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
                ),
              ),
            ],

            const SizedBox(height: 40),

            ElevatedButton(
              onPressed: _verified
                  ? () {
                      Navigator.pop(context, {
                        'status': 'success',
                        'method': 'AI_VISION',
                        'rawData': _rawAiResult,
                      });
                    }
                  : null,
              style: ElevatedButton.styleFrom(
                backgroundColor: colorScheme.primary,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 15),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(15),
                ),
              ),
              child: const Text(
                "Hoàn tất xác minh",
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCaptureBox({
    required String label,
    required IconData icon,
    required File? imageFile,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 180,
        decoration: BoxDecoration(
          color: Colors.grey[200],
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: Colors.grey[400]!,
            style: BorderStyle.solid,
          ),
          image: imageFile != null
              ? DecorationImage(image: FileImage(imageFile), fit: BoxFit.cover)
              : null,
        ),
        child: imageFile == null
            ? Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(icon, size: 50, color: Colors.grey[600]),
                  const SizedBox(height: 10),
                  Text(
                    label,
                    style: TextStyle(
                      color: Colors.grey[600],
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
              )
            : Container(
                decoration: BoxDecoration(
                  color: Colors.black.withOpacity(0.3),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: const Center(
                  child: Icon(
                    Icons.check_circle,
                    color: Colors.greenAccent,
                    size: 50,
                  ),
                ),
              ),
      ),
    );
  }
}
