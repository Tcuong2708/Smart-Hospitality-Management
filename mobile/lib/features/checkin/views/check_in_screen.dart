import 'package:flutter/material.dart';
import 'package:may_hotel_app/models/room_model.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';

class CheckInScreen extends StatefulWidget {
  final RoomModel? room;
  const CheckInScreen({super.key, this.room});

  @override
  State<CheckInScreen> createState() => _CheckInScreenState();
}

class _CheckInScreenState extends State<CheckInScreen> {
  final HotelApiProvider apiProvider = HotelApiProvider();

  // 🟩 KHAY LƯU TRỮ DANH SÁCH PHÒNG LIVE TOÀN HỆ THỐNG
  List<RoomModel> _allRooms = [];
  bool _isLoading = true;
  int _initialTab = 0;

  @override
  void initState() {
    super.initState();
    // Xác định tab mặc định ban đầu nếu có phòng truyền qua
    if (widget.room != null) {
      _initialTab = (widget.room!.maTrangThai == 2) ? 1 : 0;
    }
    // Kích nổ lệnh tải dữ liệu live từ SQL Server
    _loadAllRoomsData();
  }

  // 🔄 HÀM TẢI LIVE TẤT CẢ CÁC PHÒNG ĐỂ LÀM MỚI TAB VÀ QUÉT SẠCH PHÒNG ĐANG Ở
  Future<void> _loadAllRoomsData() async {
    if (!mounted) return;
    setState(() => _isLoading = true);
    try {
      final rooms = await apiProvider.getAllRooms();
      if (mounted) {
        setState(() {
          _allRooms = rooms;
          _isLoading = false;
        });
        debugPrint("🟢 Đã đồng bộ thành công ${_allRooms.length} phòng từ SQL Server.");
      }
    } catch (e) {
      if (mounted) setState(() => _isLoading = false);
      debugPrint("❌ Lỗi tải danh sách phòng mục CheckIn: $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return DefaultTabController(
      length: 2,
      initialIndex: _initialTab,
      child: Scaffold(
        backgroundColor: theme.scaffoldBackgroundColor,
        appBar: AppBar(
          title: Text(widget.room != null ? "Xử lý ${widget.room!.tenPhong}" : "Quản lý Lưu Trú Live"),
          backgroundColor: colorScheme.surface,
          foregroundColor: colorScheme.onSurface,
          elevation: 0.5,
          actions: [
            // Nút đồng bộ nhanh cho Lễ tân ép tải lại danh sách
            IconButton(
              icon: const Icon(Icons.sync_rounded),
              onPressed: _loadAllRoomsData,
            )
          ],
          bottom: const TabBar(
            tabs: [
              Tab(icon: Icon(Icons.login), text: "Check-in"),
              Tab(icon: Icon(Icons.logout), text: "Check-out"),
            ],
          ),
        ),
        body: _isLoading
            ? const Center(child: CircularProgressIndicator()) // Vòng xoay nạp dữ liệu live từ backend
            : TabBarView(
          children: [
            _buildGuestTabContent(isCheckIn: true),
            _buildGuestTabContent(isCheckIn: false),
          ],
        ),
      ),
    );
  }

  // 🚀 HÀM PHÂN TÁCH LUỒNG DỮ LIỆU THỰC CHIẾN ĐỘNG CHO TỪNG TAB
  Widget _buildGuestTabContent({required bool isCheckIn}) {
    List<RoomModel> filteredRooms = [];

    if (widget.room != null) {
      // 👉 TRƯỜNG HỢP 1: Vào từ sơ đồ phòng -> Lọc tìm đúng chiếc phòng đó từ DB live
      final liveRoom = _allRooms.firstWhere(
            (r) => r.maPhong == widget.room!.maPhong,
        orElse: () => widget.room!,
      );

      bool isCorrectTab = (isCheckIn && liveRoom.maTrangThai == 1) || (!isCheckIn && liveRoom.maTrangThai == 2);
      if (isCorrectTab) {
        filteredRooms = [liveRoom];
      }
    } else {
      // 👉 TRƯỜNG HỢP 2: Vào từ Menu tổng -> Quét sạch toàn bộ các phòng thỏa mãn điều kiện dưới Database
      // Tab Check-in: Lấy tất cả phòng đang Trống (maTrangThai == 1)
      // Tab Check-out: Lấy tất cả phòng Đang ở (maTrangThai == 2), chấp hết mọi loại khách vãng lai/tài khoản
      filteredRooms = _allRooms.where((r) => isCheckIn ? r.maTrangThai == 1 : r.maTrangThai == 2).toList();
    }

    // Nếu bộ lọc trống không tìm thấy phòng nào phù hợp
    if (filteredRooms.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.inventory_2_outlined, size: 60, color: Colors.grey[400]),
            const SizedBox(height: 10),
            Text(
              isCheckIn ? "Hiện tại không có phòng nào trống để Check-in." : "Hiện tại không có phòng nào đang lưu trú.",
              style: TextStyle(color: Colors.grey[600], fontSize: 15, fontWeight: FontWeight.w500),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      );
    }

    // Hiển thị danh sách card phòng dưới dạng cuộn ListView chuyên nghiệp
    return ListView.builder(
      padding: const EdgeInsets.all(15),
      itemCount: filteredRooms.length,
      itemBuilder: (context, index) => _buildRoomCard(context, filteredRooms[index], isCheckIn),
    );
  }

