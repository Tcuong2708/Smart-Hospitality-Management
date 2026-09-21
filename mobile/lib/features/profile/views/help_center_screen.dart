import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/utils/responsive.dart';

class HelpCenterScreen extends StatelessWidget {
  const HelpCenterScreen({Key? key}) : super(key: key);

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
        // 🚀 ĐỒNG BỘ CHÍ MẠNG: Khóa màu Pin, Wifi và Đồng hồ khớp theo giao diện sáng/tối
        systemOverlayStyle: SystemUiOverlayStyle(
          statusBarColor: Colors.transparent,
          statusBarIconBrightness: isDark ? Brightness.light : Brightness.dark,
          statusBarBrightness: isDark ? Brightness.dark : Brightness.light,
        ),
        leading: BackButton(color: colorScheme.onSurface),
        title: Text(
          "Trung tâm trợ giúp",
          style: TextStyle(color: colorScheme.onSurface, fontWeight: FontWeight.bold),
        ),
        centerTitle: true,
      ),
      body: ListView(
        padding: EdgeInsets.symmetric(
          horizontal: Responsive.width(context) * 0.08,
          vertical: Responsive.sp(context, 10),
        ),
        children: [
          // --- PHÂN HỆ KHAY LIÊN HỆ ĐỒNG BỘ CAO CẤP ---
          _buildContactCard(
            context,
            icon: Icons.phone_in_talk_rounded,
            title: "Gọi cho lễ tân Tổng đài",
            subTitle: "Hotline phục vụ 24/7 nội bộ phòng",
            trailingText: "Phím số 0",
            onTap: () {
              // Có thể tích hợp url_launcher để gọi điện thoại thật nếu cần
            },
          ),
          _buildContactCard(
            context,
            icon: Icons.forum_rounded,
            title: "Chat trực tuyến với Khách sạn",
            subTitle: "Kết nối trực tiếp lễ tân đang trực",
            trailingText: "Phản hồi < 5p",
            onTap: () {},
          ),
          _buildContactCard(
            context,
            icon: Icons.alternate_email_rounded,
            title: "Gửi Email phản hồi đóng góp",
            subTitle: "Hỗ trợ thủ tục hóa đơn, hoàn tiền",
            trailingText: "support@mayhotel.vn",
            onTap: () {},
          ),

          SizedBox(height: Responsive.sp(context, 25)),

          // --- TIÊU ĐỀ KHỐI FAQ ---
          Padding(
            padding: const EdgeInsets.only(left: 4, bottom: 15),
            child: Text(
              "Câu hỏi thường gặp (FAQ)",
              style: TextStyle(
                fontSize: Responsive.sp(context, 17),
                fontWeight: FontWeight.bold,
                color: colorScheme.onSurface,
              ),
            ),
          ),

          // --- DANH SÁCH ACCORDION XỔ XUỐNG THỰC CHIẾN MƯỢT MÀ ---
          _buildFaqAccordion(
            context,
            question: "Làm sao để hủy lịch đặt phòng khách sạn?",
            answer: "Quý khách có thể thực hiện hủy phòng trực tiếp trên ứng dụng tại mục 'Lịch sử đặt phòng' trước 24 giờ so với giờ Check-in tiêu chuẩn để không phát sinh chi phí phụ thu.",
          ),
          _buildFaqAccordion(
            context,
            question: "Chính sách và thời gian hoàn tiền diễn ra như thế nào?",
            answer: "Số tiền hoàn lại sẽ được tự động chuyển về tài khoản ngân hàng hoặc ví điện tử quý khách đã dùng để thanh toán trong vòng 3 - 5 ngày làm việc tùy thuộc vào ngân hàng liên kết.",
          ),
          _buildFaqAccordion(
            context,
            question: "Thời gian Check-in và Check-out tiêu chuẩn của May Hotel?",
            answer: "Thời gian nhận phòng (Check-in) tiêu chuẩn là từ 14:00 giờ và thời gian trả phòng (Check-out) tiêu chuẩn là trước 12:00 giờ trưa hàng ngày.",
          ),
          _buildFaqAccordion(
            context,
            question: "Làm thế nào để sử dụng thẻ từ NFC mở khóa phòng?",
            answer: "Sau khi hoàn tất thủ tục Check-in tại quầy lễ tân, quý khách chỉ cần bật tính năng NFC trên điện thoại, áp lưng máy vào ổ khóa thông minh của phòng để kích hoạt mở cửa tự động.",
          ),

          SizedBox(height: Responsive.sp(context, 20)),
        ],
      ),
    );
  }

  // 🚀 WIDGET CON: KHAY LIÊN HỆ ĐỒNG BỘ CHUẨN ĐỘ BO V20 VÀ BÓNG ĐỔ TRANG LOGIN
  Widget _buildContactCard(
      BuildContext context, {
        required IconData icon,
        required String title,
        required String subTitle,
        required String trailingText,
        required VoidCallback onTap,
      }) {
    final colorScheme = Theme.of(context).colorScheme;
    final bool isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      margin: EdgeInsets.only(bottom: Responsive.sp(context, 14)),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(Responsive.sp(context, 20)), // Cùng độ bo v20 đồng bộ toàn hệ thống
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(isDark ? 0.3 : 0.05), // Khớp chính xác độ mờ bóng đổ dự án
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
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: colorScheme.primary.withOpacity(0.1),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(icon, color: colorScheme.primary, size: 24),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                          title,
                          style: TextStyle(fontWeight: FontWeight.bold, color: colorScheme.onSurface, fontSize: 14)
                      ),
                      const SizedBox(height: 4),
                      Text(
                          subTitle,
                          style: TextStyle(color: colorScheme.onSurface.withOpacity(0.5), fontSize: 12)
                      ),
                      const SizedBox(height: 4),
                      Text(
                          trailingText,
                          style: TextStyle(color: colorScheme.primary, fontWeight: FontWeight.bold, fontSize: 11)
                      ),
                    ],
                  ),
                ),
                Icon(Icons.arrow_forward_ios_rounded, color: colorScheme.onSurface.withOpacity(0.3), size: 14),
              ],
            ),
          ),
        ),
      ),
    );
  }

  // 🚀 WIDGET CON: ACCORDION FAQ XỔ XUỐNG CAO CẤP KHÔNG BỊ RÁC CHỈ MỤC DIVIDER
  Widget _buildFaqAccordion(BuildContext context, {required String question, required String answer}) {
    final colorScheme = Theme.of(context).colorScheme;
    final bool isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      margin: EdgeInsets.only(bottom: Responsive.sp(context, 12)),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(Responsive.sp(context, 20)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(isDark ? 0.3 : 0.04),
            blurRadius: 8,
            offset: const Offset(0, 4),
          )
        ],
      ),
      child: Theme(
        // Xóa sạch các vệt gạch ngang mặc định cực xấu của Flutter ExpansionTile
        data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
        child: ExpansionTile(
          iconColor: colorScheme.primary,
          collapsedIconColor: colorScheme.onSurface.withOpacity(0.4),
          tilePadding: const EdgeInsets.symmetric(horizontal: 18, vertical: 4),
          childrenPadding: const EdgeInsets.only(left: 18, right: 18, bottom: 18),
          title: Text(
            question,
            style: TextStyle(
              fontWeight: FontWeight.w600,
              fontSize: 14,
              color: colorScheme.onSurface,
            ),
          ),
          children: [
            Align(
              alignment: Alignment.centerLeft,
              child: Text(
                answer,
                style: TextStyle(
                  color: colorScheme.onSurface.withOpacity(0.6),
                  height: 1.5,
                  fontSize: 13,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}