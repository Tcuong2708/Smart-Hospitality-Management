import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';
import 'package:may_hotel_app/services/mrz_ocr_service.dart';
import 'package:may_hotel_app/services/nfc_service.dart';
import 'package:path_provider/path_provider.dart';

class MobileSelfCheckinScreen extends StatefulWidget {
  const MobileSelfCheckinScreen({
    super.key,
    required this.bookingId,
    required this.roomName,
    this.totalGuests = 1,
  });

  final int bookingId;
  final String roomName;
  final int totalGuests;

  @override
  State<MobileSelfCheckinScreen> createState() =>
      _MobileSelfCheckinScreenState();
}

class _MobileSelfCheckinScreenState extends State<MobileSelfCheckinScreen> {
  final ImagePicker _picker = ImagePicker();
  final HotelApiProvider _api = HotelApiProvider();
  File? _documentFront;
  File? _selfie;
  File? _chipFaceFile;
  NfcIdentityResult? _identityResult;
  bool _isProcessing = false;
  int _currentGuestIndex = 0;
  int _verifiedGuestCount = 0;
  late int _totalGuests;
  bool _allGuestsVerified = false;
  String _statusMessage = 'Bước 1: Chụp ảnh selfie khuôn mặt.';

  @override
  void initState() {
    super.initState();
    _totalGuests = widget.totalGuests.clamp(1, 20).toInt();
    _loadVerificationStatus();
  }

  @override
  void dispose() {
    _deleteChipFace();
    super.dispose();
  }

