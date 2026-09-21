import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:may_hotel_app/main.dart';
import 'package:may_hotel_app/features/checkin/views/room_map_screen.dart';
import 'package:may_hotel_app/features/services/views/service_management_screen.dart';
// Import thêm trang Sơ đồ phòng của Cường vào đây
import 'package:may_hotel_app/features/checkin/views/room_map_screen.dart';
import 'package:may_hotel_app/features/reception/views/cccd_checkin_screen.dart';
import 'package:may_hotel_app/features/reception/views/receipt_printer_screen.dart';
import '../../../core/constants/app_colors.dart';
import '../../admin/views/admin_management_screen.dart';
import '../../admin/views/admin_room_screen.dart';
import '../../admin/views/admin_user_screen.dart';
import 'booked_rooms_screen.dart';
import '../../checkin/views/check_in_screen.dart';
import 'home_screen.dart';
import 'booking_screen.dart';
import 'discovery_screen.dart';
import '../../chat/views/chat_screen.dart';
import 'profile_screen.dart';
import '../../admin/views/admin_statistics_screen.dart';

import 'package:may_hotel_app/services/auth_state_service.dart';
import 'package:may_hotel_app/services/language_service.dart';

class NavItem {
  final IconData icon;
  final String label;
  final Widget screen;
  final bool isBig;

  NavItem({
    required this.icon,
    required this.label,
    required this.screen,
    this.isBig = false
  });
}

class MainScreen extends StatefulWidget {
  final String role;
  final String username;

  const MainScreen({Key? key, required this.role, required this.username}) : super(key: key);
  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _selectedIndex = 0;
  late List<NavItem> _navItems;

  @override
  void initState() {
    super.initState();
    _initializeMenu();
  }

  void _initializeMenu() {
    final lang = LanguageService();
    
    if (widget.role == 'admin') {
      _navItems = [
        NavItem(icon: Icons.analytics_outlined, label: lang.translate('statistics'), screen: AdminStatisticsScreen()),
        NavItem(icon: Icons.meeting_room_outlined, label: lang.translate('rooms'), screen: AdminRoomScreen()),
        NavItem(icon: Icons.admin_panel_settings, label: lang.translate('admin'), screen: AdminManagementScreen(), isBig: true),
        NavItem(icon: Icons.people_alt_outlined, label: lang.translate('personnel'), screen: const AdminUserScreen()),
        NavItem(icon: Icons.person_outline, label: lang.translate('profile'), screen: ProfileScreen(username: widget.username)),
      ];
    } else if (widget.role == 'staff') {
      _navItems = [
        NavItem(icon: Icons.grid_view_rounded, label: lang.translate('map'), screen: const RoomMapScreen()),
        NavItem(icon: Icons.checklist_rtl_rounded, label: 'Check-in', screen: CheckInScreen()),
        NavItem(icon: Icons.nfc_rounded, label: 'Quét CCCD', screen: const CccdCheckinScreen(), isBig: true),
        NavItem(icon: Icons.print_rounded, label: 'In Hóa Đơn', screen: const ReceiptPrinterScreen()),
        NavItem(icon: Icons.person_outline, label: lang.translate('profile'), screen: ProfileScreen(username: widget.username)),
      ];
    } else {
      _navItems = [
        NavItem(icon: Icons.home_max_rounded, label: lang.translate('home'), screen: const HomeScreen()),
        NavItem(icon: Icons.calendar_month_outlined, label: lang.translate('booked'), screen: const BookedRoomsScreen()),
        NavItem(icon: Icons.search_rounded, label: lang.translate('discovery'), screen: const DiscoveryScreen(), isBig: true),
        NavItem(icon: Icons.chat_bubble_outline, label: lang.translate('chatbot'), screen: const ChatScreen()),
        NavItem(icon: Icons.person_outline, label: lang.translate('me'), screen: ProfileScreen(username: widget.username)),
      ];
    }
  }

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    // 🚀 LẮNG NGHE THEME & NGÔN NGỮ CHỦ ĐỘNG
    return ListenableBuilder(
      listenable: Listenable.merge([themeNotifier, LanguageService()]),
      builder: (context, _) {
        final bool isDark = themeNotifier.value == ThemeMode.dark;
        _initializeMenu(); // Cập nhật lại menu khi ngôn ngữ đổi
        
        return AnnotatedRegion<SystemUiOverlayStyle>(
          value: SystemUiOverlayStyle(
            statusBarColor: Colors.transparent,
            statusBarIconBrightness: isDark ? Brightness.light : Brightness.dark,
            statusBarBrightness: isDark ? Brightness.dark : Brightness.light,
            systemNavigationBarColor: Colors.transparent,
            systemNavigationBarDividerColor: Colors.transparent,
            systemNavigationBarIconBrightness: isDark ? Brightness.light : Brightness.dark,
            systemNavigationBarContrastEnforced: false,
          ),
          child: Scaffold(
            body: IndexedStack(
              index: _selectedIndex,
              children: _navItems.map((item) => item.screen).toList(),
            ),
            bottomNavigationBar: Container(
              color: Theme.of(context).scaffoldBackgroundColor,
              child: SafeArea(
                maintainBottomViewPadding: true,
                child: _buildModernNav(),
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildModernNav() {
    bool isTablet = MediaQuery.of(context).size.width >= 600;
    
    return Container(
      height: 75,
      margin: EdgeInsets.symmetric(
        horizontal: isTablet ? 100 : 10, // Co bóp thanh điều hướng trên máy tính bảng cho đẹp
        vertical: 10,
      ),
      decoration: BoxDecoration(
        color: AppColors.primary,
        borderRadius: BorderRadius.circular(30),
        boxShadow: [
          BoxShadow(
              color: AppColors.primary.withOpacity(0.3),
              blurRadius: 15,
              spreadRadius: 2,
              offset: const Offset(0, 5)
          )
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: List.generate(_navItems.length, (index) {
          return _buildNavItem(_navItems[index], index);
        }),
      ),
    );
  }

  Widget _buildNavItem(NavItem item, int index) {
    bool isSelected = _selectedIndex == index;

    // Nếu là nút Big Icon (nút giữa), cho nó nổi bật hơn
    if (item.isBig) {
      return GestureDetector(
        onTap: () => _onItemTapped(index),
        child: Transform.translate(
          offset: const Offset(0, -5), // Nhích lên một chút cho chuyên nghiệp
          child: Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: isSelected ? Colors.white : Colors.white.withOpacity(0.2),
              shape: BoxShape.circle,
              boxShadow: isSelected ? [
                BoxShadow(color: Colors.white.withOpacity(0.5), blurRadius: 10)
              ] : [],
            ),
            child: Icon(
              item.icon,
              color: isSelected ? AppColors.primary : Colors.white,
              size: 30,
            ),
          ),
        ),
      );
    }

    return GestureDetector(
      onTap: () => _onItemTapped(index),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              item.icon,
              color: isSelected ? Colors.white : Colors.white.withOpacity(0.5),
              size: 24,
            ),
            const SizedBox(height: 4),
            // Chỉ hiện label khi được chọn để thanh menu trông thoáng hơn
            if (isSelected)
              Container(
                width: 4,
                height: 4,
                decoration: const BoxDecoration(
                  color: Colors.white,
                  shape: BoxShape.circle,
                ),
              )
            else
              Text(
                item.label,
                style: TextStyle(
                  color: Colors.white.withOpacity(0.5),
                  fontSize: 9,
                ),
              ),
          ],
        ),
      ),
    );
  }
}