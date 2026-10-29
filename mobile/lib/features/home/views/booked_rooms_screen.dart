import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';
import 'package:may_hotel_app/services/auth_state_service.dart';
import '../../../core/utils/responsive.dart';
import '../../checkin/views/mobile_self_checkin_screen.dart';
import '../../auth/views/login_screen.dart';
import '../../operations/services/mobile_operations_api.dart';
import '../../services/views/service_order_screen.dart';

class BookedRoomsScreen extends StatefulWidget {
  const BookedRoomsScreen({super.key});

  @override
  State<BookedRoomsScreen> createState() => _BookedRoomsScreenState();
}

class _BookedRoomsScreenState extends State<BookedRoomsScreen>
    with SingleTickerProviderStateMixin {
  final HotelApiProvider _apiProvider = HotelApiProvider();
  List<dynamic> _bookedRooms = [];
  bool _isLoading = true;
  String _errorMessage = '';
  String _selectedFilter = 'ALL'; // ALL, ACTIVE, RESERVED, HISTORY

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
      final auth = AuthStateService().currentAuth;
      final userId = auth.userId;
      if (!auth.isLoggedIn || userId == null) {
        if (mounted) {
          setState(() {
            _bookedRooms = [];
            _errorMessage = 'Vui lòng đăng nhập để xem lịch sử đặt phòng.';
            _isLoading = false;
          });
        }
        return;
      }

      final res = await _apiProvider.getTransactionHistory(userId);
      if (mounted) {
        setState(() {
          _bookedRooms = _distinctBookings(res);
          _isLoading = false;
        });
      }
    } catch (e) {
      debugPrint("❌ Lỗi lấy lịch sử đặt phòng: $e");
      if (mounted) {
        setState(() {
          _errorMessage = "Không thể kết nối máy chủ. Vui lòng kiểm tra lại.";
          _isLoading = false;
        });
      }
    }
  }

  List<dynamic> _distinctBookings(List<dynamic> bookings) {
    final distinct = <String, dynamic>{};
    for (var index = 0; index < bookings.length; index++) {
      final item = bookings[index];
      final bookingId = item is Map ? item['bookingId']?.toString() : null;
      final key = bookingId == null || bookingId.isEmpty
          ? 'row-$index'
          : 'booking-$bookingId';
      distinct.putIfAbsent(key, () => item);
    }
    return distinct.values.toList();
  }

  Future<bool> _requireCustomerLogin(String featureName) async {
    if (AuthStateService().currentAuth.isLoggedIn) return true;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Vui lòng đăng nhập để $featureName.')),
    );
    await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const LoginScreen()),
    );
    if (!mounted || !AuthStateService().currentAuth.isLoggedIn) return false;

    await _fetchBookingHistory();
    return mounted;
  }

  // Định dạng ngày tháng an toàn
  String _safeFormatDate(dynamic dateStr) {
    if (dateStr == null) return '--/--/----';
    String s = dateStr.toString();
    if (s.length >= 10) {
      return s.substring(0, 10);
    }
    return s;
  }

  // Lấy danh sách đã lọc
  List<dynamic> get _filteredRooms {
    if (_selectedFilter == 'ACTIVE') {
      return _bookedRooms.where((item) {
        String st = item['trangThai']?.toString() ?? '';
        return _isActiveStayStatus(st);
      }).toList();
    } else if (_selectedFilter == 'RESERVED') {
      return _bookedRooms.where((item) {
        String st = item['trangThai']?.toString() ?? '';
        return _isAwaitingCheckInStatus(st);
      }).toList();
    } else if (_selectedFilter == 'HISTORY') {
      return _bookedRooms.where((item) {
        String st = item['trangThai']?.toString() ?? '';
        return _isHistoricalStatus(st);
      }).toList();
    }
    return _bookedRooms;
  }

  // Tìm booking đang lưu trú active
  dynamic get _activeBooking {
    try {
      return _bookedRooms.firstWhere((item) {
        String st = item['trangThai']?.toString() ?? '';
        return _isActiveStayStatus(st);
      });
    } catch (e) {
      return null;
    }
  }

  bool _isActiveStayStatus(String status) {
    final normalized = status.trim().toLowerCase();
    return const {
      'đã nhận phòng',
      'occupied',
      'in use',
      'đang ở',
      'đang lưu trú',
    }.contains(normalized);
  }

  bool _isAwaitingCheckInStatus(String status) {
    final normalized = status.trim().toLowerCase();
    return normalized == 'reserved' ||
        normalized == 'pending' ||
        normalized == 'confirmed' ||
        normalized.contains('đã xác nhận') ||
        normalized.contains('chờ nhận phòng') ||
        normalized.contains('đã xác thực');
  }

  bool _isCompletedStayStatus(String status) {
    final normalized = status.trim().toLowerCase();
    return const {
      'đã trả phòng',
      'completed',
      'checked-out',
    }.contains(normalized);
  }

  bool _isHistoricalStatus(String status) {
    final normalized = status.trim().toLowerCase();
    return _isCompletedStayStatus(status) ||
        normalized == 'đã hủy' ||
        normalized == 'cancelled' ||
        normalized == 'canceled';
  }

  int get _historyBookingCount => _bookedRooms.where((item) {
    final status = item is Map ? item['trangThai']?.toString() ?? '' : '';
    return _isHistoricalStatus(status);
  }).length;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final bool isDarkMode = theme.brightness == Brightness.dark;

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      appBar: AppBar(
        title: Text(
          "Phòng đã đặt & Danh mục",
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: Responsive.sp(context, 18),
            color: colorScheme.onSurface,
          ),
        ),
        centerTitle: true,
        elevation: 0.5,
        backgroundColor: colorScheme.surface,
        systemOverlayStyle: SystemUiOverlayStyle(
          statusBarColor: Colors.transparent,
          statusBarIconBrightness: isDarkMode
              ? Brightness.light
              : Brightness.dark,
          statusBarBrightness: isDarkMode ? Brightness.dark : Brightness.light,
        ),
      ),
      body: RefreshIndicator(
        onRefresh: _fetchBookingHistory,
        color: colorScheme.primary,
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: EdgeInsets.symmetric(
            horizontal: Responsive.sp(context, 16),
            vertical: 12,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 🎛️ 1. DANH MỤC TIỆN ÍCH DÀNH CHO KHÁCH HÀNG (QUICK CATEGORIES)
              _buildCategoryMenuGrid(colorScheme, isDarkMode),

              const SizedBox(height: 20),

              // 🏷️ 2. BỘ LỌC PHÂN LOẠI (TAB FILTERS)
              _buildFilterChips(colorScheme, isDarkMode),

              const SizedBox(height: 16),

              // 📦 3. NỘI DUNG HIỂN THỊ CHÍNH (LOADING, ERROR, HOẶC DANH SÁCH PHÒNG)
              _buildMainContent(colorScheme, isDarkMode),
            ],
          ),
        ),
      ),
    );
  }

  // 🎛️ 1. LƯỚI DANH MỤC LỰA CHỌN TIỆN ÍCH KHÁCH HÀNG
  Widget _buildCategoryMenuGrid(ColorScheme colorScheme, bool isDarkMode) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: isDarkMode ? colorScheme.surface : Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(isDarkMode ? 0.3 : 0.06),
            blurRadius: 15,
            offset: const Offset(0, 5),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                Icons.dashboard_customize_rounded,
                color: isDarkMode ? Colors.amber : colorScheme.primary,
                size: 20,
              ),
              const SizedBox(width: 8),
              Text(
                "Danh mục tiện ích Khách hàng",
                style: TextStyle(
                  fontSize: Responsive.sp(context, 15),
                  fontWeight: FontWeight.bold,
                  color: colorScheme.onSurface,
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              // DANH MỤC 1: ĐẶT DỊCH VỤ PHÒNG
              Expanded(
                child: _buildCategoryCard(
                  title: "Đặt Dịch Vụ",
                  subtitle: "Ăn uống, giặt ủi, thuê xe...",
                  icon: Icons.room_service_rounded,
                  badgeText: _activeBooking != null ? "Đang ở" : null,
                  badgeColor: Colors.green,
                  cardGradient: [
                    const Color(0xFF0F2942),
                    const Color(0xFF1E3E62),
                  ],
                  iconColor: Colors.amber,
                  onTap: () async {
                    if (!await _requireCustomerLogin('đặt dịch vụ')) return;
                    if (!mounted) return;
                    final active = _activeBooking;
                    if (active != null) {
                      final bookingId =
                          int.tryParse(
                            active['bookingId']?.toString() ?? '0',
                          ) ??
                          0;
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (_) =>
                              ServiceOrderScreen(bookingId: bookingId),
                        ),
                      );
                    } else {
                      _showServicePromptDialog(colorScheme);
                    }
                  },
                ),
              ),
              const SizedBox(width: 12),

              // DANH MỤC 2: XEM LỊCH SỬ ĐẶT PHÒNG
              Expanded(
                child: _buildCategoryCard(
                  title: "Lịch Sử Đặt",
                  subtitle: "Tất cả phiếu & hóa đơn",
                  icon: Icons.history_toggle_off_rounded,
                  badgeText: "$_historyBookingCount đơn",
                  badgeColor: colorScheme.primary,
                  cardGradient: isDarkMode
                      ? [Colors.grey.shade900, Colors.grey.shade800]
                      : [const Color(0xFFF4F6F9), const Color(0xFFE9ECEF)],
                  iconColor: isDarkMode
                      ? Colors.lightBlueAccent
                      : const Color(0xFF0F2942),
                  textColor: isDarkMode
                      ? Colors.white
                      : const Color(0xFF0F2942),
                  subtitleColor: isDarkMode
                      ? Colors.white70
                      : Colors.grey.shade700,
                  onTap: () async {
                    if (!await _requireCustomerLogin('xem lịch sử đặt phòng')) {
                      return;
                    }
                    setState(() {
                      _selectedFilter = 'HISTORY';
                    });
                  },
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              // DANH MỤC 3: TỰ NHẬN PHÒNG (NFC / AI)
              Expanded(
                child: _buildCategoryCard(
                  title: "Nhận Phòng NFC",
                  subtitle: "Quét CCCD & Mở khóa",
                  icon: Icons.nfc_rounded,
                  cardGradient: isDarkMode
                      ? [Colors.teal.shade900, Colors.teal.shade800]
                      : [const Color(0xFFE6F4EA), const Color(0xFFCEEAD6)],
                  iconColor: Colors.teal.shade700,
                  textColor: Colors.teal.shade900,
                  subtitleColor: Colors.teal.shade800,
                  onTap: () async {
                    if (!await _requireCustomerLogin('xác thực nhận phòng')) {
                      return;
                    }
                    if (!mounted) return;
                    final reserved = _bookedRooms.firstWhere(
                      (item) => _isAwaitingCheckInStatus(
                        item['trangThai']?.toString() ?? '',
                      ),
                      orElse: () => null,
                    );
                    if (reserved == null) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text(
                            'Bạn chưa có booking đang chờ nhận phòng.',
                          ),
                        ),
                      );
                      return;
                    }
                    final bId = reserved != null
                        ? (int.tryParse(
                                reserved['bookingId']?.toString() ?? '0',
                              ) ??
                              0)
                        : 0;
                    final rName = reserved != null
                        ? (reserved['loaiPhong']?.toString() ??
                              reserved['roomType']?.toString() ??
                              'Loại phòng của bạn')
                        : 'Loại phòng khách sạn';

                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => MobileSelfCheckinScreen(
                          bookingId: bId,
                          roomName: rName,
                          totalGuests:
                              int.tryParse(
                                reserved?['guestCount']?.toString() ?? '1',
                              ) ??
                              1,
                        ),
                      ),
                    );
                  },
                ),
              ),
              const SizedBox(width: 12),

              // DANH MỤC 4: ĐẶT PHÒNG NGHỈ MỚI
              Expanded(
                child: _buildCategoryCard(
                  title: "Khám Phá Phòng",
                  subtitle: "Đặt phòng sang trọng",
                  icon: Icons.hotel_rounded,
                  cardGradient: isDarkMode
                      ? [Colors.amber.shade900, Colors.amber.shade800]
                      : [const Color(0xFFFFF8E1), const Color(0xFFFFECB3)],
                  iconColor: Colors.amber.shade900,
                  textColor: const Color(0xFF4A3B00),
                  subtitleColor: Colors.amber.shade900,
                  onTap: () {
                    if (Navigator.canPop(context)) {
                      Navigator.pop(context);
                    } else {
                      Navigator.pushNamedAndRemoveUntil(
                        context,
                        '/main',
                        (route) => false,
                      );
                    }
                  },
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  // 💳 ITEM CARD NÚT DANH MỤC ĐẸP MẮT
  Widget _buildCategoryCard({
    required String title,
    required String subtitle,
    required IconData icon,
    String? badgeText,
    Color? badgeColor,
    required List<Color> cardGradient,
    required Color iconColor,
    Color textColor = Colors.white,
    Color subtitleColor = Colors.white70,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        height: 105,
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            colors: cardGradient,
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: cardGradient.first.withOpacity(0.2),
              blurRadius: 8,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Stack(
          children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Container(
                  padding: const EdgeInsets.all(7),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.2),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(icon, color: iconColor, size: 22),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.bold,
                        color: textColor,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: TextStyle(
                        fontSize: 10,
                        color: subtitleColor,
                        fontWeight: FontWeight.w400,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ],
            ),
            if (badgeText != null)
              Positioned(
                top: 0,
                right: 0,
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 6,
                    vertical: 2,
                  ),
                  decoration: BoxDecoration(
                    color: badgeColor ?? Colors.redAccent,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    badgeText,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 9,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  // 🏷️ 2. KHỐI CHỌN LỌC PHÂN LOẠI (CHIPS)
  Widget _buildFilterChips(ColorScheme colorScheme, bool isDarkMode) {
    final filters = [
      {'key': 'ALL', 'label': 'Tất cả (${_bookedRooms.length})'},
      {'key': 'ACTIVE', 'label': 'Đang ở'},
      {'key': 'RESERVED', 'label': 'Chờ nhận'},
      {'key': 'HISTORY', 'label': 'Lịch sử'},
    ];

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: filters.map((f) {
          final isSelected = _selectedFilter == f['key'];
          return Padding(
            padding: const EdgeInsets.only(right: 8),
            child: ChoiceChip(
              label: Text(
                f['label']!,
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                  color: isSelected
                      ? (isDarkMode ? Colors.black : Colors.white)
                      : colorScheme.onSurface.withOpacity(0.7),
                ),
              ),
              selected: isSelected,
              selectedColor: isDarkMode ? Colors.amber : colorScheme.primary,
              backgroundColor: isDarkMode
                  ? colorScheme.surface
                  : Colors.grey.shade200,
              onSelected: (selected) async {
                if (selected) {
                  if (!await _requireCustomerLogin('xem lịch sử đặt phòng')) {
                    return;
                  }
                  setState(() {
                    _selectedFilter = f['key']!;
                  });
                }
              },
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(20),
              ),
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
            ),
          );
        }).toList(),
      ),
    );
  }

  // 📦 3. BỘ ĐIỀU PHỐI NỘI DUNG CHÍNH
  Widget _buildMainContent(ColorScheme colorScheme, bool isDarkMode) {
    if (_isLoading) {
      return Container(
        height: 200,
        alignment: Alignment.center,
        child: CircularProgressIndicator(color: colorScheme.primary),
      );
    }

    if (_errorMessage.isNotEmpty) {
      final isGuest = !AuthStateService().currentAuth.isLoggedIn;
      return _buildEmptyOrErrorState(
        icon: isGuest ? Icons.lock_outline_rounded : Icons.wifi_off_rounded,
        title: _errorMessage,
        buttonText: isGuest ? "Đăng nhập ngay" : "Thử lại ngay",
        onPressed: isGuest
            ? () async {
                await _requireCustomerLogin('xem lịch sử đặt phòng');
              }
            : _fetchBookingHistory,
        colorScheme: colorScheme,
      );
    }

    final roomsToDisplay = _filteredRooms;

    if (roomsToDisplay.isEmpty) {
      return _buildEmptyOrErrorState(
        icon: Icons.calendar_month_outlined,
        title: _selectedFilter == 'ALL'
            ? "Bạn chưa có lịch đặt phòng nào tại May Hotel"
            : "Không có đơn đặt phòng nào trong danh mục này",
        buttonText: "Khám phá phòng ngay",
        onPressed: () {
          if (Navigator.canPop(context)) {
            Navigator.pop(context);
          } else {
            Navigator.pushNamedAndRemoveUntil(
              context,
              '/main',
              (route) => false,
            );
          }
        },
        colorScheme: colorScheme,
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: Text(
            "Danh sách đơn đặt phòng (${roomsToDisplay.length})",
            style: TextStyle(
              fontSize: Responsive.sp(context, 14),
              fontWeight: FontWeight.bold,
              color: colorScheme.onSurface.withOpacity(0.8),
            ),
          ),
        ),
        ListView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: roomsToDisplay.length,
          itemBuilder: (context, index) {
            final item = roomsToDisplay[index];
            return _buildBookingCard(item, colorScheme, isDarkMode);
          },
        ),
      ],
    );
  }

  // 💳 THIẾT KẾ CARD PHÒNG ĐÃ ĐẶT
  Widget _buildBookingCard(
    dynamic item,
    ColorScheme colorScheme,
    bool isDarkMode,
  ) {
    String tenPhong = item['loaiPhong']?.toString().trim().isNotEmpty == true
        ? item['loaiPhong'].toString()
        : item['roomType']?.toString().trim().isNotEmpty == true
        ? item['roomType'].toString()
        : 'Loại phòng đang chờ xếp';
    String hinhAnh = item['hinhAnh']?.toString() ?? '';

    String ngayNhan = _safeFormatDate(
      item['ngayNhanPhong'] ?? item['ngayNhan'],
    );
    String ngayTra = _safeFormatDate(item['ngayTraPhong'] ?? item['ngayTra']);

    double tongTien = double.tryParse(item['tongTien']?.toString() ?? '0') ?? 0;
    String trangThai = item['trangThai']?.toString() ?? 'Chờ xác nhận';
    final bool daThanhToan = item['daThanhToan'] == true;
    final bool isFaceVerified = item['isFaceVerified'] == true;
    if (isFaceVerified && !_isActiveStayStatus(trangThai)) {
      trangThai = 'Đã xác thực - chờ lễ tân gán phòng';
    }

    if (hinhAnh.startsWith('assets/images/http')) {
      hinhAnh = hinhAnh.replaceFirst('assets/images/', '');
    }
    final bool isNetworkImage = hinhAnh.startsWith('http');
    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: _isActiveStayStatus(trangThai)
              ? Colors.blue.withOpacity(0.5)
              : Colors.transparent,
          width: 1.5,
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(isDarkMode ? 0.3 : 0.05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(12),
              child: isNetworkImage
                  ? Image.network(
                      hinhAnh,
                      width: 85,
                      height: 85,
                      fit: BoxFit.cover,
                      errorBuilder: (c, e, s) =>
                          _buildImagePlaceholder(colorScheme),
                    )
                  : Image.asset(
                      hinhAnh,
                      width: 85,
                      height: 85,
                      fit: BoxFit.cover,
                      errorBuilder: (c, e, s) =>
                          _buildImagePlaceholder(colorScheme),
                    ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Text(
                          tenPhong,
                          style: TextStyle(
                            fontSize: Responsive.sp(context, 15),
                            fontWeight: FontWeight.bold,
                            color: colorScheme.onSurface,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      _buildStatusPill(trangThai, isDarkMode),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Text(
                    "📅 Lịch: $ngayNhan ➔ $ngayTra",
                    style: TextStyle(
                      fontSize: 12,
                      color: colorScheme.onSurface.withOpacity(0.6),
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 5),
                  InkWell(
                    onTap: () => _showInvoiceDetails(item),
                    borderRadius: BorderRadius.circular(6),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 2),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            Icons.receipt_long_outlined,
                            size: 15,
                            color: daThanhToan ? Colors.green : Colors.orange,
                          ),
                          const SizedBox(width: 4),
                          Text(
                            daThanhToan
                                ? 'Đã thanh toán · Xem hóa đơn'
                                : 'Chưa thanh toán · Xem chi tiết',
                            style: TextStyle(
                              fontSize: 11,
                              fontWeight: FontWeight.w600,
                              color: daThanhToan
                                  ? Colors.green
                                  : Colors.orange.shade800,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        "${tongTien.toStringAsFixed(0)} VNĐ",
                        style: TextStyle(
                          fontSize: Responsive.sp(context, 14),
                          fontWeight: FontWeight.bold,
                          color: isDarkMode
                              ? Colors.amber
                              : colorScheme.primary,
                        ),
                      ),
                      _buildActionButtons(item, trangThai, colorScheme),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  double _number(dynamic value) =>
      double.tryParse(value?.toString() ?? '0') ?? 0;

  String _money(dynamic value) => '${_number(value).toStringAsFixed(0)} VNĐ';

  Future<void> _showInvoiceDetails(dynamic rawItem) async {
    final item = Map<String, dynamic>.from(rawItem as Map);
    final services = item['dichVu'] is List
        ? List<dynamic>.from(item['dichVu'] as List)
        : <dynamic>[];
    final paid = item['daThanhToan'] == true;

    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (sheetContext) => SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                item['invoiceId'] == null
                    ? 'Chi tiết thanh toán'
                    : 'Hóa đơn #${item['invoiceId']}',
                style: Theme.of(
                  sheetContext,
                ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 12),
              _invoiceRow('Tiền phòng', _money(item['tienPhong'])),
              _invoiceRow('Tiền dịch vụ', _money(item['tienDichVu'])),
              _invoiceRow('Phụ thu', _money(item['phuThu'])),
              _invoiceRow('Giảm giá', _money(item['giamGia'])),
              const Divider(height: 22),
              _invoiceRow(
                'Tổng thanh toán',
                _money(item['tongTien']),
                emphasized: true,
              ),
              _invoiceRow(
                'Trạng thái',
                paid ? 'Đã thanh toán' : 'Chưa thanh toán',
              ),
              if ((item['phuongThucThanhToan']?.toString() ?? '').isNotEmpty)
                _invoiceRow(
                  'Phương thức',
                  item['phuongThucThanhToan'].toString(),
                ),
              if (services.isNotEmpty) ...[
                const SizedBox(height: 14),
                Text(
                  'Dịch vụ đã đặt',
                  style: Theme.of(sheetContext).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 8),
                for (final rawService in services)
                  if (rawService is Map)
                    _invoiceRow(
                      '${rawService['tenDichVu'] ?? 'Dịch vụ'} × ${rawService['soLuong'] ?? 1}',
                      _money(rawService['thanhTien']),
                    ),
              ],
              const SizedBox(height: 16),
              const Text(
                'Hóa đơn nhiệt do quầy dịch vụ hoặc lễ tân thực hiện in.',
                style: TextStyle(fontSize: 12, color: Colors.grey),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _invoiceRow(String label, String value, {bool emphasized = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(child: Text(label)),
          const SizedBox(width: 12),
          Text(
            value,
            textAlign: TextAlign.right,
            style: TextStyle(
              fontWeight: emphasized ? FontWeight.bold : FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }

  // 🎛️ KHỐI NÚT THAO TÁC THEO TRẠNG THÁI
  Widget _buildActionButtons(
    dynamic item,
    String trangThai,
    ColorScheme colorScheme,
  ) {
    final int bookingId =
        int.tryParse(item['bookingId']?.toString() ?? '0') ?? 0;

    if (trangThai.contains("Đã xác nhận") ||
        trangThai == "Reserved" ||
        trangThai == "Pending" ||
        trangThai == "Chờ nhận phòng" ||
        trangThai == "Đã xác thực - chờ lễ tân gán phòng") {
      return Wrap(
        spacing: 6,
        runSpacing: 4,
        alignment: WrapAlignment.end,
        children: [
          OutlinedButton(
            style: OutlinedButton.styleFrom(
              padding: const EdgeInsets.symmetric(horizontal: 10),
              minimumSize: const Size(0, 32),
              side: const BorderSide(color: Colors.redAccent),
            ),
            onPressed: () => _confirmCancellation(item),
            child: const Text(
              'Hủy phòng',
              style: TextStyle(fontSize: 11, color: Colors.redAccent),
            ),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: colorScheme.primary,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(horizontal: 12),
              minimumSize: const Size(0, 32),
            ),
            onPressed: () async {
              final result = await Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => MobileSelfCheckinScreen(
                    bookingId: bookingId,
                    roomName:
                        item['loaiPhong']?.toString() ??
                        item['roomType']?.toString() ??
                        'Loại phòng của bạn',
                    totalGuests:
                        int.tryParse(
                          item['requiredIdentityCount']?.toString() ??
                              item['adultCount']?.toString() ??
                              item['guestCount']?.toString() ??
                              '1',
                        ) ??
                        1,
                  ),
                ),
              );
              if (result == true) _fetchBookingHistory();
            },
            child: const Text(
              'Nhận phòng',
              style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold),
            ),
          ),
        ],
      );
    } else if (_isActiveStayStatus(trangThai)) {
      return Wrap(
        spacing: 6,
        runSpacing: 4,
        alignment: WrapAlignment.end,
        children: [
          ElevatedButton.icon(
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.amber.shade700,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(horizontal: 10),
              minimumSize: const Size(0, 32),
            ),
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(
                builder: (_) => ServiceOrderScreen(bookingId: bookingId),
              ),
            ),
            icon: const Icon(Icons.room_service, size: 14),
            label: const Text(
              'Dịch vụ',
              style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold),
            ),
          ),
          OutlinedButton.icon(
            style: OutlinedButton.styleFrom(minimumSize: const Size(0, 32)),
            onPressed: () => _showReviewDialog(item),
            icon: const Icon(Icons.star_outline, size: 16),
            label: const Text('Đánh giá', style: TextStyle(fontSize: 11)),
          ),
        ],
      );
    } else if (_isCompletedStayStatus(trangThai)) {
      return OutlinedButton.icon(
        style: OutlinedButton.styleFrom(minimumSize: const Size(0, 32)),
        onPressed: () => _showReviewDialog(item),
        icon: const Icon(Icons.star_outline, size: 16),
        label: const Text('Đánh giá', style: TextStyle(fontSize: 11)),
      );
    }
    return const SizedBox.shrink();
  }

  Future<void> _confirmCancellation(dynamic item) async {
    final bookingId = int.tryParse(item['bookingId']?.toString() ?? '') ?? 0;
    try {
      final preview = await MobileOperationsApi().previewCancellation(
        bookingId,
      );
      if (!mounted) return;
      final penalty = (preview['penaltyRate'] as num?)?.toDouble() ?? 0;
      final reason = TextEditingController();
      final confirmed = await showDialog<bool>(
        context: context,
        builder: (dialogContext) => AlertDialog(
          title: const Text('Xác nhận hủy đặt phòng'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Mức phí hủy áp dụng: ${penalty.toStringAsFixed(0)}%.'),
              const SizedBox(height: 12),
              TextField(
                controller: reason,
                maxLength: 300,
                decoration: const InputDecoration(
                  labelText: 'Lý do hủy (không bắt buộc)',
                  border: OutlineInputBorder(),
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              child: const Text('Quay lại'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(dialogContext, true),
              child: const Text('Xác nhận hủy'),
            ),
          ],
        ),
      );
      if (confirmed != true) return;
      await MobileOperationsApi().cancelBooking(
        bookingId,
        reason: reason.text.trim(),
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Hủy đặt phòng thành công.')),
      );
      _fetchBookingHistory();
    } catch (error) {
      if (!mounted) return;
      _showOperationError(error);
    }
  }

  Future<void> _showReviewDialog(dynamic item) async {
    final bookingId = int.tryParse(item['bookingId']?.toString() ?? '') ?? 0;
    var rating = 5;
    final content = TextEditingController();
    final submitted = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('Đánh giá May Hotel'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                '${item['loaiPhong'] ?? item['roomType'] ?? 'Loại phòng lưu trú'} · Kỳ lưu trú #$bookingId',
                textAlign: TextAlign.center,
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(
                  5,
                  (index) => IconButton(
                    onPressed: () => setDialogState(() => rating = index + 1),
                    icon: Icon(
                      index < rating ? Icons.star : Icons.star_border,
                      color: Colors.amber.shade700,
                    ),
                  ),
                ),
              ),
              TextField(
                controller: content,
                maxLength: 1000,
                maxLines: 3,
                decoration: const InputDecoration(
                  labelText: 'Nhận xét (không bắt buộc)',
                  border: OutlineInputBorder(),
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              child: const Text('Đóng'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(dialogContext, true),
              child: const Text('Gửi đánh giá'),
            ),
          ],
        ),
      ),
    );
    if (submitted != true) return;
    try {
      await MobileOperationsApi().submitReview(
        bookingId,
        rating: rating,
        content: content.text.trim(),
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Cảm ơn bạn đã gửi đánh giá.')),
      );
    } catch (error) {
      if (!mounted) return;
      _showOperationError(error);
    }
  }

  void _showOperationError(Object error) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Không thể thực hiện: $error'),
        backgroundColor: Colors.redAccent,
      ),
    );
  }

  // Dialog thông báo khi không có phòng active để đặt dịch vụ
  void _showServicePromptDialog(ColorScheme colorScheme) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Row(
          children: [
            Icon(Icons.info_outline, color: colorScheme.primary),
            const SizedBox(width: 8),
            const Text("Thông báo dịch vụ"),
          ],
        ),
        content: const Text(
          "Dịch vụ phòng (Ăn uống, thuê xe, giặt ủi...) chỉ dành cho Khách hàng đang có phòng lưu trú (Đã Check-in).\n\n"
          "Nếu bạn đã có phòng, vui lòng chọn nút 'Nhận phòng' ở danh sách bên dưới.",
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text("Đóng"),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(ctx);
              if (Navigator.canPop(context)) {
                Navigator.pop(context);
              } else {
                Navigator.pushNamedAndRemoveUntil(
                  context,
                  '/main',
                  (route) => false,
                );
              }
            },
            child: const Text("Khám phá phòng ngay"),
          ),
        ],
      ),
    );
  }

  Widget _buildStatusPill(String status, bool isDarkMode) {
    Color cardColor;
    Color textColor;
    String displayStatus = status;

    if (_isActiveStayStatus(status)) {
      cardColor = Colors.blue.withOpacity(0.15);
      textColor = isDarkMode ? Colors.lightBlueAccent : Colors.blue.shade700;
      displayStatus = "Đang ở";
    } else if (status == 'Đã trả phòng' || status == 'Completed') {
      cardColor = Colors.grey.withOpacity(0.2);
      textColor = isDarkMode ? Colors.grey.shade300 : Colors.grey.shade700;
      displayStatus = "Đã trả phòng";
    } else if (status.contains('Thành công') ||
        status.contains('Đã xác nhận') ||
        status.contains('Reserved') ||
        status.contains('Pending') ||
        status.contains('Chờ nhận phòng') ||
        status.contains('Đã xác thực') ||
        status.contains('True')) {
      cardColor = Colors.green.withOpacity(0.15);
      textColor = isDarkMode ? Colors.greenAccent : Colors.green.shade700;
      displayStatus = "Chờ nhận phòng";
    } else {
      cardColor = Colors.orange.withOpacity(0.15);
      textColor = isDarkMode ? Colors.amber : Colors.orange.shade800;
      displayStatus = "Chờ xử lý";
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: cardColor,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        displayStatus,
        style: TextStyle(
          color: textColor,
          fontSize: 11,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  Widget _buildImagePlaceholder(ColorScheme colorScheme) {
    return Container(
      color: Colors.grey.shade200,
      child: Icon(Icons.hotel, color: colorScheme.primary),
    );
  }

  Widget _buildEmptyOrErrorState({
    required IconData icon,
    required String title,
    required String buttonText,
    required VoidCallback onPressed,
    required ColorScheme colorScheme,
  }) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 30, horizontal: 20),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 60, color: colorScheme.onSurface.withOpacity(0.2)),
            const SizedBox(height: 16),
            Text(
              title,
              textAlign: TextAlign.center,
              style: TextStyle(
                color: colorScheme.onSurface.withOpacity(0.5),
                fontSize: 14,
                fontWeight: FontWeight.w500,
                height: 1.4,
              ),
            ),
            const SizedBox(height: 20),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: colorScheme.primary,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                padding: const EdgeInsets.symmetric(
                  horizontal: 24,
                  vertical: 12,
                ),
                elevation: 2,
              ),
              onPressed: onPressed,
              child: Text(
                buttonText,
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
