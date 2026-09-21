// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:may_hotel_app/main.dart';

void main() {
  testWidgets('Counter increments smoke test', (WidgetTester tester) async {
    // Đổi MyApp thành MayHotelApp ở dòng dưới đây
    await tester.pumpWidget(const MayHotelApp());

    // Lưu ý: Các dòng kiểm tra counter (0, 1) bên dưới
    // có thể vẫn sẽ báo lỗi logic vì App của bạn giờ là màn hình Login,
    // không còn nút bấm tăng số nữa.
  });
}
