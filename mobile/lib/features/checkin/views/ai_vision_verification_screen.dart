import 'package:flutter/material.dart';

class AiVisionVerificationScreen extends StatefulWidget {
  final int totalGuests;
  const AiVisionVerificationScreen({Key? key, required this.totalGuests}) : super(key: key);

  @override
  State<AiVisionVerificationScreen> createState() => _AiVisionVerificationScreenState();
}

class _AiVisionVerificationScreenState extends State<AiVisionVerificationScreen> {
  bool _isProcessing = false;
  String _rawAiResult = "";

  void _simulateAiVision() async {
    setState(() {
      _isProcessing = true;
      _rawAiResult = "🔄 Đang quét ảnh và nhận diện bằng AI Vision...";
    });

    await Future.delayed(const Duration(seconds: 3));

    setState(() {
      _isProcessing = false;
      _rawAiResult = """
🟢 KẾT QUẢ AI VISION (DỮ LIỆU THÔ):
{
  "ho_ten": "NGUYỄN VĂN A",
  "so_cccd": "012345678901",
  "ngay_sinh": "01/01/1990",
  "gioi_tinh": "Nam",
  "que_quan": "Hà Nội",
  "dia_chi_thuong_tru": "Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội",
  "ngay_cap": "01/01/2021",
  "noi_cap": "Cục Cảnh sát QLHC về trật tự xã hội"
}
      """;
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      appBar: AppBar(
        title: const Text("AI Vision Verification", style: TextStyle(fontWeight: FontWeight.bold)),
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
            
            _buildCaptureBox("Mặt trước CCCD", Icons.add_a_photo_outlined),
            const SizedBox(height: 15),
            _buildCaptureBox("Mặt sau CCCD", Icons.add_a_photo_outlined),
            
            const SizedBox(height: 30),
            
            ElevatedButton.icon(
              onPressed: _isProcessing ? null : _simulateAiVision,
              icon: const Icon(Icons.auto_awesome_rounded),
              label: const Text("Bắt đầu nhận diện AI"),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.purple,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 15),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
              ),
            ),
            
            if (_rawAiResult.isNotEmpty) ...[
              const SizedBox(height: 30),
              const Text("Dữ liệu thô từ AI:", style: TextStyle(fontWeight: FontWeight.bold)),
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
              onPressed: _rawAiResult.contains("🟢") ? () {
                Navigator.pop(context, {
                  'status': 'success',
                  'method': 'AI_VISION',
                  'rawData': _rawAiResult
                });
              } : null,
              style: ElevatedButton.styleFrom(
                backgroundColor: colorScheme.primary,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 15),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
              ),
              child: const Text("Hoàn tất xác minh", style: TextStyle(fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCaptureBox(String label, IconData icon) {
    return Container(
      height: 180,
      decoration: BoxDecoration(
        color: Colors.grey[200],
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.grey[400]!, style: BorderStyle.solid),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, size: 50, color: Colors.grey[600]),
          const SizedBox(height: 10),
          Text(label, style: TextStyle(color: Colors.grey[600], fontWeight: FontWeight.w500)),
        ],
      ),
    );
  }
}
