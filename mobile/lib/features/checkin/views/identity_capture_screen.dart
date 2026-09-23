import 'package:flutter/material.dart';
import '../../../core/utils/responsive.dart';
import '../../auth/models/guest_identity.dart';
import '../../../services/nfc_service.dart';
import '../../../services/notification_service.dart';
import 'package:image_picker/image_picker.dart';
import 'dart:io';
class IdentityCaptureScreen extends StatefulWidget {
  final int totalGuests;
  const IdentityCaptureScreen({Key? key, required this.totalGuests}) : super(key: key);

  @override
  State<IdentityCaptureScreen> createState() => _IdentityCaptureScreenState();
}

class _IdentityCaptureScreenState extends State<IdentityCaptureScreen> {
  late List<GuestIdentity> guestList;

  // 🟩 CÁC BIẾN QUẢN LÝ TRẠNG THÁI QUÉT NFC CHO TỪNG KHÁCH HÀNG
  late List<bool> _isScanningList;
  late List<String> _scanResultList;

  // 🟩 CÁC BIẾN QUẢN LÝ ẢNH CHỤP CHO TỪNG KHÁCH HÀNG
  late List<File?> _frontImages;
  late List<File?> _backImages;
  late List<File?> _selfieImages;
  
  final ImagePicker _picker = ImagePicker();

  @override
  void initState() {
    super.initState();
    // Tạo danh sách slot dựa trên số lượng khách
    guestList = List.generate(widget.totalGuests, (index) => GuestIdentity(guestName: "Khách hàng ${index + 1}"));

    // Khởi tạo khay bộ nhớ đệm trạng thái NFC tương ứng với số lượng khách
    _isScanningList = List.generate(widget.totalGuests, (index) => false);
    _scanResultList = List.generate(widget.totalGuests, (index) => "");
    
    // Khởi tạo khay bộ nhớ chứa ảnh cho từng khách
    _frontImages = List.generate(widget.totalGuests, (index) => null);
    _backImages = List.generate(widget.totalGuests, (index) => null);
    _selfieImages = List.generate(widget.totalGuests, (index) => null);
  }

  // 🔔 HÀM KÍCH NỔ QUÉT THẺ NFC VÀ XỬ LÝ SỰ CỐ RÚT THẺ NHANH
  void _handleNfcScan(int index) async {
    setState(() {
      _isScanningList[index] = true;
      _scanResultList[index] = "⌛ Đang kết nối... Hãy giữ cố định thẻ CCCD sát mặt sau máy.";
    });

    await NfcService.startCccdScanning(
      onSuccess: (rawData) {
        setState(() {
          _isScanningList[index] = false;
          _scanResultList[index] = "🟢 XÁC THỰC THÀNH CÔNG:\n$rawData";
          // Cập nhật tên khách hàng tự động từ dữ liệu bốc từ thẻ NFC nếu muốn
          if (rawData.contains("Họ tên:")) {
            final parts = rawData.split('|');
            final namePart = parts.firstWhere((p) => p.contains("Họ tên:"), orElse: () => "");
            if (namePart.isNotEmpty) {
              guestList[index].guestName = namePart.replaceAll("Họ tên:", "").trim();
            }
          }
        });

        // Bắn thông báo đẩy reo ting ting lên đỉnh màn hình điện thoại
        NotificationService().showBookingNotification(
          id: index,
          hotelName: "May Hotel",
          roomType: "Hồ sơ ${guestList[index].guestName}",
        );
      },
      onError: (errorMessage) {
        setState(() {
          _isScanningList[index] = false;
          // Hiển thị trực tiếp chuỗi lỗi "Rút thẻ quá nhanh" màu đỏ lên thẻ của khách đó
          _scanResultList[index] = errorMessage;
        });
      },
    );
  }

