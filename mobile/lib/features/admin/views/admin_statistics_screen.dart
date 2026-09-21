import 'package:flutter/material.dart';
import 'package:flutter/services.dart'; // 🚀 BẮT BUỘC: Để đồng bộ màu Pin, Wifi và Đồng hồ máy Samsung
import '../../../core/constants/app_colors.dart';
import '../../../core/utils/responsive.dart';
import '../../auth/services/auth_api_service.dart'; // Đã đồng bộ đường dẫn tương đối nhóm phân lớp

class AdminStatisticsScreen extends StatefulWidget {
  const AdminStatisticsScreen({Key? key}) : super(key: key);

  @override
  State<AdminStatisticsScreen> createState() => _StatisticsScreenState();
}

class _StatisticsScreenState extends State<AdminStatisticsScreen> {
  final _apiService = AuthApiService();

  bool _isLoading = true;
  String? _errorMessage;
  bool _isSimulationMode = false;

  // Dữ liệu thống kê chuẩn hóa từ C# SQL Server Backend
  double _totalRevenue = 0.0;
  int _vacantRooms = 0;
  int _totalBookings = 0;
  List<double> _monthlyRevenue = List.filled(12, 0.0);
  List<int> _monthlyBookings = List.filled(12, 0);

  // Chọn loại dữ liệu để hiển thị biểu đồ: 0 - Doanh thu, 1 - Đơn đặt
  int _selectedChartTab = 0;
  int? _hoveredIndex; // Index của cột được tap vào để hiển thị tooltip

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  // 🚀 LUỒNG NẠP DỮ LIỆU LIVE 100% TỪ SQL SERVER BACKEND
  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      if (_isSimulationMode) {
        await _loadSimulatedData();
      } else {
        // Gọi hàm bốc gói tin từ StatsApiController.cs của nhóm
        final data = await _apiService.getStatsSummary();
        setState(() {
          _totalRevenue = (data['revenue'] as num?)?.toDouble() ?? 0.0;
          _vacantRooms = (data['vacant'] as num?)?.toInt() ?? 0;
          _totalBookings = (data['totalBookings'] as num?)?.toInt() ?? 0;

          if (data['monthlyRevenue'] != null) {
            _monthlyRevenue = (data['monthlyRevenue'] as List)
                .map((e) => (e as num).toDouble())
                .toList();
          }
          if (data['monthlyBookings'] != null) {
            _monthlyBookings = (data['monthlyBookings'] as List)
                .map((e) => (e as num).toInt())
                .toList();
          }
          _isLoading = false;
        });
      }
    } catch (e) {
      debugPrint("Lỗi tải thống kê hệ thống: $e");
      setState(() {
        _isSimulationMode = true;
        _errorMessage = "Mất kết nối API Server. Hệ thống tự chuyển sang Chế độ mô phỏng ngoại tuyến.";
      });
      await _loadSimulatedData();
    }
  }

  // Luồng dữ liệu giả lập cứu cánh phòng khi mạng lỗi gầm giường lúc demo
  Future<void> _loadSimulatedData() async {
    await Future.delayed(const Duration(milliseconds: 500));
    setState(() {
      _totalRevenue = 154500000.0;
      _vacantRooms = 8;
      _totalBookings = 64;
      _monthlyRevenue = [14.2, 16.5, 9.8, 21.4, 26.8, 32.2, 29.5, 36.1, 42.0, 19.5, 24.4, 40.2]
          .map((m) => m * 1000000.0)
          .toList();
      _monthlyBookings = [26, 32, 20, 38, 48, 55, 50, 60, 68, 34, 40, 62];
      _isLoading = false;
    });
  }

  void _showSnackBar(String msg) {
    ScaffoldMessenger.of(context).removeCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(msg, style: const TextStyle(fontWeight: FontWeight.w500)),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final bool isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        // 🚀 ĐỒNG BỘ TIÊU CHUẨN STATUS BAR: Khóa màu Pin, Wifi và Đồng hồ máy Samsung không bị tàng hình
        systemOverlayStyle: SystemUiOverlayStyle(
          statusBarColor: Colors.transparent,
          statusBarIconBrightness: isDark ? Brightness.light : Brightness.dark,
          statusBarBrightness: isDark ? Brightness.dark : Brightness.light,
        ),
        title: Text("Báo cáo & Thống kê", style: TextStyle(fontWeight: FontWeight.bold, color: colorScheme.onSurface)),
        centerTitle: true,
        actions: [
          IconButton(
            icon: Icon(_isSimulationMode ? Icons.cloud_off_rounded : Icons.cloud_done_rounded,
                color: _isSimulationMode ? Colors.orange : Colors.green),
            onPressed: () {
              setState(() => _isSimulationMode = !_isSimulationMode);
              _showSnackBar(_isSimulationMode ? "Đã bật Chế độ mô phỏng ngoại tuyến." : "Đã kích hoạt kết nối SQL Server thật.");
              _loadData();
            },
            tooltip: "Chuyển chế độ kết nối",
          ),
          IconButton(
            icon: Icon(Icons.refresh_rounded, color: colorScheme.onSurface),
            onPressed: _loadData,
            tooltip: "Làm mới dữ liệu",
          )
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
        onRefresh: _loadData,
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: EdgeInsets.symmetric(
            horizontal: Responsive.width(context) * 0.06,
            vertical: Responsive.sp(context, 10),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (_errorMessage != null) _buildWarningBanner(),

              Text(
                "Tổng quan hoạt động",
                style: TextStyle(fontSize: Responsive.sp(context, 16), fontWeight: FontWeight.bold, color: colorScheme.onSurface),
              ),
              const SizedBox(height: 12),

              // 📊 HÀNG METRIC CARDS ĐỒNG BỘ RADIUS V20 VÀ GRADIANT CAO CẤP
              Row(
                children: [
                  _buildMetricCard(
                    context,
                    title: "Tổng doanh thu",
                    value: _formatCurrency(_totalRevenue),
                    icon: Icons.monetization_on_rounded,
                    gradient: const LinearGradient(colors: [Color(0xFF1B5E20), Color(0xFF4CAF50)]),
                  ),
                  const SizedBox(width: 12),
                  _buildMetricCard(
                    context,
                    title: "Lượt đặt phòng",
                    value: "${_totalBookings} đơn đặt",
                    icon: Icons.assignment_turned_in_rounded,
                    gradient: const LinearGradient(colors: [Color(0xFF4A148C), Color(0xFFAB47BC)]),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              _buildFullWidthMetricCard(context, title: "Số phòng hiện tại đang trống", value: "${_vacantRooms} phòng khả dụng", icon: Icons.bed_outlined, color: Colors.blue),

              const SizedBox(height: 25),

              // 📈 ĐỒNG BỘ KHAY CHỌN TAB BIỂU ĐỒ
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    "Xu hướng hoạt động",
                    style: TextStyle(fontSize: Responsive.sp(context, 16), fontWeight: FontWeight.bold, color: colorScheme.onSurface),
                  ),
                  Container(
                    height: 36,
                    padding: const EdgeInsets.all(2),
                    decoration: BoxDecoration(color: colorScheme.surfaceVariant.withOpacity(0.4), borderRadius: BorderRadius.circular(10)),
                    child: Row(
                      children: [
                        _buildChartTabButton("Doanh thu", 0),
                        _buildChartTabButton("Đơn đặt", 1),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 15),

              // Khung chứa biểu đồ hình cột tự dựng mượt mà
              _buildChartContainer(context),

              const SizedBox(height: 25),

              Text(
                "Chi tiết 12 tháng qua",
                style: TextStyle(fontSize: Responsive.sp(context, 16), fontWeight: FontWeight.bold, color: colorScheme.onSurface),
              ),
              const SizedBox(height: 12),
              _buildMonthlyDataList(context),
            ],
          ),
        ),
      ),
    );
  }

  // --- CÁC WIDGET CON ĐỒNG BỘ LAYOUT TOÀN DIỆN ---

  Widget _buildWarningBanner() {
    return Container(
      padding: const EdgeInsets.all(12),
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        color: Colors.orange.withOpacity(0.12),
        border: Border.all(color: Colors.orange.withOpacity(0.4)),
        borderRadius: BorderRadius.circular(15),
      ),
      child: Row(
        children: [
          const Icon(Icons.wifi_off_rounded, color: Colors.orange, size: 20),
          const SizedBox(width: 10),
          Expanded(child: Text(_errorMessage!, style: const TextStyle(fontSize: 11, color: Colors.orange, fontWeight: FontWeight.bold))),
        ],
      ),
    );
  }

  Widget _buildMetricCard(BuildContext context, {required String title, required String value, required IconData icon, required Gradient gradient}) {
    return Expanded(
      child: Container(
        height: 120,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          gradient: gradient,
          borderRadius: BorderRadius.circular(Responsive.sp(context, 20)), // Đồng bộ bo tròn v20 toàn hệ thống
          boxShadow: [BoxShadow(color: gradient.colors.first.withOpacity(0.25), blurRadius: 10, offset: const Offset(0, 5))],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Icon(icon, color: Colors.white.withOpacity(0.9), size: 26),
                const Icon(Icons.trending_up_rounded, color: Colors.white, size: 16),
              ],
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(value, style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold), overflow: TextOverflow.ellipsis),
                const SizedBox(height: 2),
                Text(title, style: TextStyle(color: Colors.white.withOpacity(0.75), fontSize: 11)),
              ],
            )
          ],
        ),
      ),
    );
  }

  Widget _buildFullWidthMetricCard(BuildContext context, {required String title, required String value, required IconData icon, required Color color}) {
    final colorScheme = Theme.of(context).colorScheme;
    final bool isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(Responsive.sp(context, 20)),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(isDark ? 0.3 : 0.05), blurRadius: 10, offset: const Offset(0, 5))],
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(color: color.withOpacity(0.1), shape: BoxShape.circle),
            child: Icon(icon, color: color, size: 24),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: TextStyle(color: colorScheme.onSurface.withOpacity(0.5), fontSize: 12, fontWeight: FontWeight.bold)),
                const SizedBox(height: 4),
                Text(value, style: TextStyle(color: colorScheme.onSurface, fontSize: 18, fontWeight: FontWeight.bold)),
              ],
            ),
          ),
          Icon(Icons.chevron_right_rounded, color: colorScheme.onSurface.withOpacity(0.3)),
        ],
      ),
    );
  }

  Widget _buildChartTabButton(String label, int index) {
    final colorScheme = Theme.of(context).colorScheme;
    final bool isSelected = _selectedChartTab == index;

    return GestureDetector(
      onTap: () => setState(() { _selectedChartTab = index; _hoveredIndex = null; }),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: isSelected ? colorScheme.surface : Colors.transparent,
          borderRadius: BorderRadius.circular(8),
          boxShadow: isSelected ? [const BoxShadow(color: Colors.black12, blurRadius: 4, offset: Offset(0, 2))] : null,
        ),
        child: Text(
          label,
          style: TextStyle(color: isSelected ? colorScheme.primary : colorScheme.onSurface.withOpacity(0.5), fontSize: 12, fontWeight: isSelected ? FontWeight.bold : FontWeight.normal),
        ),
      ),
    );
  }

  Widget _buildChartContainer(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final bool isDark = Theme.of(context).brightness == Brightness.dark;

    List<double> chartValues = _selectedChartTab == 0 ? _monthlyRevenue : _monthlyBookings.map((e) => e.toDouble()).toList();
    double maxValue = chartValues.isNotEmpty ? chartValues.reduce((a, b) => a > b ? a : b) : 0.0;
    if (maxValue == 0) maxValue = 1.0;

    return Container(
      height: 240,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(Responsive.sp(context, 20)),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(isDark ? 0.3 : 0.05), blurRadius: 10, offset: const Offset(0, 5))],
      ),
      child: Column(
        children: [
          if (_hoveredIndex != null)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
              decoration: BoxDecoration(color: colorScheme.primary, borderRadius: BorderRadius.circular(8)),
              child: Text(
                "Tháng ${_hoveredIndex! + 1}: ${_selectedChartTab == 0 ? _formatCurrency(chartValues[_hoveredIndex!]) : "${chartValues[_hoveredIndex!].toInt()} lượt đặt"}",
                style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold),
              ),
            )
          else
            Text("Chạm từng cột để theo dõi số liệu chi tiết", style: TextStyle(color: colorScheme.onSurface.withOpacity(0.4), fontSize: 11, fontWeight: FontWeight.bold)),
          const SizedBox(height: 14),
          Expanded(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              crossAxisAlignment: CrossAxisAlignment.end,
              children: List.generate(12, (index) {
                final double percent = chartValues[index] / maxValue;
                final bool isHovered = _hoveredIndex == index;

                return GestureDetector(
                  onTap: () => setState(() => _hoveredIndex = index),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.end,                    children: [
                    Expanded(
                      child: LayoutBuilder(
                        builder: (context, constraints) {
                          return Align(
                            alignment: Alignment.bottomCenter,
                            child: Container(
                              width: 14,
                              height: (constraints.maxHeight * percent) > 4 ? (constraints.maxHeight * percent) : 4,
                              decoration: BoxDecoration(
                                gradient: LinearGradient(
                                  begin: Alignment.bottomCenter,
                                  end: Alignment.topCenter,
                                  colors: isHovered
                                      ? [colorScheme.secondary, colorScheme.secondary.withOpacity(0.6)]
                                      : [colorScheme.primary, colorScheme.primary.withOpacity(0.5)],
                                ),
                                borderRadius: BorderRadius.circular(4),
                              ),
                            ),
                          );
                        },
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text("T${index + 1}", style: TextStyle(fontSize: 9, fontWeight: isHovered ? FontWeight.bold : FontWeight.normal, color: isHovered ? colorScheme.primary : colorScheme.onSurface.withOpacity(0.5))),
                  ],
                  ),
                );
              }),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMonthlyDataList(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final bool isDark = Theme.of(context).brightness == Brightness.dark;

    return ListView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: 12,
      itemBuilder: (context, index) {
        final revenue = _monthlyRevenue[index];
        final bookings = _monthlyBookings[index];

        return Container(
          margin: const EdgeInsets.only(bottom: 10),
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          decoration: BoxDecoration(
            color: colorScheme.surface,
            borderRadius: BorderRadius.circular(15),
            boxShadow: [BoxShadow(color: Colors.black.withOpacity(isDark ? 0.2 : 0.03), blurRadius: 6, offset: const Offset(0, 3))],
            border: Border.all(color: colorScheme.outlineVariant.withOpacity(0.4)),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text("Tháng ${index + 1}", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: colorScheme.onSurface)),
              Row(
                children: [
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text(_formatCurrency(revenue), style: TextStyle(color: colorScheme.primary, fontWeight: FontWeight.bold, fontSize: 14)),
                      const SizedBox(height: 2),
                      Text("$bookings lượt đặt", style: TextStyle(color: colorScheme.onSurface.withOpacity(0.4), fontSize: 11, fontWeight: FontWeight.bold)),
                    ],
                  ),
                  const SizedBox(width: 8),
                  Icon(Icons.keyboard_arrow_right_rounded, color: colorScheme.onSurface.withOpacity(0.3)),
                ],
              )
            ],
          ),
        );
      },
    );
  }

  String _formatCurrency(double amount) {
    if (amount >= 1000000) return "${(amount / 1000000).toStringAsFixed(1)}M";
    if (amount >= 1000) return "${(amount / 1000).toStringAsFixed(0)}K";
    return "${amount.toStringAsFixed(0)}đ";
  }
}