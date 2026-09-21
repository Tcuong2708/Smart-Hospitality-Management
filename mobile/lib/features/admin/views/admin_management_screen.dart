import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/utils/responsive.dart';

class AdminManagementScreen extends StatefulWidget {
  const AdminManagementScreen({Key? key}) : super(key: key);

  @override
  State<AdminManagementScreen> createState() => _AdminManagementScreenState();
}

class _AdminManagementScreenState extends State<AdminManagementScreen> {
  final TextEditingController _searchController = TextEditingController();
  String _searchQuery = "";

  // Dữ liệu giả lập danh sách tài khoản nhân viên (Sẵn sàng kết nối API SQL Server của nhóm)
  final List<Map<String, dynamic>> _staffList = [
    {"username": "letan01", "name": "Nguyễn Văn Đạt", "role": "Lễ tân ca sáng", "isOnline": true, "email": "datnv@mayhotel.com"},
    {"username": "letan02", "name": "Trần Thị Hồng", "role": "Lễ tân ca chiều", "isOnline": true, "email": "hongtt@mayhotel.com"},
    {"username": "letan03", "name": "Lê Hoàng Long", "role": "Lễ tân ca đêm", "isOnline": false, "email": "longlh@mayhotel.com"},
    {"username": "baove01", "name": "Phạm Minh Hùng", "role": "Đội trưởng Bảo vệ", "isOnline": true, "email": "hungpm@mayhotel.com"},
  ];

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final bool isDark = theme.brightness == Brightness.dark;