  // Card hiển thị thông tin phòng và nút bấm xử lý nhanh
  Widget _buildRoomCard(BuildContext context, RoomModel room, bool isCheckIn) {
    return Card(
      elevation: 2,
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
        leading: CircleAvatar(
          backgroundColor: isCheckIn ? Colors.blue : Colors.orange,
          child: Icon(isCheckIn ? Icons.person_add : Icons.person_remove, color: Colors.white),
        ),
        title: Text(room.tenPhong, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
        subtitle: Text(isCheckIn ? "Phòng trống sẵn sàng" : "Đang lưu trú (Hỗ trợ trả phòng sớm)"),
        trailing: ElevatedButton(
          style: ElevatedButton.styleFrom(
            backgroundColor: isCheckIn ? Colors.green : Colors.redAccent,
            foregroundColor: Colors.white,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          ),
          onPressed: () {
            if (isCheckIn) {
              _showCheckInDialog(context, room);
            } else {
              _showSurchargeDialog(context, room);
            }
          },
          child: Text(isCheckIn ? "Nhận phòng" : "Trả phòng"),
        ),
      ),
    );
  }

  // --- DIALOG XỬ LÝ CHECK-IN ---
  // --- DIALOG XỬ LÝ CHECK-IN (ĐÃ HOÀN THIỆN ĐỒNG BỘ LOGIC NGÀY THÁNG VÀ TÍNH TIỀN) ---
  void _showCheckInDialog(BuildContext context, RoomModel room) {
    final nameController = TextEditingController();
    final phoneController = TextEditingController();

    // 🚀 KHỞI TẠO NGÀY MẶC ĐỊNH NGAY TRÊN ĐOẠN ĐỐI THOẠI Flutter
    DateTime checkInDate = DateTime.now(); // Mặc định ngày nhận là hôm nay
    DateTime checkOutDate = DateTime.now().add(const Duration(days: 1)); // Mặc định ngày trả là ngày mai

    // Trích xuất giá phòng an toàn từ đối tượng room
    double roomPrice = double.tryParse((room as dynamic).price?.toString() ?? (room as dynamic).giaPhong?.toString() ?? "0") ?? 0.0;

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => StatefulBuilder( // 🚀 SỬ DỤNG STATEFULBUILDER ĐỂ BIẾN DIALOG THÀNH GIAO DIỆN ĐỘNG LIVE
        builder: (context, setDialogState) {

          // 📊 THUẬT TOÁN TÍNH SỐ ĐÊM LƯU TRÚ VÀ NHÂN CHUỖI TỔNG TIỀN LIVE
          int durationDays = checkOutDate.difference(checkInDate).inDays;
          if (durationDays <= 0) {
            durationDays = 1; // Khách đi luôn trong ngày tính tròn 1 đêm để tránh TongTien = 0
          }
          double calculatedTotal = durationDays * roomPrice;

          return AlertDialog(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
            title: Text("Check-in: ${room.tenPhong}", style: const TextStyle(fontWeight: FontWeight.bold)),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  TextField(
                    controller: nameController,
                    textCapitalization: TextCapitalization.words,
                    decoration: const InputDecoration(labelText: "Tên khách hàng", icon: Icon(Icons.person)),
                  ),
                  TextField(
                    controller: phoneController,
                    decoration: const InputDecoration(labelText: "Số điện thoại", icon: Icon(Icons.phone)),
                    keyboardType: TextInputType.phone,
                  ),
                  const SizedBox(height: 15),
                  const Divider(),
                  const Text("Chọn thời gian thuê phòng:", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: Colors.blueGrey)),
                  const SizedBox(height: 5),

                  // 📅 Ô BẤM CHỌN NGÀY NHẬN PHÒNG
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.calendar_today, color: Colors.blue, size: 20),
                    title: const Text("Ngày nhận phòng", style: TextStyle(fontSize: 11, color: Colors.grey)),
                    subtitle: Text("${checkInDate.day}/${checkInDate.month}/${checkInDate.year}", style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold)),
                    trailing: const Icon(Icons.arrow_drop_down),
                    onTap: () async {
                      final picked = await showDatePicker(
                        context: context,
                        initialDate: checkInDate,
                        firstDate: DateTime.now().subtract(const Duration(days: 7)),
                        lastDate: DateTime.now().add(const Duration(days: 365)),
                      );
                      if (picked != null) {
                        setDialogState(() {
                          checkInDate = picked;
                          // Nếu ngày trả vô tình nhỏ hơn hoặc bằng ngày nhận mới, tự động đẩy ngày trả lên 1 ngày
                          if (checkOutDate.isBefore(checkInDate) || checkOutDate.isAtSameMomentAs(checkInDate)) {
                            checkOutDate = checkInDate.add(const Duration(days: 1));
                          }
                        });
                      }
                    },
                  ),

