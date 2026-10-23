import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';
import 'package:may_hotel_app/services/nfc_service.dart';

class MobileSelfCheckinScreen extends StatefulWidget {
  final int bookingId;
  final String roomName;
  final int totalGuests;

  const MobileSelfCheckinScreen({
    Key? key,
    required this.bookingId,
    required this.roomName,
    this.totalGuests = 1,
  }) : super(key: key);

  @override
  State<MobileSelfCheckinScreen> createState() =>
      _MobileSelfCheckinScreenState();
}

class _MobileSelfCheckinScreenState extends State<MobileSelfCheckinScreen> {
  final ImagePicker _picker = ImagePicker();
  File? _cccdFront;
  File? _selfie;
  bool _isProcessing = false;
  String _statusMessage = "1. Vui lòng chụp Khuôn mặt (Selfie) trước.";
  String? _nfcData; // Dữ liệu đọc từ chip NFC
  bool _isNfcScanned = false;
  int _currentGuestIndex = 0;
  int _verifiedGuestCount = 0;
  late int _totalGuests;
  bool _allGuestsVerified = false;

  @override
  void initState() {
    super.initState();
    _totalGuests = widget.totalGuests.clamp(1, 20).toInt();
    _loadVerificationStatus();
  }

  Future<void> _loadVerificationStatus() async {
    if (widget.bookingId <= 0) return;
    final status = await HotelApiProvider().getCheckInVerificationStatus(
      widget.bookingId,
    );
    if (!mounted || status == null) return;

    final required =
        int.tryParse(status['requiredGuestCount']?.toString() ?? '') ??
        _totalGuests;
    final verified =
        int.tryParse(status['verifiedGuestCount']?.toString() ?? '') ?? 0;
    final total = required.clamp(1, 20).toInt();
    final completed = status['isFaceVerified'] == true || verified >= total;

    setState(() {
      _totalGuests = total;
      _verifiedGuestCount = verified.clamp(0, total).toInt();
      _allGuestsVerified = completed;
      _currentGuestIndex = completed
          ? total - 1
          : _verifiedGuestCount.clamp(0, total - 1).toInt();
      _statusMessage = completed
          ? "Đã xác thực đủ $_verifiedGuestCount/$_totalGuests khách. Vui lòng chờ lễ tân gán phòng..."
          : _verifiedGuestCount > 0
          ? "Đã xác thực $_verifiedGuestCount/$_totalGuests khách. Tiếp tục xác thực khách ${_currentGuestIndex + 1}."
          : "1. Vui lòng chụp Khuôn mặt (Selfie) trước.";
    });
  }

  Future<void> _takePicture(bool isCccd) async {
    try {
      final XFile? photo = await _picker.pickImage(
        source: ImageSource.camera,
        preferredCameraDevice: isCccd ? CameraDevice.rear : CameraDevice.front,
        imageQuality: isCccd ? 78 : 72,
        maxWidth: isCccd ? 1600 : 1280,
        maxHeight: isCccd ? 1200 : 1280,
      );

      if (photo != null) {
        setState(() {
          if (isCccd) {
            _cccdFront = File(photo.path);
            _statusMessage = "Đã đủ dữ liệu! Bấm xác thực AI bên dưới.";
          } else {
            _selfie = File(photo.path);
            _statusMessage = "2. Tiếp theo, vui lòng quét thẻ NFC CCCD.";
          }
        });
      }
    } catch (e) {
      debugPrint("Lỗi mở camera: $e");
    }
  }

