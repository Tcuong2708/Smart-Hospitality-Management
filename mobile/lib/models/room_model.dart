import 'package:flutter/cupertino.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

class RoomModel {
  final int maPhong;
  final String tenPhong;
  final int maTrangThai;
  final double price;
  final String imageUrl;
  final String detail;
  final int availableCount;



  RoomModel({
    required this.maPhong,
    required this.tenPhong,
    required this.maTrangThai,
    required this.price,
    required this.imageUrl,
    required this.detail,
    this.availableCount = 0,
  });



  factory RoomModel.fromJson(Map<String, dynamic> json) {
    String rawImg = (json['imageUrl'] ?? json['ImageUrl'] ?? "").toString();
    String processedImg;

    if (rawImg.startsWith('http')) {
      processedImg = rawImg;
    } else if (rawImg.isNotEmpty) {
      final String baseUrl = dotenv.env['BASE_URL'] ?? "http://10.0.2.2:8080";
      processedImg = baseUrl + "/images/" + rawImg;
    } else {
      processedImg = "https://via.placeholder.com/150";
    }

    return RoomModel(
      maPhong: json['id'] ?? json['Id'] ?? json['ID'] ?? 0,
      tenPhong: (json['name'] ?? json['Name'] ?? json['tenPhong'] ?? "N/A").toString(),
      maTrangThai: json['maTrangThai'] ?? json['MaTrangThai'] ?? 1,
      price: (json['price'] ?? json['Price'] ?? 0.0).toDouble(),
      imageUrl: processedImg,
      detail: json['detail'] ?? json['Detail'] ?? "",
      availableCount: json['availableCount'] ?? json['AvailableCount'] ?? 0,
    );
  }
}