                  // 📅 Ô BẤM CHỌN NGÀY TRẢ PHÒNG DỰ KIẾN
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.calendar_today, color: Colors.orange, size: 20),
                    title: const Text("Ngày trả phòng dự kiến", style: TextStyle(fontSize: 11, color: Colors.grey)),
                    subtitle: Text("${checkOutDate.day}/${checkOutDate.month}/${checkOutDate.year}", style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold)),
                    trailing: const Icon(Icons.arrow_drop_down),
                    onTap: () async {
                      final picked = await showDatePicker(
                        context: context,
                        initialDate: checkOutDate,
                        firstDate: checkInDate.add(const Duration(days: 1)), // Ngày trả tối thiểu phải sau ngày nhận 1 ngày
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

                  // 💰 HỘP HIỂN THỊ TỔNG TIỀN PHÒNG ĐỒNG BỘ DỰ KIẾN LIVE
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(color: Colors.amber.withOpacity(0.1), borderRadius: BorderRadius.circular(10)),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text("Đơn giá: ${roomPrice.toStringAsFixed(0)} VNĐ / Đêm", style: const TextStyle(fontSize: 12)),
                        Text("Thời gian: $durationDays Đêm", style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                        const SizedBox(height: 4),
                        Text("💵 TỔNG TIỀN: ${calculatedTotal.toStringAsFixed(0)} VNĐ", style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Colors.red)),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(onPressed: () => Navigator.pop(context), child: const Text("Hủy")),
              ElevatedButton(
                onPressed: () async {
                  if (nameController.text.trim().isEmpty) {
                    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("⚠️ Vui lòng điền tên khách hàng!")));
                    return;
                  }

                  // ĐỊNH DẠNG CHUỖI ĐỂ TRUYỀN BIẾN SẠCH (YYYY-MM-DD) THEO ĐÚNG ĐÒI HỎI CỦA BIẾN MỚI
                  String formattedCheckIn = "${checkInDate.year}-${checkInDate.month.toString().padLeft(2, '0')}-${checkInDate.day.toString().padLeft(2, '0')}";
                  String formattedCheckOut = "${checkOutDate.year}-${checkOutDate.month.toString().padLeft(2, '0')}-${checkOutDate.day.toString().padLeft(2, '0')}";

                  // Hiện vòng xoay Loading gác cổng kết nối
                  showDialog(
                    context: context,
                    barrierDismissible: false,
                    builder: (context) => const Center(child: CircularProgressIndicator()),
                  );

                  try {
                    // 🚀 GỌI API VỚI ĐẦY ĐỦ CÁC THAM SỐ ĐÃ ĐƯỢC ĐỊNH DANH AN TOÀN TRÊN MÃ DART FLUTTER
                    final bool success = await apiProvider.postCheckIn(
                      roomId: room.maPhong,
                      name: nameController.text.trim(),
                      phone: phoneController.text.trim(),
                      price: roomPrice,
                      checkInDate: formattedCheckIn,   // Hết lỗi gạch đỏ
                      checkOutDate: formattedCheckOut, // Hết lỗi gạch đỏ
                      totalPrice: calculatedTotal,     // Hết lỗi gạch đỏ
                    );

                    if (context.mounted) Navigator.pop(context); // Đóng vòng xoay Loading

                    if (success && context.mounted) {
                      Navigator.pop(context); // Đóng Dialog nhập liệu chính

                      // Kiểm tra xem trang quản lý tổng hợp có hàm làm mới tại chỗ hay không
                      // Nếu có hàm _loadAllRoomsData() thì dùng, không thì pop(true) về trang Sơ đồ phòng reload
                      try {
                        _loadAllRoomsData();
                      } catch (_) {
                        Navigator.pop(context, true);
                      }

                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text("Nhận phòng và khởi tạo tiền hóa đơn thành công!"), backgroundColor: Colors.green),
                      );
                    }
                  } catch (e) {
                    if (context.mounted) Navigator.pop(context);
                    debugPrint("Lỗi Check-in: $e");
                  }
                },
                child: const Text("Xác nhận"),
              ),
            ],
          );
        },
      ),
    );
  }

  // --- DIALOG XỬ LÝ CHECK-OUT (BẮT KIỂU DỮ LIỆU TONGTIEN CHUẨN) ---
  void _showSurchargeDialog(BuildContext context, RoomModel room) {
    final surchargeController = TextEditingController(text: "0");

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: Text("Thanh toán: ${room.tenPhong}"),
        content: TextField(
          controller: surchargeController,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(
            labelText: "Phụ thu phát sinh nếu có",
            suffixText: "VNĐ",
            icon: Icon(Icons.money_off),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text("Hủy")),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent),
            onPressed: () async {
              double surcharge = double.tryParse(surchargeController.text) ?? 0;

              showDialog(
                context: context,
                barrierDismissible: false,
                builder: (context) => const Center(child: CircularProgressIndicator()),
              );

              try {
                final response = await apiProvider.postCheckOut(room.maPhong, surcharge);

                if (context.mounted) Navigator.pop(context); // Đóng Loading

                if (response != null && response.statusCode == 200 && context.mounted) {
                  double total = 0.0;
                  if (response.data != null && response.data['tongTien'] != null) {
                    total = double.tryParse(response.data['tongTien'].toString()) ?? 0.0;
                  }

                  Navigator.pop(context); // Đóng Dialog phụ thu
                  _loadAllRoomsData(); // Ép tải lại danh sách tại chỗ để xóa phòng vừa checkout khỏi danh sách lưu trú

                  showDialog(
                    context: context,
                    builder: (context) => AlertDialog(
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
                      title: const Row(
                        children: [
                          Icon(Icons.check_circle_rounded, color: Colors.green),
                          SizedBox(width: 8),
                          Text("Thanh toán thành công", style: TextStyle(fontWeight: FontWeight.bold)),
                        ],
                      ),
                      content: Text("Tổng hóa đơn thanh toán thực tế:\n💰 ${total.toStringAsFixed(0)} VNĐ", style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500)),
                      actions: [
                        TextButton(onPressed: () => Navigator.pop(context), child: const Text("Đóng OK"))
                      ],
                    ),
                  );
                }
              } catch (e) {
                if (context.mounted) Navigator.pop(context);
                debugPrint("Lỗi Check-out: $e");
              }
            },
            child: const Text("Thanh toán", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }
}