  Future<void> _loadVerificationStatus() async {
    if (widget.bookingId <= 0) return;
    final status = await _api.getCheckInVerificationStatus(widget.bookingId);
    if (!mounted || status == null) return;
    final required =
        int.tryParse('${status['requiredGuestCount'] ?? ''}') ?? _totalGuests;
    final verified = int.tryParse('${status['verifiedGuestCount'] ?? ''}') ?? 0;
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
          ? 'Đã xác thực đủ $_verifiedGuestCount/$_totalGuests khách. Chờ lễ tân gán phòng.'
          : _verifiedGuestCount > 0
          ? 'Tiếp tục xác thực khách ${_currentGuestIndex + 1}/$_totalGuests.'
          : 'Bước 1: Chụp ảnh selfie khuôn mặt.';
    });
  }

  Future<void> _takePicture({required bool document}) async {
    try {
      final photo = await _picker.pickImage(
        source: ImageSource.camera,
        preferredCameraDevice: document
            ? CameraDevice.rear
            : CameraDevice.front,
        imageQuality: document ? 82 : 75,
        maxWidth: document ? 1600 : 1280,
        maxHeight: document ? 1200 : 1280,
      );
      if (!mounted || photo == null) return;
      setState(() {
        if (document) {
          _documentFront = File(photo.path);
          _statusMessage = 'Đã đủ dữ liệu. Bấm xác thực danh tính.';
        } else {
          _selfie = File(photo.path);
          _statusMessage = 'Bước 2: Chọn phương thức NFC hoặc AI.';
        }
      });
    } catch (error) {
      _showMessage('Không thể mở camera: $error');
    }
  }

  Future<void> _chooseIdentityMethod() async {
    if (_selfie == null) {
      _showMessage('Vui lòng chụp selfie trước.');
      return;
    }
    final method = await showModalBottomSheet<IdentityNfcMethod>(
      context: context,
      showDragHandle: true,
      builder: (context) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Chọn phương thức xác thực',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              const Text(
                'Đọc chip thật là tính năng nghiên cứu BAC/PACE, không kết nối C06 và không thay thế định danh điện tử chính thức.',
              ),
              const SizedBox(height: 16),
              ListTile(
                leading: const Icon(Icons.nfc),
                title: const Text('Thử đọc chip CCCD thật'),
                subtitle: const Text(
                  'Quét MRZ mặt sau rồi áp thẻ vào điện thoại',
                ),
                onTap: () => Navigator.pop(context, IdentityNfcMethod.realNfc),
              ),
              ListTile(
                leading: const Icon(Icons.developer_board),
                title: const Text('NFC mô phỏng có ký số'),
                subtitle: const Text('Dùng thẻ NDEF MAY HOTEL trong buổi demo'),
                onTap: () =>
                    Navigator.pop(context, IdentityNfcMethod.signedNdef),
              ),
              ListTile(
                leading: const Icon(Icons.face_retouching_natural),
                title: const Text('Xác thực AI không NFC'),
                subtitle: const Text('Chụp mặt trước giấy tờ và selfie'),
                onTap: () => Navigator.pop(context, IdentityNfcMethod.aiOnly),
              ),
            ],
          ),
        ),
      ),
    );
    if (method == null) return;
    switch (method) {
      case IdentityNfcMethod.realNfc:
        await _scanRealCccd();
      case IdentityNfcMethod.signedNdef:
        await _scanDemoNdef();
      case IdentityNfcMethod.aiOnly:
        await _selectAiOnly();
    }
  }

  Future<void> _scanRealCccd() async {
    setState(() {
      _isProcessing = true;
      _statusMessage = 'Chụp mặt sau CCCD để nhận dạng MRZ...';
    });
    File? backImage;
    try {
      final photo = await _picker.pickImage(
        source: ImageSource.camera,
        preferredCameraDevice: CameraDevice.rear,
        imageQuality: 90,
        maxWidth: 2000,
      );
      if (photo == null) return;
      backImage = File(photo.path);
      final detected = await MrzOcrService.extract(backImage);
      if (!mounted) return;
      final access = await _requestAccessData(detected);
      if (access == null) return;
      setState(() {
        _statusMessage = 'Giữ CCCD ổn định tại vị trí NFC để đọc DG1/DG2...';
      });
      final result = await NfcService.readRealCccd(access);
      final directory = await getTemporaryDirectory();
      final face = File(
        '${directory.path}${Platform.pathSeparator}mayhotel-chip-face-${widget.bookingId}-$_currentGuestIndex.jpg',
      );
      await face.writeAsBytes(result.faceImageBytes!, flush: true);
      await _deleteChipFace();
      if (!mounted) return;
      setState(() {
        _chipFaceFile = face;
        _identityResult = result;
        _documentFront = null;
        _statusMessage = result.sodRead
            ? 'Đã đọc DG1, DG2 và EF.SOD. Có thể xác thực khuôn mặt.'
            : 'Đã đọc DG1/DG2; chưa kiểm tra được chuỗi chứng thư EF.SOD.';
      });
    } catch (error) {
      if (mounted) {
        _showMessage('Không đọc được chip thật: $error');
        setState(() {
          _statusMessage =
              'Đọc chip thật thất bại. Chọn NDEF mô phỏng hoặc AI-only.';
        });
      }
    } finally {
      if (backImage != null) {
        try {
          await backImage.delete();
        } catch (_) {}
      }
      if (mounted) setState(() => _isProcessing = false);
    }
  }

  Future<void> _scanDemoNdef() async {
    setState(() {
      _isProcessing = true;
      _statusMessage = 'Áp thẻ NDEF MAY HOTEL vào mặt lưng điện thoại...';
    });
    try {
      final result = await NfcService.readSignedDemoNdef();
      await _deleteChipFace();
      if (!mounted) return;
      setState(() {
        _identityResult = result;
        _documentFront = null;
        _statusMessage =
            'NDEF hợp lệ về cấu trúc. Chụp mặt trước CCCD để AI đối sánh.';
      });
    } catch (error) {
      if (mounted) {
        _showMessage('Không đọc được thẻ NFC mô phỏng: $error');
        setState(
          () => _statusMessage = 'NFC mô phỏng thất bại. Có thể chọn AI-only.',
        );
      }
    } finally {
      if (mounted) setState(() => _isProcessing = false);
    }
  }

  Future<void> _selectAiOnly() async {
    await _deleteChipFace();
    if (!mounted) return;
    setState(() {
      _identityResult = const NfcIdentityResult(
        method: IdentityNfcMethod.aiOnly,
      );
      _documentFront = null;
      _statusMessage = 'AI-only: chụp mặt trước CCCD để đối sánh khuôn mặt.';
    });
  }

  Future<RealCccdAccessData?> _requestAccessData(
    RealCccdAccessData? detected,
  ) async {
    final number = TextEditingController(text: detected?.documentNumber ?? '');
    final dob = TextEditingController(text: _formatDate(detected?.dateOfBirth));
    final expiry = TextEditingController(
      text: _formatDate(detected?.dateOfExpiry),
    );
    String? error;
    final result = await showDialog<RealCccdAccessData>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: Text(
            detected == null ? 'Nhập khóa truy cập chip' : 'Kiểm tra MRZ',
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text(
                  'Thông tin chỉ dùng trong bộ nhớ để thiết lập BAC/PACE và không được lưu.',
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: number,
                  decoration: const InputDecoration(labelText: 'Số giấy tờ'),
                ),
                TextField(
                  controller: dob,
                  keyboardType: TextInputType.datetime,
                  decoration: const InputDecoration(
                    labelText: 'Ngày sinh',
                    hintText: 'dd/MM/yyyy',
                  ),
                ),
                TextField(
                  controller: expiry,
                  keyboardType: TextInputType.datetime,
                  decoration: const InputDecoration(
                    labelText: 'Ngày hết hạn',
                    hintText: 'dd/MM/yyyy',
                  ),
                ),
                if (error != null) ...[
                  const SizedBox(height: 8),
                  Text(error!, style: const TextStyle(color: Colors.red)),
                ],
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext),
              child: const Text('Hủy'),
            ),
            FilledButton(
              onPressed: () {
                final parsedDob = _parseDate(dob.text);
                final parsedExpiry = _parseDate(expiry.text);
                final normalizedNumber = number.text.trim().toUpperCase();
                if (normalizedNumber.length < 6 ||
                    parsedDob == null ||
                    parsedExpiry == null) {
                  setDialogState(() {
                    error = 'Kiểm tra số giấy tờ và hai ngày theo dd/MM/yyyy.';
                  });
                  return;
                }
                Navigator.pop(
                  dialogContext,
                  RealCccdAccessData(
                    documentNumber: normalizedNumber,
                    dateOfBirth: parsedDob,
                    dateOfExpiry: parsedExpiry,
                  ),
                );
              },
              child: const Text('Bắt đầu đọc chip'),
            ),
          ],
        ),
      ),
    );
    number.dispose();
    dob.dispose();
    expiry.dispose();
    return result;
  }

  Future<void> _submitVerification() async {
    final selfie = _selfie;
    final identity = _identityResult;
    final document = identity?.method == IdentityNfcMethod.realNfc
        ? _chipFaceFile
        : _documentFront;
    if (selfie == null || identity == null || document == null) {
      _showMessage(
        'Vui lòng hoàn tất selfie, phương thức xác thực và ảnh giấy tờ.',
      );
      return;
    }

    setState(() {
      _isProcessing = true;
      _statusMessage = 'Backend đang đối sánh khuôn mặt và ghi nhận kết quả...';
    });
    try {
      final method = switch (identity.method) {
        IdentityNfcMethod.realNfc => 'REAL_NFC',
        IdentityNfcMethod.signedNdef => 'SIGNED_NDEF',
        IdentityNfcMethod.aiOnly => 'AI_ONLY',
      };
      final response = await _api.submitIdentityVerification(
        bookingId: widget.bookingId,
        guestIndex: _currentGuestIndex,
        method: method,
        documentImage: document,
        selfieImage: selfie,
        identityValue: identity.identityValue,
        nfcProof: identity.ndefProof,
      );
      if (!mounted || response == null) return;
      final required =
          int.tryParse('${response['requiredGuestCount']}') ?? _totalGuests;
      final verified =
          int.tryParse('${response['verifiedGuestCount']}') ??
          _verifiedGuestCount + 1;
      final completed =
          response['allGuestsVerified'] == true || verified >= required;
      await _deleteChipFace();
      if (!mounted) return;
      setState(() {
        _totalGuests = required.clamp(1, 20).toInt();
        _verifiedGuestCount = verified.clamp(0, _totalGuests).toInt();
        _allGuestsVerified = completed;
        _currentGuestIndex = completed
            ? _totalGuests - 1
            : _verifiedGuestCount.clamp(0, _totalGuests - 1).toInt();
        _selfie = null;
        _documentFront = null;
        _identityResult = null;
        _statusMessage = completed
            ? 'Đã xác thực đủ $_verifiedGuestCount/$_totalGuests khách. Chờ lễ tân gán phòng.'
            : 'Đã xác thực $_verifiedGuestCount/$_totalGuests khách. Tiếp tục khách ${_currentGuestIndex + 1}.';
      });
    } on DioException catch (error) {
      final body = error.response?.data;
      final message = body is Map
          ? '${body['message'] ?? body['error'] ?? error.message}'
          : error.message ?? 'Không thể xác thực';
      _showMessage(message);
      if (mounted) {
        setState(
          () => _statusMessage =
              'Xác thực thất bại. Vui lòng kiểm tra và thử lại.',
        );
      }
    } catch (error) {
      _showMessage('Không thể xác thực: $error');
      if (mounted) {
        setState(() => _statusMessage = 'Xác thực thất bại. Vui lòng thử lại.');
      }
    } finally {
      if (mounted) setState(() => _isProcessing = false);
    }
  }

  Future<void> _deleteChipFace() async {
    final file = _chipFaceFile;
    _chipFaceFile = null;
    if (file != null) {
      try {
        if (await file.exists()) await file.delete();
      } catch (_) {}
    }
  }

  String _formatDate(DateTime? value) => value == null
      ? ''
      : '${value.day.toString().padLeft(2, '0')}/${value.month.toString().padLeft(2, '0')}/${value.year}';

  DateTime? _parseDate(String raw) {
    final parts = raw.trim().split('/');
    if (parts.length != 3) return null;
    try {
      final date = DateTime(
        int.parse(parts[2]),
        int.parse(parts[1]),
        int.parse(parts[0]),
      );
      return date.day == int.parse(parts[0]) &&
              date.month == int.parse(parts[1])
          ? date
          : null;
    } catch (_) {
      return null;
    }
  }

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final identity = _identityResult;
    final needsDocumentPhoto =
        identity != null && identity.method != IdentityNfcMethod.realNfc;
    return Scaffold(
      appBar: AppBar(title: const Text('Tự Nhận Phòng (AI)')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'Phòng: ${widget.roomName}',
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 6),
            Text(
              'Khách ${_allGuestsVerified ? _totalGuests : _currentGuestIndex + 1}/$_totalGuests • Đã xác thực $_verifiedGuestCount/$_totalGuests',
              textAlign: TextAlign.center,
              style: TextStyle(
                color: colors.primary,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: (_allGuestsVerified ? Colors.green : colors.primary)
                    .withValues(alpha: 0.08),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(
                  color: _allGuestsVerified ? Colors.green : colors.primary,
                ),
              ),
              child: Text(
                _statusMessage,
                textAlign: TextAlign.center,
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
            ),
            const SizedBox(height: 24),
            if (_allGuestsVerified) ...[
              const Icon(Icons.verified_user, size: 88, color: Colors.green),
              const SizedBox(height: 16),
              const Text(
                'Tất cả khách đã hoàn tất xác thực. Lễ tân sẽ gán phòng trên hệ thống web.',
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 24),
              FilledButton.icon(
                onPressed: () => Navigator.pop(context, true),
                icon: const Icon(Icons.done_all),
                label: const Text('Hoàn tất'),
              ),
            ] else ...[
              _photoBox(
                label: 'Bước 1: Ảnh selfie khuôn mặt',
                icon: Icons.face,
                file: _selfie,
                onTap: () => _takePicture(document: false),
              ),
              const SizedBox(height: 18),
              FilledButton.tonalIcon(
                onPressed: _isProcessing ? null : _chooseIdentityMethod,
                icon: const Icon(Icons.nfc),
                label: Text(_methodLabel(identity?.method)),
              ),
              const SizedBox(height: 8),
              const Text(
                'NFC thật là tính năng nghiên cứu; hệ thống không kết nối C06. NDEF chỉ dùng để mô phỏng quy trình.',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 12, color: Colors.grey),
              ),
              const SizedBox(height: 18),
              if (identity?.method == IdentityNfcMethod.realNfc)
                Container(
                  padding: const EdgeInsets.all(18),
                  decoration: BoxDecoration(
                    color: Colors.green.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: Colors.green),
                  ),
                  child: const Row(
                    children: [
                      Icon(Icons.credit_card, color: Colors.green),
                      SizedBox(width: 12),
                      Expanded(
                        child: Text('Đã đọc ảnh DG2 từ chip để đối sánh.'),
                      ),
                    ],
                  ),
                )
              else
                _photoBox(
                  label: 'Bước 3: Ảnh mặt trước CCCD',
                  icon: Icons.credit_card,
                  file: _documentFront,
                  enabled: needsDocumentPhoto,
                  onTap: () => _takePicture(document: true),
                ),
              const SizedBox(height: 28),
              FilledButton.icon(
                onPressed: _isProcessing ? null : _submitVerification,
                icon: _isProcessing
                    ? const SizedBox.square(
                        dimension: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.verified_user),
                label: const Text('Tiến hành xác thực'),
              ),
            ],
          ],
        ),
      ),
    );
  }

  String _methodLabel(IdentityNfcMethod? method) => switch (method) {
    IdentityNfcMethod.realNfc => 'Bước 2: Đã đọc chip CCCD thật',
    IdentityNfcMethod.signedNdef => 'Bước 2: Đã đọc NDEF mô phỏng',
    IdentityNfcMethod.aiOnly => 'Bước 2: Đã chọn AI-only',
    null => 'Bước 2: Chọn NFC / AI fallback',
  };

  Widget _photoBox({
    required String label,
    required IconData icon,
    required File? file,
    required VoidCallback onTap,
    bool enabled = true,
  }) => GestureDetector(
    onTap: enabled
        ? onTap
        : () => _showMessage('Vui lòng chọn phương thức xác thực trước.'),
    child: Column(
      children: [
        Container(
          height: 128,
          width: double.infinity,
          decoration: BoxDecoration(
            color: enabled
                ? Theme.of(context).colorScheme.surfaceContainerHighest
                : Colors.grey.shade200,
            borderRadius: BorderRadius.circular(15),
            border: Border.all(
              color: enabled
                  ? Theme.of(context).colorScheme.outline
                  : Colors.grey,
            ),
            image: file == null
                ? null
                : DecorationImage(image: FileImage(file), fit: BoxFit.cover),
          ),
          child: file == null
              ? Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(
                      icon,
                      size: 42,
                      color: enabled
                          ? Theme.of(context).colorScheme.primary
                          : Colors.grey,
                    ),
                    const SizedBox(height: 6),
                    Text(enabled ? 'Bấm chụp' : 'Chưa khả dụng'),
                  ],
                )
              : const Icon(
                  Icons.check_circle,
                  color: Colors.greenAccent,
                  size: 42,
                ),
        ),
        const SizedBox(height: 8),
        Text(label, style: const TextStyle(fontWeight: FontWeight.bold)),
      ],
    ),
  );
}