    // Lọc danh sách nhân viên theo thanh tìm kiếm thông minh
    final filteredStaff = _staffList.where((staff) {
      final name = staff['name'].toString().toLowerCase();
      final username = staff['username'].toString().toLowerCase();
      return name.contains(_searchQuery.toLowerCase()) || username.contains(_searchQuery.toLowerCase());
    }).toList();

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        // 🚀 ĐỒNG BỘ STATUS BAR: Ép biểu tượng Pin, Wifi, Đồng hồ lật màu chuẩn chỉ
        systemOverlayStyle: SystemUiOverlayStyle(
          statusBarColor: Colors.transparent,
          statusBarIconBrightness: isDark ? Brightness.light : Brightness.dark,
          statusBarBrightness: isDark ? Brightness.dark : Brightness.light,
        ),
        title: Text(
          "Quản Trị Hệ Thống",
          style: TextStyle(color: colorScheme.onSurface, fontWeight: FontWeight.bold, fontSize: Responsive.sp(context, 18)),
        ),
        centerTitle: true,
        actions: [
          IconButton(
            icon: Icon(Icons.shield_outlined, color: colorScheme.primary),
            onPressed: () {},
          )
        ],
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.symmetric(
          horizontal: Responsive.width(context) * 0.07,
          vertical: Responsive.sp(context, 10),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // --- 🎛️ KHỐI 1: LƯỚI CHỨC NĂNG QUẢN TRỊ (GRID) ---
            Text(
              "Phân hệ quản lý",
              style: TextStyle(fontSize: Responsive.sp(context, 15), fontWeight: FontWeight.bold, color: colorScheme.onSurface),
            ),
            const SizedBox(height: 12),
            GridView.count(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              crossAxisCount: 2,
              crossAxisSpacing: 12,
              mainAxisSpacing: 12,
              childAspectRatio: 1.4,
              children: [
                _buildGridCard(context, Icons.people_alt_rounded, "Nhân Sự", "4 tài khoản", Colors.blue, () {}),
                _buildGridCard(context, Icons.meeting_room_rounded, "Sơ Đồ Phòng", "48 phòng", Colors.green, () {}),
                _buildGridCard(context, Icons.receipt_long_rounded, "Hóa Đơn Live", "12 đơn mới", Colors.orange, () {}),
                _buildGridCard(context, Icons.settings_suggest_rounded, "Cấu Hình", "Thông số thông minh", Colors.purple, () {}),
              ],
            ),

            SizedBox(height: Responsive.sp(context, 25)),

            // --- 🔍 KHỐI 2: THANH TÌM KIẾM NHÂN VIÊN CAO CẤP ---
            Text(
              "Danh sách nhân sự nội bộ",
              style: TextStyle(fontSize: Responsive.sp(context, 15), fontWeight: FontWeight.bold, color: colorScheme.onSurface),
            ),
            const SizedBox(height: 12),
            _buildSearchField(context),

            const SizedBox(height: 15),

            // --- 👥 KHỐI 3: DANH SÁCH THẺ NHÂN VIÊN ĐỒNG BỘ LAYOUT ---
            filteredStaff.isEmpty
                ? const Padding(
              padding: EdgeInsets.symmetric(vertical: 20),
              child: Text("Không tìm thấy nhân viên phù hợp!", style: TextStyle(color: Colors.grey)),
            )
                : ListView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: filteredStaff.length,
              itemBuilder: (context, index) {
                final staff = filteredStaff[index];
                return _buildStaffCard(context, staff);
              },
            ),
            SizedBox(height: Responsive.sp(context, 20)),
          ],
        ),
      ),
    );
  }

  // --- CÁC HELPER WIDGET CON ĐẠT ĐỘ ĐỒNG BỘ THIẾT KẾ 100% ---

  Widget _buildGridCard(BuildContext context, IconData icon, String title, String sub, Color color, VoidCallback onTap) {
    final colorScheme = Theme.of(context).colorScheme;
    final bool isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(Responsive.sp(context, 20)), // Đồng bộ bo tròn góc v20
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(isDark ? 0.3 : 0.04), // Đồng bộ sắc độ bóng đổ trang Login
            blurRadius: 10,
            offset: const Offset(0, 5),
          )
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(Responsive.sp(context, 20)),
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(color: color.withOpacity(0.1), shape: BoxShape.circle),
                  child: Icon(icon, color: color, size: 22),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: colorScheme.onSurface)),
                    const SizedBox(height: 2),
                    Text(sub, style: TextStyle(color: colorScheme.onSurface.withOpacity(0.4), fontSize: 11, fontWeight: FontWeight.w500)),
                  ],
                )
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildSearchField(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final bool isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(Responsive.sp(context, 20)),
        boxShadow: [
          BoxShadow(color: Colors.black.withOpacity(isDark ? 0.3 : 0.04), blurRadius: 8, offset: const Offset(0, 4))
        ],
      ),
      child: TextField(
        controller: _searchController,
        onChanged: (val) => setState(() => _searchQuery = val),
        style: TextStyle(color: colorScheme.onSurface, fontSize: 14),
        decoration: InputDecoration(
          prefixIcon: Icon(Icons.search_rounded, color: colorScheme.onSurface.withOpacity(0.4)),
          suffixIcon: _searchQuery.isNotEmpty
              ? IconButton(
            icon: Icon(Icons.clear_rounded, color: colorScheme.onSurface.withOpacity(0.4)),
            onPressed: () {
              _searchController.clear();
              setState(() => _searchQuery = "");
            },
          )
              : null,
          hintText: "Tìm nhân viên (Họ tên hoặc tài khoản)...",
          hintStyle: TextStyle(color: colorScheme.onSurface.withOpacity(0.3), fontSize: 13),
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(vertical: 14),
        ),
      ),
    );
  }

  Widget _buildStaffCard(BuildContext context, Map<String, dynamic> staff) {
    final colorScheme = Theme.of(context).colorScheme;
    final bool isDark = Theme.of(context).brightness == Brightness.dark;
    final bool isOnline = staff['isOnline'] as bool;

    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(Responsive.sp(context, 20)),
        boxShadow: [
          BoxShadow(color: Colors.black.withOpacity(isDark ? 0.2 : 0.03), blurRadius: 6, offset: const Offset(0, 3))
        ],
        border: Border.all(color: colorScheme.outlineVariant.withOpacity(0.3)),
      ),
      child: Row(
        children: [
          // Khay trạng thái Online/Offline hình tròn tinh tế
          Stack(
            children: [
              CircleAvatar(
                radius: 22,
                backgroundColor: colorScheme.primary.withOpacity(0.1),
                child: Icon(Icons.person_rounded, color: colorScheme.primary.withOpacity(0.7)),
              ),
              Positioned(
                bottom: 0,
                right: 0,
                child: Container(
                  width: 12,
                  height: 12,
                  decoration: BoxDecoration(
                    color: isOnline ? Colors.green : Colors.grey,
                    shape: BoxShape.circle,
                    border: Border.all(color: colorScheme.surface, width: 2),
                  ),
                ),
              )
            ],
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(staff['name'], style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: colorScheme.onSurface)),
                const SizedBox(height: 2),
                Text("@${staff['username']} • ${staff['role']}", style: TextStyle(color: colorScheme.onSurface.withOpacity(0.5), fontSize: 11, fontWeight: FontWeight.w500)),
              ],
            ),
          ),
          Icon(Icons.chevron_right_rounded, color: colorScheme.onSurface.withOpacity(0.3)),
        ],
      ),
    );
  }
}