  // 📸 HÀM CHỤP ẢNH CHUNG (MẶT TRƯỚC, MẶT SAU, SELFIE)
  Future<void> _takePicture(int guestIndex, String type) async {
    try {
      // Ưu tiên camera trước nếu là ảnh Selfie
      final source = ImageSource.camera;
      final cameraDevice = type == 'selfie' ? CameraDevice.front : CameraDevice.rear;
      
      final XFile? photo = await _picker.pickImage(
        source: source,
        preferredCameraDevice: cameraDevice,
        imageQuality: 85,
      );

      if (photo != null) {
        setState(() {
          if (type == 'front') _frontImages[guestIndex] = File(photo.path);
          else if (type == 'back') _backImages[guestIndex] = File(photo.path);
          else if (type == 'selfie') _selfieImages[guestIndex] = File(photo.path);
        });
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text("Lỗi khởi động máy ảnh: $e")),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      appBar: AppBar(
        title: const Text("Xác minh danh tính", style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: colorScheme.surface,
        foregroundColor: colorScheme.onSurface,
        elevation: 0.5,
      ),
      body: Column(
        children: [
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.all(20),
              itemCount: guestList.length,
              itemBuilder: (context, index) => _buildGuestCaptureCard(index, colorScheme),
            ),
          ),
          _buildSubmitButton(context),
        ],
      ),
    );
  }

  Widget _buildGuestCaptureCard(int index, ColorScheme colorScheme) {
    return Container(
      margin: const EdgeInsets.only(bottom: 25),
      padding: const EdgeInsets.all(15),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: colorScheme.outlineVariant.withOpacity(0.5)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(guestList[index].guestName,
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                    overflow: TextOverflow.ellipsis),
              ),
              Row(
                children: [
                  // 🟩 NÚT QUÉT THẺ NFC THỰC CHIẾN
                  IconButton(
                    icon: Icon(
                      Icons.nfc,
                      color: _isScanningList[index] ? Colors.amber : colorScheme.primary,
                    ),
                    tooltip: "Quét CCCD qua NFC",
                    onPressed: _isScanningList[index] ? null : () => _handleNfcScan(index),
                  ),
                  // Nút giả lập VNeID giữ nguyên
                  TextButton.icon(
                    onPressed: () => setState(() => guestList[index].isVNeID = !guestList[index].isVNeID),
                    icon: Icon(Icons.qr_code_scanner, size: 18, color: guestList[index].isVNeID ? Colors.green : Colors.grey),
                    label: Text(guestList[index].isVNeID ? "Đã dùng VNeID" : "Dùng VNeID",
                        style: TextStyle(color: guestList[index].isVNeID ? Colors.green : Colors.grey, fontSize: 12)),
                  ),
                ],
              ),
            ],
          ),

          // 🟩 BẢNG BÁO CÁO TIẾN TRÌNH VÀ KẾT QUẢ NFC QUÉT THẺ TRỰC QUAN
          if (_scanResultList[index].isNotEmpty) ...[
            const SizedBox(height: 10),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: _scanResultList[index].contains("❌")
                    ? Colors.red.withOpacity(0.1)
                    : (_scanResultList[index].contains("🟢") ? Colors.green.withOpacity(0.1) : colorScheme.surfaceVariant.withOpacity(0.4)),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(
                  color: _scanResultList[index].contains("❌")
                      ? Colors.red.withOpacity(0.3)
                      : (_scanResultList[index].contains("🟢") ? Colors.green.withOpacity(0.3) : colorScheme.outlineVariant),
                ),
              ),
              child: Text(
                _scanResultList[index],
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w500,
                  color: _scanResultList[index].contains("❌")
                      ? Colors.red[700]
                      : (_scanResultList[index].contains("🟢") ? Colors.green[800] : colorScheme.onSurfaceVariant),
                ),
              ),
            ),
          ],

          const SizedBox(height: 15),
          Row(
            children: [
              Expanded(
                child: _buildImagePickerBox(
                  label: "Mặt trước CCCD", 
                  icon: Icons.camera_front, 
                  imageFile: _frontImages[index], 
                  onTap: () => _takePicture(index, 'front'),
                ),
              ),
              const SizedBox(width: 15),
              Expanded(
                child: _buildImagePickerBox(
                  label: "Mặt sau CCCD", 
                  icon: Icons.camera_rear, 
                  imageFile: _backImages[index], 
                  onTap: () => _takePicture(index, 'back'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 15),
          // THÊM CHỨC NĂNG CHỤP SELFIE KHUÔN MẶT ĐỐI CHIẾU
          SizedBox(
            width: double.infinity,
            child: _buildImagePickerBox(
              label: "Chụp ảnh khuôn mặt (Xác thực Liveness/Face Match)", 
              icon: Icons.face_retouching_natural, 
              imageFile: _selfieImages[index], 
              onTap: () => _takePicture(index, 'selfie'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildImagePickerBox({
    required String label, 
    required IconData icon, 
    required File? imageFile,
    required VoidCallback onTap,
  }) {
    final colorScheme = Theme.of(context).colorScheme;
    return GestureDetector(
      onTap: onTap,
      child: Column(
        children: [
          Container(
            height: 100,
            width: double.infinity,
            decoration: BoxDecoration(
              color: colorScheme.surfaceVariant.withOpacity(0.3),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: colorScheme.outlineVariant, style: BorderStyle.solid),
              image: imageFile != null 
                  ? DecorationImage(image: FileImage(imageFile), fit: BoxFit.cover) 
                  : null,
            ),
            child: imageFile == null 
                ? Icon(icon, color: colorScheme.primary.withOpacity(0.5), size: 30)
                : Container(
                    decoration: BoxDecoration(
                      color: Colors.black.withOpacity(0.3),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(Icons.check_circle, color: Colors.greenAccent, size: 30),
                  ),
          ),
          const SizedBox(height: 8),
          Text(label, style: const TextStyle(fontSize: 11, color: Colors.grey), textAlign: TextAlign.center),
        ],
      ),
    );
  }
// Sửa lại khối lệnh nút bấm hoàn tất tại identity_capture_screen.dart
  Widget _buildSubmitButton(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      color: Theme.of(context).colorScheme.surface,
      child: ElevatedButton(
        style: ElevatedButton.styleFrom(
          backgroundColor: Theme.of(context).colorScheme.primary,
          minimumSize: const Size(double.infinity, 55),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
        ),
        onPressed: () {
          // 🚀 CẢI TIẾN LỚN: Gom sạch danh sách họ tên và data NFC của TẤT CẢ các khách hàng đã quét
          List<Map<String, dynamic>> structuredGuests = [];
          for (int i = 0; i < guestList.length; i++) {
            structuredGuests.add({
              'name': guestList[i].guestName,
              'nfcData': _scanResultList[i],
              'hasSelfie': _selfieImages[i] != null,
              'hasFrontId': _frontImages[i] != null,
              'hasBackId': _backImages[i] != null,
            });
          }

          // Trả kết quả mảng cấu trúc về cho RoomMapScreen đón nhận
          Navigator.pop(context, {
            'status': 'success',
            'guestDataList': structuredGuests,
          });
        },
        child: const Text("Hoàn tất xác minh", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
      ),
    );
  }
}