  Future<void> _scanNfcCard() async {
    setState(() {
      _statusMessage = "Vui lòng áp thẻ CCCD vào mặt lưng điện thoại...";
    });
    await NfcService.startCccdScanning(
      onSuccess: (data) {
        setState(() {
          _nfcData = data;
          _isNfcScanned = true;
          _statusMessage =
              "3. Đọc NFC thành công! Cuối cùng, chụp Mặt trước CCCD.";
        });
      },
      onError: (err) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(err)));
        setState(() {
          _statusMessage = "Đọc NFC thất bại. Hãy thử lại.";
        });
      },
    );
  }

  Future<void> _submitAI() async {
    if (_selfie == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Vui lòng chụp ảnh khuôn mặt trước!")),
      );
      return;
    }
    if (!_isNfcScanned) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Vui lòng quét chip NFC trên thẻ CCCD!")),
      );
      return;
    }
    if (_cccdFront == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Vui lòng chụp ảnh mặt trước CCCD!")),
      );
      return;
    }

    setState(() {
      _isProcessing = true;
      _statusMessage =
          "Đang gửi ảnh lên AI Server (YOLOv8, VietOCR & FaceMatch)...";
    });

    try {
      // 1. Gọi Python AI Server trích xuất OCR và FaceMatch
      final aiRes = await HotelApiProvider().verifyCccdWithSelfie(
        _cccdFront!,
        _selfie!,
      );
      if (aiRes == null || aiRes['status'] != 'success') {
        setState(() {
          _isProcessing = false;
          _statusMessage =
              "AI không xác thực được danh tính. Vui lòng thử lại.";
        });
        return;
      }

      final faceVer = aiRes['face_verification'];
      if (faceVer is! Map) {
        setState(() {
          _isProcessing = false;
          _statusMessage =
              "AI chưa trả về kết quả so khớp khuôn mặt. Vui lòng thử lại.";
        });
        return;
      }

      final faceMatched =
          faceVer['verified'] == true ||
          faceVer['matched'] == true ||
          faceVer['is_match'] == true;
      final rawSimilarity = faceVer['similarity'];
      final rawDistance = faceVer['distance'];
      final similarity = rawSimilarity is num
          ? rawSimilarity.toDouble()
          : rawDistance is num
          ? ((1 - rawDistance.toDouble()) * 100).clamp(0, 100).toDouble()
          : 0.0;
      if (!faceMatched || similarity < 60) {
        setState(() {
          _isProcessing = false;
          _statusMessage =
              "Khuôn mặt không khớp CCCD (${similarity.toStringAsFixed(1)}%). Vui lòng chụp lại.";
        });
        return;
      }
      setState(() {
        _statusMessage = "AI xác thực thành công! Đang đồng bộ với lễ tân...";
      });

      // 2. Ghi nhận xác thực; lễ tân vẫn là nơi gán phòng và hoàn tất check-in.
      final res = await HotelApiProvider().submitCheckInAI(
        widget.bookingId,
        _nfcData!.trim(),
        _currentGuestIndex,
        faceMatched: faceMatched,
        faceSimilarity: similarity,
      );

      if (res != null) {
        final allGuestsVerified = res['allGuestsVerified'] == true;
        final requiredCount =
            int.tryParse(res['requiredGuestCount']?.toString() ?? '') ??
            _totalGuests;
        final verifiedCount =
            int.tryParse(res['verifiedGuestCount']?.toString() ?? '') ??
            (_currentGuestIndex + 1);
        setState(() {
          _isProcessing = false;
          _totalGuests = requiredCount.clamp(1, 20).toInt();
          _verifiedGuestCount = verifiedCount;
          _allGuestsVerified = allGuestsVerified;
          if (allGuestsVerified) {
            _statusMessage =
                "Đã xác thực đủ $_verifiedGuestCount/$_totalGuests khách. Vui lòng chờ lễ tân gán phòng...";
          } else {
            _currentGuestIndex = verifiedCount;
            _cccdFront = null;
            _selfie = null;
            _nfcData = null;
            _isNfcScanned = false;
            _statusMessage =
                "Đã xác thực $verifiedCount/$_totalGuests khách. Tiếp tục chụp khuôn mặt cho khách ${_currentGuestIndex + 1}.";
          }
        });
      } else {
        setState(() {
          _isProcessing = false;
          _statusMessage = "Lỗi cập nhật máy chủ. Vui lòng thử lại sau.";
        });
      }
    } catch (e) {
      debugPrint("Lỗi xác thực AI: $e");
      setState(() {
        _isProcessing = false;
        _statusMessage =
            "Không thể xác thực. Vui lòng kiểm tra lại ảnh và thử lại.";
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(title: const Text("Tự Nhận Phòng (AI)")),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              "Phòng: ${widget.roomName}",
              style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 6),
            Text(
              "Khách ${_allGuestsVerified ? _totalGuests : _currentGuestIndex + 1}/$_totalGuests • Đã xác thực $_verifiedGuestCount/$_totalGuests",
              style: TextStyle(
                color: colorScheme.primary,
                fontWeight: FontWeight.w600,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 10),
            Container(
              padding: const EdgeInsets.all(15),
              decoration: BoxDecoration(
                color: _allGuestsVerified
                    ? Colors.green.withOpacity(0.1)
                    : Colors.blue.withOpacity(0.1),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(
                  color: _allGuestsVerified ? Colors.green : Colors.blue,
                ),
              ),
              child: Text(
                _statusMessage,
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: _allGuestsVerified
                      ? Colors.green[700]
                      : Colors.blue[700],
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            const SizedBox(height: 30),

            if (_allGuestsVerified) ...[
              const Icon(Icons.verified_user, size: 90, color: Colors.green),
              const SizedBox(height: 16),
              const Text(
                "Tất cả khách đã hoàn tất xác thực. Lễ tân sẽ tiếp tục gán phòng trên hệ thống web.",
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
              ),
              const SizedBox(height: 24),
              FilledButton.icon(
                onPressed: () => Navigator.pop(context, true),
                icon: const Icon(Icons.done_all),
                label: const Text('Hoàn tất'),
              ),
            ] else ...[
              // BƯỚC 1: SELFIE
              _buildPhotoBox(
                label: "Bước 1: Ảnh Selfie Khuôn Mặt",
                icon: Icons.face,
                file: _selfie,
                onTap: () => _takePicture(false),
                colorScheme: colorScheme,
              ),
              const SizedBox(height: 20),

              // BƯỚC 2: NFC (Chỉ khả dụng nếu đã có Selfie)
              ElevatedButton.icon(
                onPressed: _selfie != null
                    ? _scanNfcCard
                    : () => ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text(
                            "Vui lòng hoàn thành Bước 1 (Selfie) trước!",
                          ),
                        ),
                      ),
                icon: const Icon(Icons.nfc),
                label: Text(
                  _isNfcScanned
                      ? "Bước 2: Đã đọc NFC"
                      : "Bước 2: Quét chip NFC CCCD",
                ),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _isNfcScanned
                      ? Colors.green
                      : (_selfie != null ? Colors.amber : Colors.grey),
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 12),
                ),
              ),
              const SizedBox(height: 20),

              // BƯỚC 3: CCCD (Chỉ khả dụng nếu đã quét NFC)
              _buildPhotoBox(
                label: "Bước 3: Ảnh Mặt trước CCCD",
                icon: Icons.credit_card,
                file: _cccdFront,
                onTap: _isNfcScanned
                    ? () => _takePicture(true)
                    : () => ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text(
                            "Vui lòng hoàn thành Bước 2 (Quét NFC) trước!",
                          ),
                        ),
                      ),
                colorScheme: colorScheme,
                isDisabled: !_isNfcScanned,
              ),

              const SizedBox(height: 40),
              ElevatedButton(
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 15),
                  backgroundColor: colorScheme.primary,
                  foregroundColor: colorScheme.onPrimary,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(10),
                  ),
                ),
                onPressed: _isProcessing ? null : _submitAI,
                child: _isProcessing
                    ? const CircularProgressIndicator(color: Colors.white)
                    : const Text(
                        "Tiến Hành Xác Thực AI",
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildPhotoBox({
    required String label,
    required IconData icon,
    required File? file,
    required VoidCallback onTap,
    required ColorScheme colorScheme,
    bool isDisabled = false,
  }) {
    return GestureDetector(
      onTap: isDisabled
          ? () => ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text("Vui lòng hoàn thành bước trước!")),
            )
          : onTap,
      child: Column(
        children: [
          Container(
            height: 120,
            width: double.infinity,
            decoration: BoxDecoration(
              color: isDisabled ? Colors.grey[300] : colorScheme.surfaceVariant,
              borderRadius: BorderRadius.circular(15),
              border: Border.all(
                color: isDisabled ? Colors.grey : colorScheme.outline,
                width: 1,
              ),
              image: file != null
                  ? DecorationImage(image: FileImage(file), fit: BoxFit.cover)
                  : null,
            ),
            child: file == null
                ? Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        icon,
                        size: 40,
                        color: isDisabled
                            ? Colors.grey
                            : colorScheme.primary.withOpacity(0.6),
                      ),
                      const SizedBox(height: 5),
                      Text(
                        "Bấm chụp",
                        style: TextStyle(
                          fontSize: 12,
                          color: isDisabled ? Colors.grey : Colors.grey,
                        ),
                      ),
                    ],
                  )
                : Container(
                    decoration: BoxDecoration(
                      color: Colors.black.withOpacity(0.3),
                      borderRadius: BorderRadius.circular(15),
                    ),
                    child: const Icon(
                      Icons.check_circle,
                      color: Colors.greenAccent,
                      size: 40,
                    ),
                  ),
          ),
          const SizedBox(height: 8),
          Text(
            label,
            style: TextStyle(
              fontWeight: FontWeight.bold,
              color: isDisabled ? Colors.grey : Colors.black,
            ),
          ),
        ],
      ),
    );
  }
}
