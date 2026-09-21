import 'package:flutter/material.dart';
import '../models/room_model.dart';

class RoomDetailScreen extends StatelessWidget {
  final RoomModel room;
  const RoomDetailScreen({Key? key, required this.room}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(room.tenPhong), // Đã sửa: name -> tenPhong
        centerTitle: true,
      ),
      body: SingleChildScrollView( // Thêm để tránh lỗi tràn màn hình khi mô tả quá dài
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Hiệu ứng Hero bay từ HomeScreen sang
            Hero(
              tag: room.maPhong.toString(), // Chuyển id (int) sang String cho Hero tag
              child: room.imageUrl.startsWith('http')
                  ? Image.network(room.imageUrl, fit: BoxFit.cover, height: 300, width: double.infinity)
                  : Image.asset(room.imageUrl, fit: BoxFit.cover, height: 300, width: double.infinity),
            ),
            Padding(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Tên phòng và Trạng thái
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(room.tenPhong, style: const TextStyle(fontSize: 28, fontWeight: FontWeight.bold)),
                      _buildStatusChip(room.maTrangThai),
                    ],
                  ),
                  const SizedBox(height: 10),

                  // Giá tiền
                  Text(
                      "${room.price.toStringAsFixed(0)} VNĐ / Đêm",
                      style: const TextStyle(fontSize: 22, color: Colors.blue, fontWeight: FontWeight.w600)
                  ),
                  const Divider(height: 40, thickness: 1.2),

                  // Mô tả chi tiết
                  const Text("Mô tả phòng", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 10),
                  Text(
                    room.detail, // Đã sửa: description -> detail
                    style: TextStyle(fontSize: 16, color: Colors.grey[700], height: 1.5),
                  ),

                  const SizedBox(height: 30),

                  // NÚT BẤM HÀNH ĐỘNG (Phù hợp với nhiệm vụ CRUD & Check-in của Cường)
                  SizedBox(
                    width: double.infinity,
                    height: 50,
                    child: ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: room.maTrangThai == 1 ? Colors.green : Colors.blueGrey,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                      onPressed: () {
                        // Logic điều hướng tùy theo trạng thái
                        if (room.maTrangThai == 1) {
                          // TODO: Mở màn hình Check-in
                        }
                      },
                      child: Text(
                        room.maTrangThai == 1 ? "ĐẶT PHÒNG NGAY" : "XEM HÓA ĐƠN",
                        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  // Widget hiển thị nhãn trạng thái
  Widget _buildStatusChip(int status) {
    String label = status == 1 ? "Trống" : (status == 2 ? "Có khách" : "Đang dọn");
    Color color = status == 1 ? Colors.green : (status == 2 ? Colors.red : Colors.orange);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(color: color.withOpacity(0.2), borderRadius: BorderRadius.circular(20)),
      child: Text(label, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 12)),
    );
  }
}