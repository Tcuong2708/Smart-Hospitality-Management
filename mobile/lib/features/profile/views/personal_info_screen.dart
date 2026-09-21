import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/utils/responsive.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart'; // 🚀 ÉP CHỌC THẲNG: Đi trực tiếp qua Provider lõi để né lỗi trung gian

class PersonalInfoScreen extends StatefulWidget {
  final String username;

  const PersonalInfoScreen({Key? key, required this.username}) : super(key: key);

  @override
  State<PersonalInfoScreen> createState() => _PersonalInfoScreenState();
}

class _PersonalInfoScreenState extends State<PersonalInfoScreen> {
  // Đổi sang dùng trực tiếp HotelApiProvider lõi của Cường đã thông mạch Ngrok
  final _apiProvider = HotelApiProvider();

  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _phoneController = TextEditingController();
  final TextEditingController _dobController = TextEditingController();
  final TextEditingController _genderController = TextEditingController();

  bool _isLoading = true;
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    _fetchUserProfile();
  }

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    _dobController.dispose();
    _genderController.dispose();
    super.dispose();
  }

  // 🔄 LUỒNG 1: TẢI HỒ SƠ TÀI KHOẢN TỪ BACKEND (Đfont ĐỒNG BỘ CHỮ VIẾT THƯỜNG KHỚP C#)
  Future<void> _fetchUserProfile() async {
    try {
      final profile = await _apiProvider.getUserProfile(widget.username);

      if (profile != null && mounted) {
        setState(() {
          // 🎯 SỬA CHÍ MẠNG: Đảo sang viết thường hoTen, email, soDienThoai, diaChi khớp 100% với JSON C# trả về
          _nameController.text = profile['hoTen']?.toString() ?? "";
          _emailController.text = profile['email']?.toString() ?? "";
          _phoneController.text = profile['soDienThoai']?.toString() ?? "";
          _dobController.text = profile['diaChi']?.toString() ?? ""; // Tạm thời dùng trường diaChi thay thế cho NgaySinh nếu DB chưa map cột
          _genderController.text = "Nam"; // Giá trị đệm cố định
          _isLoading = false;
        });
      } else {
        _loadOfflineData();
      }
    } catch (e) {
      debugPrint("Lỗi nạp thông tin cá nhân: $e");
      _loadOfflineData();
    }
  }

  void _loadOfflineData() {
    if (!mounted) return;
    setState(() {
      _nameController.text = widget.username;
      _emailController.text = widget.username.contains('@') ? widget.username : "${widget.username}@mayhotel.com";
      _phoneController.text = "Chưa cập nhật";
      _dobController.text = "Chưa cập nhật";
      _genderController.text = "Nam";
      _isLoading = false;
    });
  }

  // 🔄 LUỒNG 2: ĐẨY CẬP NHẬT LÊN SQL SERVER BACKEND (THÔNG QUA BODY PAYLOAD GỌI DIO)
  Future<void> _handleUpdateProfile() async {
    setState(() => _isSaving = true);

    try {
      // Chuẩn bị map dữ liệu gói gọn đưa vào body trải phẳng
      final Map<String, dynamic> updateData = {
        "hoTen": _nameController.text.trim(),
        "soDienThoai": _phoneController.text.trim(),
        "diaChi": _dobController.text.trim(), // Đồng bộ map xuống trường địa chỉ C#
        "email": _emailController.text.trim(),
      };

      // 🚀 CHỐT HẠ: Gọi chuẩn hàm cập nhật của Provider để đẩy key "username" viết thường lên C#
      final bool success = await _apiProvider.updateUserProfile(widget.username, updateData);

      if (success && mounted) {
        _showCustomSnackBar("🟢 Cập nhật thông tin cá nhân thành công!", Colors.green);
        Navigator.pop(context, true); // Trả về true báo hiệu cho màn hình Profile cập nhật lại giao diện chữ
      } else {
        _showCustomSnackBar("Cập nhật thất bại. Vui lòng kiểm tra dữ liệu đầu vào!", Colors.red);
      }
    } catch (e) {
      debugPrint("Lỗi lưu thông tin: $e");
      _showCustomSnackBar("Lỗi: Không thể đồng bộ thông tin lên Server.", Colors.red);
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  void _showCustomSnackBar(String message, Color color) {
    ScaffoldMessenger.of(context).removeCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message, style: const TextStyle(fontWeight: FontWeight.w500)),
        backgroundColor: color,
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
        systemOverlayStyle: SystemUiOverlayStyle(
          statusBarColor: Colors.transparent,
          statusBarIconBrightness: isDark ? Brightness.light : Brightness.dark,
          statusBarBrightness: isDark ? Brightness.dark : Brightness.light,
        ),
        leading: BackButton(color: colorScheme.onSurface),
        title: Text(
          "Thông tin cá nhân",
          style: TextStyle(color: colorScheme.onSurface, fontWeight: FontWeight.bold),
        ),
        centerTitle: true,
      ),
      body: _isLoading
          ? Center(child: CircularProgressIndicator(color: colorScheme.primary))
          : SingleChildScrollView(
        padding: EdgeInsets.symmetric(
          horizontal: Responsive.width(context) * 0.08,
          vertical: Responsive.sp(context, 15),
        ),
        child: Column(
          children: [
            // 👤 Đfont VÁ LỖI KHUNG ẢNH ĐẠI DIỆN: Dùng cơ chế bọc lỗi chống sập Handshake SSL
            Center(
              child: Stack(
                children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(100),
                    child: Image.network(
                      "https://via.placeholder.com/150",
                      width: Responsive.sp(context, 110),
                      height: Responsive.sp(context, 110),
                      fit: BoxFit.cover,
                      // 🔥 BẪY CỨU NẠP: Nếu link mạng sập chứng chỉ, lập tức bung ngay Container Icon người thay thế mượt mà
                      errorBuilder: (context, error, stackTrace) => Container(
                        width: Responsive.sp(context, 110),
                        height: Responsive.sp(context, 110),
                        color: colorScheme.primary.withOpacity(0.1),
                        child: Icon(Icons.person_rounded, size: 50, color: colorScheme.primary),
                      ),
                    ),
                  ),
                  Positioned(
                    bottom: 0,
                    right: 0,
                    child: Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                          color: colorScheme.primary,
                          shape: BoxShape.circle,
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withOpacity(0.15),
                              blurRadius: 6,
                              offset: const Offset(0, 3),
                            )
                          ]
                      ),
                      child: const Icon(Icons.camera_alt_rounded, color: Colors.white, size: 18),
                    ),
                  ),
                ],
              ),
            ),
            SizedBox(height: Responsive.sp(context, 30)),

            _buildSynchronizedField(context, "Họ và tên *", _nameController, Icons.badge_outlined),
            _buildSynchronizedField(context, "Email *", _emailController, Icons.email_outlined, keyboardType: TextInputType.emailAddress),
            _buildSynchronizedField(context, "Số điện thoại *", _phoneController, Icons.phone_android_outlined, keyboardType: TextInputType.phone),
            _buildSynchronizedField(context, "Địa chỉ cư trú", _dobController, Icons.map_outlined, hint: "Nhập địa chỉ của bạn"),
            _buildSynchronizedField(context, "Giới tính", _genderController, Icons.wc_outlined, hint: "Nam / Nữ"),

            SizedBox(height: Responsive.sp(context, 25)),

            // NÚT BẤM LƯU THAY ĐỔI
            SizedBox(
              width: double.infinity,
              height: Responsive.sp(context, 55),
              child: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: colorScheme.primary,
                  disabledBackgroundColor: colorScheme.primary.withOpacity(0.6),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(Responsive.sp(context, 15)),
                  ),
                  elevation: 4,
                ),
                onPressed: _isSaving ? null : _handleUpdateProfile,
                child: _isSaving
                    ? const SizedBox(
                  height: 22,
                  width: 22,
                  child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5),
                )
                    : const Text(
                  "Lưu thay đổi",
                  style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16),
                ),
              ),
            ),
            SizedBox(height: Responsive.sp(context, 20)),
          ],
        ),
      ),
    );
  }

  Widget _buildSynchronizedField(
      BuildContext context,
      String label,
      TextEditingController controller,
      IconData icon, {
        TextInputType keyboardType = TextInputType.text,
        String? hint,
      }) {
    final colorScheme = Theme.of(context).colorScheme;
    final bool isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      margin: EdgeInsets.only(bottom: Responsive.sp(context, 16)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(left: 4, bottom: 6),
            child: Text(
                label,
                style: TextStyle(
                  color: colorScheme.onSurface.withOpacity(0.6),
                  fontWeight: FontWeight.bold,
                  fontSize: 13,
                )
            ),
          ),
          Container(
            decoration: BoxDecoration(
              color: colorScheme.surface,
              borderRadius: BorderRadius.circular(Responsive.sp(context, 15)),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(isDark ? 0.3 : 0.05),
                  blurRadius: 10,
                  offset: const Offset(0, 5),
                )
              ],
            ),
            child: TextField(
              controller: controller,
              keyboardType: keyboardType,
              style: TextStyle(color: colorScheme.onSurface, fontSize: 15),
              decoration: InputDecoration(
                prefixIcon: Icon(icon, color: colorScheme.primary.withOpacity(0.6), size: 22),
                hintText: hint,
                hintStyle: TextStyle(color: colorScheme.onSurface.withOpacity(0.3), fontSize: 14),
                border: InputBorder.none,
                contentPadding: const EdgeInsets.symmetric(vertical: 15, horizontal: 10),
              ),
            ),
          ),
        ],
      ),
    );
  }
}