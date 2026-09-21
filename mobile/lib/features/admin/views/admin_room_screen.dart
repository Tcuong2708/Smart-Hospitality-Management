import 'package:flutter/material.dart';
import 'package:may_hotel_app/models/room_model.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';

class AdminRoomScreen extends StatefulWidget {
  const AdminRoomScreen({super.key});

  @override
  State<AdminRoomScreen> createState() => _AdminRoomScreenState();
}

class _AdminRoomScreenState extends State<AdminRoomScreen> {
  final HotelApiProvider apiProvider = HotelApiProvider();
  List<RoomModel> rooms = [];
  bool isLoading = true;

  @override
  void initState() {
    super.initState();
    _fetchRooms();
  }

  // Lấy danh sách phòng từ Backend
  Future<void> _fetchRooms() async {
    setState(() => isLoading = true);
    try {
      final data = await apiProvider.getAdminRooms();
      setState(() {
        rooms = data;
        isLoading = false;
      });
    } catch (e) {
      setState(() => isLoading = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text("Lỗi tải danh sách: $e")),
      );
    }
  }

  // Hàm xóa phòng
  void _deleteRoom(int id) async {
    bool confirm = await showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text("Xác nhận xóa"),
        content: const Text("Bạn có chắc chắn muốn xóa phòng này không?"),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text("Hủy")),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text("Xóa", style: TextStyle(color: Colors.red))),
        ],
      ),
    );

    if (confirm == true) {
      bool success = await apiProvider.deleteRoom(id);
      if (success) {
        _fetchRooms(); // Load lại danh sách
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Đã xóa phòng thành công")));
      }
    }
  }

  // Hàm hiển thị Form Thêm/Sửa
  void _showRoomDialog({RoomModel? room}) {
    final isEdit = room != null;
    final nameController = TextEditingController(text: isEdit ? room.tenPhong : "");
    final priceController = TextEditingController(text: isEdit ? room.price.toString() : "");
    final detailController = TextEditingController(text: isEdit ? room.detail : "");
    final imageController = TextEditingController(text: isEdit ? room.imageUrl : "");

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: Text(isEdit ? "Chỉnh sửa phòng" : "Thêm phòng mới"),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(controller: nameController, decoration: const InputDecoration(labelText: "Tên phòng")),
              TextField(controller: priceController, decoration: const InputDecoration(labelText: "Giá phòng"), keyboardType: TextInputType.number),
              TextField(controller: detailController, decoration: const InputDecoration(labelText: "Mô tả")),
              TextField(controller: imageController, decoration: const InputDecoration(labelText: "Link ảnh (URL)")),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text("Hủy")),
          ElevatedButton(
            onPressed: () async {
              Map<String, dynamic> roomData = {
                "Name": nameController.text,
                "Price": double.tryParse(priceController.text) ?? 0,
                "Detail": detailController.text,
                "ImageUrl": imageController.text,
                "MaTrangThai": isEdit ? room.maTrangThai : 1,
              };

              bool success;
              if (isEdit) {
                success = await apiProvider.updateRoom(room.maPhong, roomData);
              } else {
                success = await apiProvider.addRoom(roomData);
              }

              if (success && context.mounted) {
                Navigator.pop(context);
                _fetchRooms();
              }
            },
            child: const Text("Lưu"),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Quản lý danh mục phòng"),
        actions: [
          IconButton(onPressed: _fetchRooms, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: isLoading
          ? const Center(child: CircularProgressIndicator())
          : ListView.builder(
        itemCount: rooms.length,
        itemBuilder: (context, index) {
          final r = rooms[index];
          return Card(
            margin: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
            child: ListTile(
              leading: Container(
                width: 50,
                height: 50,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(8),
                  image: DecorationImage(
                    image: NetworkImage(r.imageUrl.isNotEmpty ? r.imageUrl : "https://via.placeholder.com/150"),
                    fit: BoxFit.cover,
                  ),
                ),
              ),
              title: Text(r.tenPhong, style: const TextStyle(fontWeight: FontWeight.bold)),
              subtitle: Text("${r.price.toStringAsFixed(0)} đ - ${r.detail}"),
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  IconButton(icon: const Icon(Icons.edit, color: Colors.blue), onPressed: () => _showRoomDialog(room: r)),
                  IconButton(icon: const Icon(Icons.delete, color: Colors.red), onPressed: () => _deleteRoom(r.maPhong)),
                ],
              ),
            ),
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showRoomDialog(),
        child: const Icon(Icons.add),
      ),
    );
  }
}