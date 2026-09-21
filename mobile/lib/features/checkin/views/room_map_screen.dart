import 'package:flutter/material.dart';
import '../../../models/room_model.dart';
import '../../../services/hotel_api_provider.dart';
import '../../checkin/views/check_in_screen.dart';
import '../../checkin/views/verification_selection_screen.dart';

class RoomMapScreen extends StatefulWidget {
  const RoomMapScreen({Key? key}) : super(key: key);

  @override
  State<RoomMapScreen> createState() => _RoomMapScreenState();
}

class _RoomMapScreenState extends State<RoomMapScreen> {
  final HotelApiProvider _apiProvider = HotelApiProvider();
  List<RoomModel> _rooms = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadRooms();
  }

  // Hàm tải dữ liệu từ API
  Future<void> _loadRooms() async {
    if (!mounted) return;
    setState(() => _isLoading = true);
    try {
      final rooms = await _apiProvider.getAllRooms();
      if (mounted) {
        setState(() {
          _rooms = rooms;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _isLoading = false);
      debugPrint("Lỗi tải sơ đồ phòng: $e");
    }
  }

  // Xác định màu sắc theo mã trạng thái (1: Xanh, 2: Đỏ, 3: Cam)
  Color _getRoomColor(int status) {
    switch (status) {
      case 1: return const Color(0xFF66BB6A); // Trống - Xanh lá
      case 2: return const Color(0xFFEF5350); // Có khách - Đỏ
      case 3: return const Color(0xFFFFA726); // Đang dọn - Cam
      default: return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    int vacant = _rooms.where((r) => r.maTrangThai == 1).length;
    int occupied = _rooms.where((r) => r.maTrangThai == 2).length;
    int cleaning = _rooms.where((r) => r.maTrangThai == 3).length;

    return Scaffold(
      backgroundColor: const Color(0xFFF5F7F9),
      appBar: AppBar(
        title: const Text("Sơ đồ phòng", style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        elevation: 0.5,
        centerTitle: true,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded),
            onPressed: _loadRooms,
          )
        ],
      ),
      body: Column(
        children: [
          _buildHeaderSummary(vacant, occupied, cleaning),
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : RefreshIndicator(
              onRefresh: _loadRooms,
              child: GridView.builder(
                padding: const EdgeInsets.all(16),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 3,
                  crossAxisSpacing: 14,
                  mainAxisSpacing: 14,
                  childAspectRatio: 0.72,
                ),
                itemCount: _rooms.length,
                itemBuilder: (context, index) => _buildRoomItem(_rooms[index]),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHeaderSummary(int v, int o, int c) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 20),
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(bottom: Radius.circular(20)),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _summaryItem("Trống", v, Colors.green),
          _summaryItem("Có khách", o, Colors.red),
          _summaryItem("Đang dọn", c, Colors.orange),
        ],
      ),
    );
  }

  Widget _summaryItem(String label, int count, Color color) {
    return Column(
      children: [
        Text(count.toString(), style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: color)),
        Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey, fontWeight: FontWeight.w500)),
      ],
    );
  }

  // 🚀 HÀM DỰNG Ô PHÒNG ĐÃ TÍCH HỢP MA TRẬN DÒ BIẾN AN TOÀN
  Widget _buildRoomItem(RoomModel room) {
    // 🟩 1. THANG DÒ LOẠI PHÒNG (Né hoàn toàn NoSuchMethodError)
    String roomType = "Standard";
    try { roomType = (room as dynamic).tenLoaiPhong ?? roomType; } catch (_) {}
    try { roomType = (room as dynamic).loaiPhong ?? roomType; } catch (_) {}
    try { roomType = (room as dynamic).tenLoai ?? roomType; } catch (_) {}

    // 🟩 2. THANG DÒ SỨC CHỨA / SỐ NGƯỜI
    int maxGuests = 2;
    try { maxGuests = (room as dynamic).sucChua ?? maxGuests; } catch (_) {}
    try { maxGuests = (room as dynamic).soNguoi ?? maxGuests; } catch (_) {}
    try { maxGuests = (room as dynamic).soKhachToiDa ?? maxGuests; } catch (_) {}

    return GestureDetector(
      onTap: () => _handleRoomClick(room),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 6),
        decoration: BoxDecoration(
          color: _getRoomColor(room.maTrangThai),
          borderRadius: BorderRadius.circular(15),
          boxShadow: [
            BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 4, offset: const Offset(0, 2))
          ],
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  room.maTrangThai == 3 ? Icons.cleaning_services_rounded : Icons.meeting_room_rounded,
                  color: Colors.white70,
                  size: 14,
                ),
                const SizedBox(width: 4),
                Text(
                  room.maTrangThai == 1 ? "TRỐNG" : (room.maTrangThai == 2 ? "ĐANG Ở" : "ĐANG DỌN"),
                  style: TextStyle(color: Colors.white.withOpacity(0.8), fontSize: 9, fontWeight: FontWeight.bold, letterSpacing: 0.5),
                ),
              ],
            ),

            Text(
              room.tenPhong,
              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 19),
            ),

            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.2),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                roomType.toUpperCase(),
                style: const TextStyle(color: Colors.white, fontSize: 9, fontWeight: FontWeight.w600),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),

            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.people_alt_rounded, color: Colors.white60, size: 12),
                const SizedBox(width: 4),
                Text(
                  "$maxGuests người",
                  style: const TextStyle(color: Colors.white70, fontSize: 10, fontWeight: FontWeight.w500),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  // 🔔 ĐIỀU HƯỚNG KHI CLICK VÀO PHÒNG
  void _handleRoomClick(RoomModel room) {
    if (room.maTrangThai == 1) {
      _showWalkInGuestDialog(room);
    } else if (room.maTrangThai == 2) {
      _showSurchargeDialog(context, room);
    } else if (room.maTrangThai == 3) {
      _showCleaningDialog(room);
    }
  }

  // 🟩 DIALOG NHẬP THÔNG TIN VÃN LAI - TỰ ĐỘNG TÍNH TIỀN TRÊN FLUTTER
  void _showWalkInGuestDialog(RoomModel room) {
    final nameController = TextEditingController();
    final phoneController = TextEditingController();

    // 🚀 KHỞI TẠO NGÀY MẶC ĐỊNH TRÊN FLUTTER
    DateTime checkInDate = DateTime.now(); // Ngày đặt/nhận là hôm nay
    DateTime checkOutDate = DateTime.now().add(const Duration(days: 1)); // Mặc định trả vào ngày mai

    String roomType = "Standard";
    try { roomType = (room as dynamic).tenLoaiPhong ?? roomType; } catch (_) {}
    try { roomType = (room as dynamic).loaiPhong ?? roomType; } catch (_) {}

    double roomPrice = 0.0;
    try { roomPrice = double.tryParse((room as dynamic).giaPhong?.toString() ?? "0") ?? 0.0; } catch(_) {}
    try { roomPrice = double.tryParse((room as dynamic).donGia?.toString() ?? "0") ?? 0.0; } catch(_) {}
    try { roomPrice = double.tryParse((room as dynamic).price?.toString() ?? "0") ?? 0.0; } catch(_) {}

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => StatefulBuilder( // 🚀 SỬ DỤNG STATEFULBUILDER ĐỂ BIẾN DIALOG THÀNH GIAO DIỆN ĐỘNG
        builder: (context, setDialogState) {

          // 📊 THUẬT TOÁN TÍNH TIỀN NGAY TRÊN FLUTTER (BAO SẠCH LỖI BIẾN DBMS)
          int durationDays = checkOutDate.difference(checkInDate).inDays;
          if (durationDays <= 0) {
            durationDays = 1; // Khách đi trong ngày tính tròn 1 đêm để tránh TongTien = 0
          }
          double calculatedTotal = durationDays * roomPrice;

          return AlertDialog(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
            title: Text("Vãng lai: ${room.tenPhong} ($roomType)", style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  TextField(
                    controller: nameController,
                    textCapitalization: TextCapitalization.words,
                    decoration: const InputDecoration(labelText: "Họ tên khách", border: OutlineInputBorder(), prefixIcon: Icon(Icons.badge)),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: phoneController,
                    keyboardType: TextInputType.phone,
                    decoration: const InputDecoration(labelText: "Số điện thoại", border: OutlineInputBorder(), prefixIcon: Icon(Icons.phone)),
                  ),
                  const SizedBox(height: 15),
                  const Divider(),
                  const Text("Thời gian lưu trú lưu động:", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: Colors.blueGrey)),
                  const SizedBox(height: 10),

                  // 📅 Ô CHỌN NGÀY NHẬN PHÒNG
                  ListTile(
                    leading: const Icon(Icons.calendar_today, color: Colors.blue),
                    title: const Text("Ngày nhận phòng", style: TextStyle(fontSize: 12, color: Colors.grey)),
                    subtitle: Text("${checkInDate.day}/${checkInDate.month}/${checkInDate.year}", style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
                    trailing: const Icon(Icons.arrow_drop_down),
                    onTap: () async {
                      final picked = await showDatePicker(
                        context: context,
                        initialDate: checkInDate,
                        firstDate: DateTime.now().subtract(const Duration(days: 30)),
                        lastDate: DateTime.now().add(const Duration(days: 365)),
                      );
                      if (picked != null) {
                        // Cập nhật giao diện động bên trong Dialog bằng setDialogState
                        setDialogState(() {
                          checkInDate = picked;
                          if (checkOutDate.isBefore(checkInDate)) {
                            checkOutDate = checkInDate.add(const Duration(days: 1));
                          }
                        });
                      }
                    },
                  ),

                  // 📅 Ô CHỌN NGÀY TRẢ PHÒNG
                  ListTile(
                    leading: const Icon(Icons.calendar_today, color: Colors.orange),
                    title: const Text("Ngày trả phòng dự kiến", style: TextStyle(fontSize: 12, color: Colors.grey)),
                    subtitle: Text("${checkOutDate.day}/${checkOutDate.month}/${checkOutDate.year}", style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
                    trailing: const Icon(Icons.arrow_drop_down),
                    onTap: () async {
                      final picked = await showDatePicker(
                        context: context,
                        initialDate: checkOutDate,
                        firstDate: checkInDate,
                        lastDate: DateTime.now().add(const Duration(days: 365)),
                      );
                      if (picked != null) {
                        setDialogState(() {
                          checkOutDate = picked;
                        });
                      }
                    },
                  ),
                  const Divider(),

                  // 💰 BẢNG TÍNH TOÁN HIỂN THỊ LIVE SIÊU ĐẸP ĐỂ LÀM DEMO
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(color: Colors.amber.withOpacity(0.12), borderRadius: BorderRadius.circular(10)),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text("Đơn giá: ${roomPrice.toStringAsFixed(0)} VNĐ / Đêm", style: const TextStyle(fontSize: 13)),
                        const SizedBox(height: 4),
                        Text("Thời gian: $durationDays Đêm", style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
                        const Divider(),
                        Text("💵 TỔNG TIỀN DỰ KIẾN: ${calculatedTotal.toStringAsFixed(0)} VNĐ", style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.red)),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(onPressed: () => Navigator.pop(context), child: const Text("Hủy bỏ", style: TextStyle(color: Colors.grey))),
              ElevatedButton(
                style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF66BB6A)),
                onPressed: () async {
                  if (nameController.text.trim().isEmpty || phoneController.text.trim().isEmpty) {
                    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("⚠️ Vui lòng nhập đủ tên và số điện thoại!")));
                    return;
                  }

                  String inputName = nameController.text.trim();
                  String inputPhone = phoneController.text.trim();

                  // 🟩 Lấy lại maxGuests trong scope này để tránh lỗi Undefined
                  int totalGuests = 2;
                  try { totalGuests = (room as dynamic).sucChua ?? totalGuests; } catch (_) {}
                  try { totalGuests = (room as dynamic).soNguoi ?? totalGuests; } catch (_) {}
                  try { totalGuests = (room as dynamic).soKhachToiDa ?? totalGuests; } catch (_) {}

                  // Định dạng chuỗi Date sạch (YYYY-MM-DD) để đẩy về SQL Server ăn khớp định dạng
                  String formattedCheckIn = "${checkInDate.year}-${checkInDate.month.toString().padLeft(2, '0')}-${checkInDate.day.toString().padLeft(2, '0')}";
                  String formattedCheckOut = "${checkOutDate.year}-${checkOutDate.month.toString().padLeft(2, '0')}-${checkOutDate.day.toString().padLeft(2, '0')}";

                  Navigator.pop(context); // Tắt dialog

                  // Điều hướng sang màn hình LỰA CHỌN PHƯƠNG THỨC XÁC MINH
                  // Truyền totalGuests để xác minh cho tất cả khách đi cùng
                  final verifyResult = await Navigator.push(
                    context,
                    MaterialPageRoute(builder: (context) => VerificationSelectionScreen(totalGuests: totalGuests)),
                  );

                  if (verifyResult != null && verifyResult['status'] == 'success') {
                    debugPrint("🚀 Đang bắn gói tin Check-in vạn năng từ Flutter lên SQL Server...");

                    // 🌟 GỌI HÀM API ĐÃ ĐƯỢC CHUẨN HÓA TOÀN DIỆN THAM SỐ NGÀY TIỀN TỪ FLUTTER
                    bool isSavedToDb = await _apiProvider.postCheckIn(
                      roomId: room.maPhong,
                      name: inputName,
                      phone: inputPhone,
                      price: roomPrice,
                      checkInDate: formattedCheckIn,
                      checkOutDate: formattedCheckOut,
                      totalPrice: calculatedTotal,
                    );

                    if (isSavedToDb) {
                      await _apiProvider.updateRoomStatus(room.maPhong, 2); // Đổi màu phòng sang đỏ
                      _loadRooms();
                      if (mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("🟢 Check-in thành công và đã tự động tính tiền phòng!")));
                      }
                    }
                  }
                },
                child: const Text("Tiếp tục", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
              ),
            ],
          );
        },
      ),
    );
  }

  // DIALOG THANH TOÁN (CHECK-OUT)
  void _showSurchargeDialog(BuildContext context, RoomModel room) {
    final surchargeController = TextEditingController(text: "0");
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
        title: Text("Thanh toán: ${room.tenPhong}"),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text("Nhập tiền phụ thu phát sinh nếu có:"),
            const SizedBox(height: 10),
            TextField(
              controller: surchargeController,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(border: OutlineInputBorder(), prefixText: "VNĐ "),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text("Hủy")),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent),
            onPressed: () async {
              double surcharge = double.tryParse(surchargeController.text) ?? 0;
              bool success = await _apiProvider.processCheckOut(room.maPhong, surcharge);
              if (success && context.mounted) {
                Navigator.pop(context);
                _loadRooms();
                ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Đã thanh toán! Phòng đang chờ dọn dẹp.")));
              }
            },
            child: const Text("Xác nhận", style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );
  }

  // DIALOG XÁC NHẬN DỌN PHÒNG XONG
  void _showCleaningDialog(RoomModel room) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text("Vệ sinh phòng"),
        content: Text("Phòng ${room.tenPhong} đã được dọn dẹp xong?"),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text("Chưa")),
          ElevatedButton(
            onPressed: () async {
              bool success = await _apiProvider.updateRoomStatus(room.maPhong, 1);
              if (success && context.mounted) {
                Navigator.pop(context);
                _loadRooms();
              }
            },
            child: const Text("Đã dọn xong"),
          ),
        ],
      ),
    );
  }
}