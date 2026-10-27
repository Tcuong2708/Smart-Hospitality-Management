import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:may_hotel_app/main.dart';
import '../../../core/constants/app_colors.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';
import 'package:may_hotel_app/models/room_model.dart';
import 'package:may_hotel_app/services/auth_state_service.dart';
import 'package:may_hotel_app/services/language_service.dart';
import 'package:may_hotel_app/services/room_search_cache.dart';
import '../../auth/views/login_screen.dart';
import '../widgets/room_card.dart';
import 'booking_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final HotelApiProvider _apiProvider = HotelApiProvider();
  late Future<List<RoomModel>> _roomsFuture;
  late DateTimeRange _selectedDateRange;
  int _adults = 2;
  int _children = 0;

  String _currentUserName = "Guest";
  String _searchQuery = "";
  int _selectedCategoryId = 0; // 0 = All categories
  double _maxPriceFilter = 10000000; // Default max 10,000,000 VND

  @override
  void initState() {
    super.initState();
    final today = DateUtils.dateOnly(DateTime.now());
    _selectedDateRange = DateTimeRange(
      start: today,
      end: today.add(const Duration(days: 1)),
    );
    _handleRefresh();
    _loadUserData();
  }

  void _loadUserData() {
    AuthStateService().addListener(_onAuthStateChanged);
    _onAuthStateChanged();
  }

  void _onAuthStateChanged() {
    if (mounted) {
      setState(() {
        _currentUserName = AuthStateService().currentAuth.username;
      });
    }
  }

  @override
  void dispose() {
    AuthStateService().removeListener(_onAuthStateChanged);
    super.dispose();
  }

  String _getRealTimeGreeting() {
    final lang = LanguageService();
    final int hour = DateTime.now().hour;
    if (hour >= 5 && hour < 12) {
      return lang.translate('morning');
    } else if (hour >= 12 && hour < 18) {
      return lang.translate('afternoon');
    } else {
      return lang.translate('evening');
    }
  }

  Future<void> _handleRefresh() async {
    RoomSearchCache.instance.invalidate();
    final request = _loadAvailableRoomTypes();
    setState(() {
      _roomsFuture = request;
    });
    await request;
  }

  Future<List<RoomModel>> _loadAvailableRoomTypes() async {
    final results = await _apiProvider.searchRooms(
      _selectedDateRange.start,
      _selectedDateRange.end,
      _adults,
      _children,
    );
    return results
        .whereType<Map>()
        .map((item) => RoomModel.fromJson(Map<String, dynamic>.from(item)))
        .toList();
  }

  Future<void> _searchAvailableRooms() async {
    setState(() {
      _selectedCategoryId = 0;
      _roomsFuture = _loadAvailableRoomTypes();
    });
    await _roomsFuture;
  }

  void _showPriceFilterDialog() {
    final lang = LanguageService();
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setModalState) {
            return Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    lang.currentLanguage == AppLanguage.vi
                        ? "Lọc theo giá"
                        : "Filter by Price",
                    style: const TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 20),
                  Text(
                    lang.currentLanguage == AppLanguage.vi
                        ? "Mức giá tối đa: ${(_maxPriceFilter).toStringAsFixed(0)} VND"
                        : "Max Price: ${(_maxPriceFilter).toStringAsFixed(0)} VND",
                    style: const TextStyle(fontSize: 16),
                  ),
                  Slider(
                    value: _maxPriceFilter,
                    min: 500000,
                    max: 10000000,
                    divisions: 19,
                    label: _maxPriceFilter.round().toString(),
                    onChanged: (value) {
                      setModalState(() {
                        _maxPriceFilter = value;
                      });
                      setState(() {
                        _maxPriceFilter = value;
                      });
                    },
                  ),
                  const SizedBox(height: 10),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton(
                      onPressed: () => Navigator.pop(context),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.primary,
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10),
                        ),
                      ),
                      child: Text(
                        lang.currentLanguage == AppLanguage.vi
                            ? "Áp dụng"
                            : "Apply",
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 16,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  void _navigateToBooking(
    BuildContext context,
    String title,
    String price,
    String image,
    int id,
    int maxOccupancy,
  ) {
    final authState = AuthStateService().currentAuth;
    final lang = LanguageService();

    if (!authState.isLoggedIn) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(lang.translate('booking_required'))),
      );
      Navigator.push(
        context,
        MaterialPageRoute(builder: (context) => const LoginScreen()),
      );
      return;
    }

    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => BookingScreen(
          roomTitle: title,
          roomPrice: price,
          imageUrl: image,
          maPhong: id,
          initialDateRange: _selectedDateRange,
          initialGuestCount: _adults + _children,
          maxGuestCount: maxOccupancy,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final Color textColor = theme.colorScheme.onSurface;
    final Color surfaceColor = theme.colorScheme.surface;
    final lang = LanguageService();

    return ValueListenableBuilder<ThemeMode>(
      valueListenable: themeNotifier,
      builder: (context, currentMode, _) {
        final bool isDarkMode = currentMode == ThemeMode.dark;

        return AnnotatedRegion<SystemUiOverlayStyle>(
          value: SystemUiOverlayStyle(
            statusBarColor: Colors.transparent,
            statusBarIconBrightness: isDarkMode
                ? Brightness.light
                : Brightness.dark,
            statusBarBrightness: isDarkMode
                ? Brightness.dark
                : Brightness.light,
            systemNavigationBarColor: Colors.transparent,
            systemNavigationBarIconBrightness: isDarkMode
                ? Brightness.light
                : Brightness.dark,
            systemNavigationBarContrastEnforced: false,
          ),
          child: ListenableBuilder(
            listenable: lang,
            builder: (context, _) => Scaffold(
              backgroundColor: theme.scaffoldBackgroundColor,
              body: SafeArea(
                child: RefreshIndicator(
                  onRefresh: _handleRefresh,
                  child: SingleChildScrollView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const SizedBox(height: 10),
                        _buildHeader(context, textColor, surfaceColor),
                        const SizedBox(height: 25),
                        Text(
                          "May Hotel",
                          style: TextStyle(
                            fontSize: 28,
                            fontWeight: FontWeight.bold,
                            color: textColor,
                          ),
                        ),
                        const SizedBox(height: 15),
                        _buildSearchBar(context),
                        const SizedBox(height: 16),
                        _buildBookingSearchPanel(context),
                        const SizedBox(height: 25),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              lang.translate('popular_hotel'),
                              style: TextStyle(
                                fontSize: 20,
                                fontWeight: FontWeight.bold,
                                color: textColor,
                              ),
                            ),
                            Text(
                              lang.translate('view_all'),
                              style: const TextStyle(color: AppColors.textSub),
                            ),
                          ],
                        ),
                        const SizedBox(height: 15),
                        FutureBuilder<List<RoomModel>>(
                          future: _roomsFuture,
                          builder: (context, snapshot) {
                            if (snapshot.connectionState ==
                                ConnectionState.waiting) {
                              return const Padding(
                                padding: EdgeInsets.symmetric(vertical: 40),
                                child: Center(
                                  child: CircularProgressIndicator(),
                                ),
                              );
                            }
                            if (snapshot.hasError) {
                              return Padding(
                                padding: const EdgeInsets.symmetric(
                                  vertical: 20,
                                ),
                                child: Center(
                                  child: Text(
                                    lang.currentLanguage == AppLanguage.vi
                                        ? "Không thể kết nối API Server.\nVui lòng kiểm tra kết nối!"
                                        : "Cannot connect to API Server.\nPlease check connection!",
                                    style: TextStyle(
                                      color: Colors.red.shade600,
                                      fontWeight: FontWeight.w500,
                                    ),
                                    textAlign: TextAlign.center,
                                  ),
                                ),
                              );
                            }
                            final roomsList = snapshot.data ?? [];
                            if (roomsList.isEmpty) {
                              return Padding(
                                padding: const EdgeInsets.symmetric(
                                  vertical: 40,
                                ),
                                child: Center(
                                  child: Text(
                                    lang.currentLanguage == AppLanguage.vi
                                        ? "Không có hạng phòng phù hợp trong thời gian và số khách đã chọn."
                                        : "No room type matches the selected dates and guests.",
                                    textAlign: TextAlign.center,
                                  ),
                                ),
                              );
                            }

                            // Render category tabs dynamically based on available rooms
                            Widget categoryTabs = SingleChildScrollView(
                              scrollDirection: Axis.horizontal,
                              child: Row(
                                children: [
                                  // 'All' category
                                  GestureDetector(
                                    onTap: () =>
                                        setState(() => _selectedCategoryId = 0),
                                    child: Container(
                                      margin: const EdgeInsets.only(right: 10),
                                      padding: const EdgeInsets.symmetric(
                                        horizontal: 20,
                                        vertical: 10,
                                      ),
                                      decoration: BoxDecoration(
                                        color: _selectedCategoryId == 0
                                            ? AppColors.primary
                                            : surfaceColor,
                                        borderRadius: BorderRadius.circular(20),
                                      ),
                                      child: Text(
                                        lang.currentLanguage == AppLanguage.vi
                                            ? "Tất cả"
                                            : "All",
                                        style: TextStyle(
                                          color: _selectedCategoryId == 0
                                              ? Colors.white
                                              : textColor,
                                          fontWeight: _selectedCategoryId == 0
                                              ? FontWeight.bold
                                              : FontWeight.normal,
                                        ),
                                      ),
                                    ),
                                  ),
                                  ...roomsList.map((room) {
                                    bool isSelected =
                                        _selectedCategoryId == room.maPhong;
                                    return GestureDetector(
                                      onTap: () => setState(
                                        () =>
                                            _selectedCategoryId = room.maPhong,
                                      ),
                                      child: Container(
                                        margin: const EdgeInsets.only(
                                          right: 10,
                                        ),
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 20,
                                          vertical: 10,
                                        ),
                                        decoration: BoxDecoration(
                                          color: isSelected
                                              ? AppColors.primary
                                              : surfaceColor,
                                          borderRadius: BorderRadius.circular(
                                            20,
                                          ),
                                        ),
                                        child: Text(
                                          room.tenPhong,
                                          style: TextStyle(
                                            color: isSelected
                                                ? Colors.white
                                                : textColor,
                                            fontWeight: isSelected
                                                ? FontWeight.bold
                                                : FontWeight.normal,
                                          ),
                                        ),
                                      ),
                                    );
                                  }),
                                ],
                              ),
                            );

                            List<RoomModel> filteredRoomsList = roomsList.where(
                              (room) {
                                final String roomName = room.tenPhong
                                    .toUpperCase();
                                bool matchesSearch = roomName.contains(
                                  _searchQuery.toUpperCase(),
                                );
                                bool matchesCategory =
                                    (_selectedCategoryId == 0) ||
                                    (room.maPhong == _selectedCategoryId);
                                bool matchesPrice =
                                    room.price <= _maxPriceFilter;
                                return matchesSearch &&
                                    matchesCategory &&
                                    matchesPrice;
                              },
                            ).toList();

                            Widget roomsDisplay;
                            if (filteredRoomsList.isEmpty) {
                              roomsDisplay = Padding(
                                padding: const EdgeInsets.symmetric(
                                  vertical: 30,
                                ),
                                child: Center(
                                  child: Text(
                                    lang.currentLanguage == AppLanguage.vi
                                        ? "Không tìm thấy kết quả phòng phù hợp."
                                        : "No matching rooms found.",
                                  ),
                                ),
                              );
                            } else {
                              roomsDisplay = Column(
                                children: filteredRoomsList.map((room) {
                                  final bool isAvailable =
                                      room.maTrangThai == 1 &&
                                      room.availableCount > 0;

                                  return RoomCard(
                                    title: room.tenPhong,
                                    location: isAvailable
                                        ? "${lang.translate('available_rooms')} ${room.availableCount} ${lang.translate('ready')}"
                                        : lang.translate('out_of_rooms'),
                                    price: room.price.toStringAsFixed(0),
                                    rating: "5.0",
                                    imageUrl: room.imageUrl,
                                    onAddTap: () {
                                      if (!isAvailable) {
                                        ScaffoldMessenger.of(
                                          context,
                                        ).showSnackBar(
                                          SnackBar(
                                            content: Text(
                                              lang.currentLanguage ==
                                                      AppLanguage.vi
                                                  ? "Hạng phòng ${room.tenPhong} hiện tại đã hết phòng trống!"
                                                  : "${room.tenPhong} is currently out of rooms!",
                                            ),
                                            backgroundColor: Colors.red,
                                          ),
                                        );
                                        return;
                                      }
                                      _navigateToBooking(
                                        context,
                                        "May Luxury ${room.tenPhong}",
                                        room.price.toStringAsFixed(0),
                                        room.imageUrl,
                                        room.maPhong, // This is actually room type ID
                                        room.maxOccupancy,
                                      );
                                    },
                                  );
                                }).toList(),
                              );
                            }

                            return Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                categoryTabs,
                                const SizedBox(height: 25),
                                roomsDisplay,
                              ],
                            );
                          },
                        ),
                        const SizedBox(height: 20),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildHeader(
    BuildContext context,
    Color textColor,
    Color surfaceColor,
  ) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Row(
          children: [
            CircleAvatar(
              backgroundColor: Colors.grey.shade300,
              radius: 22,
              child: ClipOval(
                child: Image.asset(
                  'assets/images/d2.jpg',
                  fit: BoxFit.cover,
                  width: 44,
                  height: 44,
                  errorBuilder: (context, error, stackTrace) => Icon(
                    Icons.person,
                    color: textColor.withValues(alpha: 0.6),
                  ),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  _getRealTimeGreeting(),
                  style: const TextStyle(
                    color: AppColors.textSub,
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  _currentUserName,
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 16,
                    color: textColor,
                  ),
                ),
              ],
            ),
          ],
        ),
        IconButton(
          onPressed: () {},
          icon: Icon(
            Icons.notifications_none_outlined,
            size: 28,
            color: textColor,
          ),
          style: IconButton.styleFrom(
            backgroundColor: surfaceColor,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildSearchBar(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final lang = LanguageService();
    return Row(
      children: [
        Expanded(
          child: Container(
            decoration: BoxDecoration(
              color: colorScheme.surface,
              borderRadius: BorderRadius.circular(15),
              boxShadow: [
                if (theme.brightness == Brightness.light)
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.03),
                    blurRadius: 10,
                    offset: const Offset(0, 4),
                  ),
              ],
            ),
            child: TextField(
              style: TextStyle(color: colorScheme.onSurface),
              onChanged: (value) => setState(() => _searchQuery = value.trim()),
              decoration: InputDecoration(
                hintText: lang.translate('search_hint'),
                hintStyle: TextStyle(
                  color: colorScheme.onSurface.withValues(alpha: 0.5),
                ),
                prefixIcon: Icon(
                  Icons.search,
                  color: colorScheme.onSurface.withValues(alpha: 0.6),
                ),
                border: InputBorder.none,
                contentPadding: const EdgeInsets.symmetric(vertical: 12),
              ),
            ),
          ),
        ),
        const SizedBox(width: 12),
        GestureDetector(
          onTap: _showPriceFilterDialog,
          child: Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: colorScheme.primary,
              borderRadius: BorderRadius.circular(15),
            ),
            child: const Icon(Icons.tune, color: Colors.white),
          ),
        ),
      ],
    );
  }

  Widget _buildBookingSearchPanel(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final isDark = theme.brightness == Brightness.dark;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: colorScheme.outlineVariant),
        boxShadow: [
          if (!isDark)
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.04),
              blurRadius: 12,
              offset: const Offset(0, 4),
            ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Tìm phòng theo kỳ lưu trú',
            style: TextStyle(
              color: colorScheme.onSurface,
              fontSize: 16,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 12),
          InkWell(
            borderRadius: BorderRadius.circular(14),
            onTap: _pickBookingDateRange,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
              decoration: BoxDecoration(
                color: isDark ? Colors.grey.shade900 : Colors.grey.shade100,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Row(
                children: [
                  Icon(
                    Icons.calendar_month_outlined,
                    color: colorScheme.primary,
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      '${_formatDate(_selectedDateRange.start)}  →  ${_formatDate(_selectedDateRange.end)}',
                      style: TextStyle(
                        color: colorScheme.onSurface,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                  const Icon(Icons.keyboard_arrow_down_rounded),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _buildGuestCounter(
                  label: 'Người lớn',
                  value: _adults,
                  onDecrease: _adults > 1
                      ? () => setState(() => _adults--)
                      : null,
                  onIncrease: _adults + _children < 10
                      ? () => setState(() => _adults++)
                      : null,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: _buildGuestCounter(
                  label: 'Trẻ em',
                  value: _children,
                  onDecrease: _children > 0
                      ? () => setState(() => _children--)
                      : null,
                  onIncrease: _adults + _children < 10
                      ? () => setState(() => _children++)
                      : null,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: _searchAvailableRooms,
              icon: const Icon(Icons.search_rounded),
              label: const Text('Kiểm tra phòng trống'),
              style: FilledButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildGuestCounter({
    required String label,
    required int value,
    required VoidCallback? onDecrease,
    required VoidCallback? onIncrease,
  }) {
    final colorScheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.fromLTRB(10, 8, 10, 6),
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.55),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: TextStyle(
              color: colorScheme.onSurfaceVariant,
              fontSize: 12,
              fontWeight: FontWeight.w600,
            ),
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              IconButton(
                visualDensity: VisualDensity.compact,
                onPressed: onDecrease,
                icon: const Icon(Icons.remove_circle_outline),
              ),
              Text(
                '$value',
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
              IconButton(
                visualDensity: VisualDensity.compact,
                onPressed: onIncrease,
                icon: const Icon(Icons.add_circle_outline),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Future<void> _pickBookingDateRange() async {
    final today = DateUtils.dateOnly(DateTime.now());
    final picked = await showDateRangePicker(
      context: context,
      firstDate: today,
      lastDate: today.add(const Duration(days: 365)),
      initialDateRange: _selectedDateRange,
    );
    if (picked == null || !mounted) return;
    final start = DateUtils.dateOnly(picked.start);
    final end = DateUtils.dateOnly(picked.end);
    if (!end.isAfter(start)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Ngày trả phòng phải sau ngày nhận phòng.'),
        ),
      );
      return;
    }
    setState(() {
      _selectedDateRange = DateTimeRange(start: start, end: end);
    });
  }

  String _formatDate(DateTime value) {
    final day = value.day.toString().padLeft(2, '0');
    final month = value.month.toString().padLeft(2, '0');
    return '$day/$month/${value.year}';
  }

  // Removed static _buildCategoryTabs since categories are now built dynamically inside FutureBuilder
}
