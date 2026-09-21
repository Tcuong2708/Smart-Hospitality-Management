import 'package:flutter/material.dart';

class Responsive {
  static double width(BuildContext context) => MediaQuery.of(context).size.width;
  static double height(BuildContext context) => MediaQuery.of(context).size.height;

  // 🚀 CẢI TIẾN: Kiểm tra thiết bị có phải máy tính bảng hay không (Breakpoint 600px)
  static bool isTablet(BuildContext context) => width(context) >= 600;

  // Tính toán kích thước dựa trên tỷ lệ màn hình chuẩn
  static double sp(BuildContext context, double size) {
    double scaleFactor = isTablet(context) ? 1.2 : 1.0; // Tăng nhẹ kích thước font trên tablet
    return size * (width(context) / 375) * scaleFactor;
  }
}
