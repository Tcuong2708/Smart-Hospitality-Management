import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart'; //
import '../../../core/utils/responsive.dart';

class BookedRoomsScreen extends StatefulWidget {
  const BookedRoomsScreen({super.key});

  @override
  State<BookedRoomsScreen> createState() => _BookedRoomsScreenState();
}

class _BookedRoomsScreenState extends State<BookedRoomsScreen> {
  final HotelApiProvider _apiProvider = HotelApiProvider();
  List<dynamic> _bookedRooms = [];
  bool _isLoading = true;
  String _errorMessage = '';

  @override
  void initState() {
    super.initState();
    _fetchBookingHistory();
  }

  Future<void> _fetchBookingHistory() async {
    if (!mounted) return;
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      const int maKHHardcode = 3; // Đồng bộ Mã khách hàng tương tự bên trang Booking

      final res = await _apiProvider.getTransactionHistory(maKHHardcode);
      if (mounted) {
        setState(() {
          _bookedRooms = res ?? [];
          _isLoading = false;
        });
      }
    } catch (e) {
      debugPrint("❌ Lỗi lấy lịch sử đặt phòng: $e");
      if (mounted) {
        setState(() {
          _errorMessage = "Không thể kết nối máy chủ hoặc Ngrok bị ngắt quãng.";
          _isLoading = false;
        });
      }
    }
  }

  // 🛡️ HÀM BỌC AN TOÀN: Định dạng ngày tháng, chống lỗi RangeError sập App cực tốt
  String _safeFormatDate(dynamic dateStr) {
    if (dateStr == null) return '--/--/----';
    String s = dateStr.toString();
    if (s.length >= 10) {
      return s.substring(0, 10);
    }
    return s;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final bool isDarkMode = theme.brightness == Brightness.dark;

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      // 🎯 ĐỒNG BỘ APP BAR & ICON PIN / ĐỒNG HỒ HỆ THỐNG
      appBar: AppBar(
        title: Text(
            "Phòng đã đặt",
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: Responsive.sp(context, 18), color: colorScheme.onSurface)
        ),
        centerTitle: true,
        elevation: 0.5,
        backgroundColor: colorScheme.surface,
        systemOverlayStyle: SystemUiOverlayStyle(
          statusBarColor: Colors.transparent,
          statusBarIconBrightness: isDarkMode ? Brightness.light : Brightness.dark,
          statusBarBrightness: isDarkMode ? Brightness.dark : Brightness.light,
        ),
      ),
      // Tính năng vuốt màn hình kéo xuống để làm mới dữ liệu Live
      body: RefreshIndicator(
        onRefresh: _fetchBookingHistory,
        color: colorScheme.primary,
        child: _buildBodyContent(colorScheme, isDarkMode),
      ),
    );
  }

  // 🎛️ BỘ ĐIỀU PHỐI WIDGET HIỂN THỊ DỰA TRÊN TRẠNG THÁI DỮ LIỆU
  Widget _buildBodyContent(ColorScheme colorScheme, bool isDarkMode) {
    // 1. Trạng thái đang xoay vòng chờ API nhả dữ liệu
    if (_isLoading) {
      return Center(child: CircularProgressIndicator(color: colorScheme.primary));
    }

    // 2. Trạng thái sập mạng hoặc lỗi Endpoint API
    if (_errorMessage.isNotEmpty) {
      return _buildEmptyOrErrorState(
        icon: Icons.wifi_off_rounded,
        title: _errorMessage,
        buttonText: "Thử lại ngay",
        onPressed: _fetchBookingHistory,
        colorScheme: colorScheme,
      );
    }

    // 3. Trạng thái thông mạch thành công nhưng tài khoản chưa từng đặt phòng nào
    if (_bookedRooms.isEmpty) {
      return _buildEmptyOrErrorState(
        icon: Icons.calendar_month_outlined,
        title: "Bạn chưa có lịch đặt phòng nào tại May Hotel",
        buttonText: "Khám phá phòng ngay",
        // 🎯 ĐA VÁ XỬ LÝ LỖI ĐEN MÀN HÌNH: Navigator Fallback linh hoạt
        onPressed: () {
          if (Navigator.canPop(context)) {
            // Trường hợp mở dạng trang lẻ độc lập từ Profile -> Lùi về an toàn
            Navigator.pop(context);
          } else {
            // Trường hợp màn hình là một TAB cố định thuộc BottomBar -> Không thể pop.
            // Ép hệ thống đẩy vòng ngoài về trang Main chính diện để chọn lại Tab Trang chủ
            Navigator.pushNamedAndRemoveUntil(context, '/main', (route) => false);
          }
        },
        colorScheme: colorScheme,
      );
    }

    // 4. HIỂN THỊ DANH SÁCH PHÒNG LIVE SIÊU SANG TRỌNG V20
    return ListView.builder(
      physics: const AlwaysScrollableScrollPhysics(), // Ép luôn cho cuộn để ăn lệnh RefreshIndicator
      padding: EdgeInsets.all(Responsive.sp(context, 16)),
      itemCount: _bookedRooms.length,
      itemBuilder: (context, index) {
        final item = _bookedRooms[index];
        return _buildBookingCard(item, colorScheme, isDarkMode);
      },
    );
  }

  // 💳 THIẾT KẾ CARD PHÒNG ĐÃ ĐẶT ĐỒNG BỘ STYLE NÂNG CAO
  Widget _buildBookingCard(dynamic item, ColorScheme colorScheme, bool isDarkMode) {
    // Bốc dữ liệu an toàn từ JSON Map của Backend C# ném qua
    String tenPhong = item['tenPhong']?.toString() ?? 'Hạng phòng cao cấp';
    String hinhAnh = item['hinhAnh']?.toString() ?? '';

    // 🛡️ ĐÃ SỬA: Chống gãy Layout/Crash bằng hàm định dạng ngày an toàn
    String ngayNhan = _safeFormatDate(item['ngayNhanPhong']);
    String ngayTra = _safeFormatDate(item['ngayTraPhong']);

    double tongTien = double.tryParse(item['tongTien']?.toString() ?? '0') ?? 0;
    String trangThai = item['trangThai']?.toString() ?? 'Chờ xác nhận';

    // Xử lý bẫy nhân đôi chuỗi assets/images/ lọt từ trang trước sang
    if (hinhAnh.startsWith('assets/images/http')) {
      hinhAnh = hinhAnh.replaceFirst('assets/images/', '');
    }
    final bool isNetworkImage = hinhAnh.startsWith('http');
    String maskedRoomTitle = tenPhong.replaceAll(RegExp(r'P\d+'), 'P***'); // Mặt nạ bảo mật số phòng

    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(15),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(isDarkMode ? 0.3 : 0.05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          )
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            // Khối hiển thị hình ảnh bo góc chống vỡ Layout
            ClipRRect(
              borderRadius: BorderRadius.circular(12),
              child: isNetworkImage
                  ? Image.network(hinhAnh, width: 85, height: 85, fit: BoxFit.cover, errorBuilder: (c, e, s) => _buildImagePlaceholder(colorScheme))
                  : Image.asset(hinhAnh, width: 85, height: 85, fit: BoxFit.cover, errorBuilder: (c, e, s) => _buildImagePlaceholder(colorScheme)),
            ),
            const SizedBox(width: 15),

            // Khu vực bốc trích thông tin chữ
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Text(
                          maskedRoomTitle,
                          style: TextStyle(fontSize: Responsive.sp(context, 15), fontWeight: FontWeight.bold, color: colorScheme.onSurface),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      _buildStatusPill(trangThai, isDarkMode), // Gắn tag trạng thái thông minh
                    ],
                  ),
                  const SizedBox(height: 6),
                  Text(
                    "📅 Lịch: $ngayNhan ➔ $ngayTra",
                    style: TextStyle(fontSize: 12, color: colorScheme.onSurface.withOpacity(0.6), fontWeight: FontWeight.w500),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    "${tongTien.toStringAsFixed(0)} VNĐ", // Định dạng tiền sạch sẽ, né lỗi VND VND
                    style: TextStyle(
                        fontSize: Responsive.sp(context, 15),
                        fontWeight: FontWeight.bold,
                        color: isDarkMode ? Colors.amber : colorScheme.primary
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  // 🏷️ TẠO PILL TAG TRẠNG THÁI TỰ ĐỘNG ĐỔI MÀU THÔNG MINH
  Widget _buildStatusPill(String status, bool isDarkMode) {
    Color cardColor;
    Color textColor;

    if (status.contains('Thành công') || status.contains('Đã xác nhận') || status.contains('True')) {
      cardColor = Colors.green.withOpacity(0.15);
      textColor = isDarkMode ? Colors.greenAccent : Colors.green.shade700;
      status = "Đã duyệt";
    } else {
      cardColor = Colors.orange.withOpacity(0.15);
      textColor = isDarkMode ? Colors.amber : Colors.orange.shade800;
      status = "Chờ xử lý";
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(color: cardColor, borderRadius: BorderRadius.circular(8)),
      child: Text(
        status,
        style: TextStyle(color: textColor, fontSize: 11, fontWeight: FontWeight.bold),
      ),
    );
  }

  Widget _buildImagePlaceholder(ColorScheme colorScheme) {
    return Container(color: colorScheme.surfaceVariant, child: Icon(Icons.hotel, color: colorScheme.onSurfaceVariant));
  }

  // 📭 GIAO DIỆN TRẠNG THÁI TRỐNG / LỖI CHUẨN DESIGN SYSTEM V20
  Widget _buildEmptyOrErrorState({
    required IconData icon,
    required String title,
    required String buttonText,
    required VoidCallback onPressed,
    required ColorScheme colorScheme,
  }) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(30),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 70, color: colorScheme.onSurface.withOpacity(0.2)),
            const SizedBox(height: 20),
            Text(
              title,
              textAlign: TextAlign.center,
              style: TextStyle(color: colorScheme.onSurface.withOpacity(0.5), fontSize: 15, fontWeight: FontWeight.w500, height: 1.4),
            ),
            const SizedBox(height: 25),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: colorScheme.primary,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                padding: const EdgeInsets.symmetric(horizontal: 25, vertical: 12),
                elevation: 2,
              ),
              onPressed: onPressed,
              child: Text(buttonText, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
            )
          ],
        ),
      ),
    );
  }
}