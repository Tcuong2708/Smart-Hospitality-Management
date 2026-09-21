import 'package:flutter/material.dart';
import 'package:may_hotel_app/models/user_model.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';

import '../../../core/constants/app_colors.dart';

class AdminUserScreen extends StatefulWidget {
  const AdminUserScreen({super.key});

  @override
  State<AdminUserScreen> createState() => _AdminUserScreenState();
}

class _AdminUserScreenState extends State<AdminUserScreen> {
  final HotelApiProvider apiProvider = HotelApiProvider();
  List<UserModel> users = [];
  bool isLoading = true;

  // MÀU SẮC THƯƠNG HIỆU GỐC (BRAND COLORS)
  static const Color navyPrimary = Color(0xFF0F2942);
  static const Color goldAccent = Color(0xFFC5A017);

  @override
  void initState() {
    super.initState();
    _fetchUsers();
  }

  Future<void> _fetchUsers() async {
    setState(() => isLoading = true);
    try {
      final data = await apiProvider.getUsers();
      setState(() {
        users = data;
        isLoading = false;
      });
    } catch (e) {
      setState(() => isLoading = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text("Lỗi kết nối API: $e"),
              backgroundColor: Colors.red),
        );
      }
    }
  }

  // HÀM HIỂN THỊ DIALOG (ĐÃ ĐỒNG BỘ DARK MODE)
  void _showUserDialog({UserModel? user}) {
    final isEdit = user != null;
    final nameController = TextEditingController(
        text: isEdit ? user.hoTen : "");
    final phoneController = TextEditingController(text: isEdit ? user.sdt : "");
    final emailController = TextEditingController(
        text: isEdit ? user.email : "");
    final addressController = TextEditingController(
        text: isEdit ? user.diaChi : "");
    final countryController = TextEditingController(
        text: isEdit ? user.quocTich : "Việt Nam");
    final userController = TextEditingController(
        text: isEdit ? user.tenDangNhap : "");
    final passController = TextEditingController(
        text: isEdit ? user.matKhau : "");

    bool currentStatus = isEdit ? user.trangThai : true;

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) =>
          StatefulBuilder(
            builder: (context, setDialogState) {
              // 🌟 KIỂM TRA CHẾ ĐỘ TỐI NỘI BỘ DIALOG
              final bool isDark = Theme
                  .of(context)
                  .brightness == Brightness.dark;

              return AlertDialog(
                backgroundColor: isDark ? const Color(0xFF1E2530) : Colors
                    .white,
                // Nền tối Carbon cao cấp
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12)),
                title: Text(
                  isEdit ? "Cập nhật khách hàng" : "Thêm khách hàng mới",
                  style: TextStyle(
                      color: isDark ? goldAccent : navyPrimary,
                      // Chế độ tối chữ tiêu đề sẽ chuyển sang màu Gold rực rỡ
                      fontWeight: FontWeight.bold,
                      fontSize: 18
                  ),
                ),
                content: SingleChildScrollView(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const SizedBox(height: 8),
                      TextField(
                        controller: nameController,
                        style: TextStyle(
                            color: isDark ? Colors.white : Colors.black),
                        decoration: InputDecoration(labelText: "Họ tên *",
                            border: const OutlineInputBorder()),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: phoneController,
                        style: TextStyle(
                            color: isDark ? Colors.white : Colors.black),
                        decoration: InputDecoration(labelText: "Số điện thoại",
                            border: const OutlineInputBorder()),
                        keyboardType: TextInputType.phone,
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: emailController,
                        style: TextStyle(
                            color: isDark ? Colors.white : Colors.black),
                        decoration: InputDecoration(labelText: "Địa chỉ Email",
                            border: const OutlineInputBorder()),
                        keyboardType: TextInputType.emailAddress,
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: addressController,
                        style: TextStyle(
                            color: isDark ? Colors.white : Colors.black),
                        decoration: InputDecoration(
                            labelText: "Địa chỉ thường trú",
                            border: const OutlineInputBorder()),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: countryController,
                        style: TextStyle(
                            color: isDark ? Colors.white : Colors.black),
                        decoration: InputDecoration(labelText: "Quốc tịch",
                            border: const OutlineInputBorder()),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: userController,
                        style: TextStyle(
                            color: isDark ? Colors.white : Colors.black),
                        decoration: InputDecoration(
                            labelText: "Tên đăng nhập *",
                            border: const OutlineInputBorder()),
                        enabled: !isEdit,
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: passController,
                        style: TextStyle(
                            color: isDark ? Colors.white : Colors.black),
                        decoration: InputDecoration(labelText: "Mật khẩu *",
                            border: const OutlineInputBorder()),
                        obscureText: true,
                      ),
                      if (isEdit) ...[
                        const SizedBox(height: 8),
                        SwitchListTile(
                          title: Text(
                              "Trạng thái tài khoản",
                              style: TextStyle(fontSize: 14,
                                  fontWeight: FontWeight.bold,
                                  color: isDark ? Colors.white70 : Colors
                                      .black87)
                          ),
                          value: currentStatus,
                          activeColor: goldAccent,
                          onChanged: (val) {
                            setDialogState(() => currentStatus = val);
                          },
                        ),
                      ]
                    ],
                  ),
                ),
                actions: [
                  TextButton(
                    onPressed: () => Navigator.pop(context),
                    child: const Text(
                        "Hủy", style: TextStyle(color: Colors.grey)),
                  ),
                  ElevatedButton(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: isDark ? goldAccent : navyPrimary,
                      // Đảo nút: Tối nút vàng, Sáng nút xanh
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(8)),
                    ),
                    onPressed: () async {
                      if (nameController.text
                          .trim()
                          .isEmpty ||
                          userController.text
                              .trim()
                              .isEmpty ||
                          (!isEdit && passController.text
                              .trim()
                              .isEmpty)) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text(
                              "Vui lòng điền đầy đủ các thông tin bắt buộc (*)!"),
                              backgroundColor: Colors.orange),
                        );
                        return;
                      }

                      final newUser = UserModel(
                        idTaiKhoan: isEdit ? user.idTaiKhoan : 0,
                        hoTen: nameController.text.trim(),
                        sdt: phoneController.text.trim(),
                        email: emailController.text.trim(),
                        diaChi: addressController.text.trim(),
                        quocTich: countryController.text.trim(),
                        tenDangNhap: userController.text.trim(),
                        matKhau: passController.text.trim(),
                        trangThai: currentStatus,
                      );

                      bool success = await apiProvider.saveUser(
                          newUser, isEdit: isEdit);
                      if (success && mounted) {
                        Navigator.pop(context);
                        _fetchUsers();
                      }
                    },
                    child: Text(
                        "Lưu",
                        style: TextStyle(
                            color: isDark ? navyPrimary : goldAccent,
                            fontWeight: FontWeight.bold)
                    ),
                  ),
                ],
              );
            },
          ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final bool isDark = Theme
        .of(context)
        .brightness == Brightness.dark;

    final Color dynamicScaffoldBg = isDark ? AppColors.primaryDark : AppColors
        .background;
    final Color dynamicCardBg = isDark ? const Color(0xFF242424) : AppColors
        .cardBg;
    final Color dynamicAppBarBg = isDark ? const Color(0xFF111111) : AppColors
        .primary;
    final Color dynamicTextColor = isDark ? Colors.white : AppColors.primary;
    final Color dynamicSubTextColor = isDark ? Colors.white60 : AppColors
        .textSub;

    return Scaffold(
      backgroundColor: dynamicScaffoldBg,
      appBar: AppBar(
        backgroundColor: dynamicAppBarBg,
        elevation: 0,
        title: const Text(
          "Quản lý Người dùng",
          style: TextStyle(
            fontWeight: FontWeight.bold,
            color: AppColors
                .accentGold,
          ),
        ),
        actions: [
          IconButton(
            onPressed: _fetchUsers,
            icon: const Icon(Icons.refresh, color: AppColors.accentGold),
          )
        ],
      ),
      body: isLoading
          ? const Center(child: CircularProgressIndicator(
          valueColor: AlwaysStoppedAnimation<Color>(AppColors.accentGold)))
          : users.isEmpty
          ? Center(child: Text("Hệ thống chưa ghi nhận khách hàng nào.",
          style: TextStyle(color: dynamicTextColor)))
          : ListView.builder(
        itemCount: users.length,
        itemBuilder: (context, index) {
          final u = users[index];
          return Card(
            color: dynamicCardBg,
            // Nền Card tự động co giãn theo Light/Dark Mode
            margin: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
            shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(10)),
            child: ListTile(
              leading: CircleAvatar(
                backgroundColor: isDark ? AppColors.accentGold : AppColors
                    .primary,
                child: Text(
                  u.hoTen.isNotEmpty ? u.hoTen[0].toUpperCase() : "?",
                  style: TextStyle(
                      color: isDark ? AppColors.primary : Colors.white,
                      fontWeight: FontWeight.bold
                  ),
                ),
              ),
              title: Text(
                u.hoTen,
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  color: isDark ? AppColors.accentGold : AppColors
                      .primary, // Ngày Đen, Đêm Vàng cực sang
                ),
              ),
              subtitle: Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(
                  "Username: ${u.tenDangNhap} \nSĐT: ${u.sdt.isNotEmpty
                      ? u.sdt
                      : 'Chưa cập nhật'} | QG: ${u.quocTich}",
                  style: TextStyle(color: dynamicSubTextColor),
                ),
              ),
              isThreeLine: true,
              trailing: Container(
                padding: const EdgeInsets.symmetric(
                    horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: u.trangThai ? Colors.green.withOpacity(0.15) : Colors
                      .red.withOpacity(0.15),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  u.trangThai ? "Hoạt động" : "Bị khóa",
                  style: TextStyle(
                      color: u.trangThai ? Colors.greenAccent : Colors
                          .redAccent,
                      fontSize: 11,
                      fontWeight: FontWeight.bold
                  ),
                ),
              ),
              onTap: () => _showUserDialog(user: u),
            ),
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        backgroundColor: isDark ? AppColors.accentGold : AppColors.primary,
        onPressed: () => _showUserDialog(),
        child: Icon(Icons.person_add_alt_1,
            color: isDark ? AppColors.primary : Colors.white),
      ),
    );
  }
}