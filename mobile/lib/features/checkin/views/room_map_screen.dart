import 'dart:async';

import 'package:flutter/material.dart';

import '../../../models/room_model.dart';
import '../../../services/hotel_api_provider.dart';

class RoomMapScreen extends StatefulWidget {
  const RoomMapScreen({super.key});

  @override
  State<RoomMapScreen> createState() => _RoomMapScreenState();
}

class _RoomMapScreenState extends State<RoomMapScreen> {
  final HotelApiProvider _apiProvider = HotelApiProvider();
  List<RoomModel> _rooms = const [];
  bool _isLoading = true;
  Timer? _refreshTimer;

  @override
  void initState() {
    super.initState();
    _loadRooms();
    _refreshTimer = Timer.periodic(
      const Duration(seconds: 5),
      (_) => _loadRooms(showLoading: false),
    );
  }

  @override
  void dispose() {
    _refreshTimer?.cancel();
    super.dispose();
  }

  Future<void> _loadRooms({bool showLoading = true}) async {
    if (!mounted) return;
    if (showLoading) setState(() => _isLoading = true);
    try {
      final rooms = await _apiProvider.getAllRooms();
      if (!mounted) return;
      if (!showLoading && rooms.isEmpty && _rooms.isNotEmpty) return;
      setState(() {
        _rooms = rooms;
        _isLoading = false;
      });
    } catch (_) {
      if (mounted && showLoading) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final vacant = _rooms.where((room) => room.maTrangThai == 1).length;
    final occupied = _rooms.where((room) => room.maTrangThai == 2).length;
    final cleaning = _rooms.where((room) => room.maTrangThai == 3).length;
    return Scaffold(
      backgroundColor: const Color(0xFFF5F7F9),
      appBar: AppBar(
        title: const Text(
          'Sơ đồ phòng',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        centerTitle: true,
        actions: [
          IconButton(
            tooltip: 'Tải lại',
            onPressed: () => _loadRooms(),
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      body: Column(
        children: [
          _summary(vacant, occupied, cleaning),
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : _rooms.isEmpty
                ? const Center(child: Text('Chưa tải được dữ liệu phòng.'))
                : RefreshIndicator(
                    onRefresh: () => _loadRooms(),
                    child: GridView.builder(
                      padding: const EdgeInsets.all(16),
                      gridDelegate:
                          const SliverGridDelegateWithFixedCrossAxisCount(
                            crossAxisCount: 3,
                            crossAxisSpacing: 12,
                            mainAxisSpacing: 12,
                            childAspectRatio: 0.78,
                          ),
                      itemCount: _rooms.length,
                      itemBuilder: (_, index) => _roomCard(_rooms[index]),
                    ),
                  ),
          ),
        ],
      ),
    );
  }

  Widget _summary(int vacant, int occupied, int cleaning) {
    final now = DateTime.now();
    final date =
        '${now.day.toString().padLeft(2, '0')}/${now.month.toString().padLeft(2, '0')}/${now.year}';
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 16),
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(bottom: Radius.circular(20)),
      ),
      child: Column(
        children: [
          Text(
            'Trạng thái hiện tại · $date · tự đồng bộ mỗi 5 giây',
            style: const TextStyle(
              color: Colors.blueGrey,
              fontSize: 12,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _summaryItem('Trống', vacant, Colors.green),
              _summaryItem('Đang ở', occupied, Colors.red),
              _summaryItem('Chờ dọn', cleaning, Colors.orange),
            ],
          ),
        ],
      ),
    );
  }

  Widget _summaryItem(String label, int count, Color color) => Column(
    children: [
      Text(
        '$count',
        style: TextStyle(
          color: color,
          fontSize: 22,
          fontWeight: FontWeight.bold,
        ),
      ),
      Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
    ],
  );

  Widget _roomCard(RoomModel room) {
    final color = _statusColor(room.maTrangThai);
    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: () => _showRoomDetails(room),
      child: Ink(
        padding: const EdgeInsets.all(9),
        decoration: BoxDecoration(
          color: color,
          borderRadius: BorderRadius.circular(16),
          boxShadow: const [
            BoxShadow(
              color: Color(0x1A000000),
              blurRadius: 5,
              offset: Offset(0, 2),
            ),
          ],
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              room.statusLabel.toUpperCase(),
              textAlign: TextAlign.center,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                color: Colors.white70,
                fontSize: 9,
                fontWeight: FontWeight.bold,
              ),
            ),
            Text(
              room.tenPhong,
              textAlign: TextAlign.center,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 17,
                fontWeight: FontWeight.bold,
              ),
            ),
            Text(
              room.roomType.isEmpty ? 'Chưa phân loại' : room.roomType,
              textAlign: TextAlign.center,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(color: Colors.white70, fontSize: 10),
            ),
            Text(
              'Tối đa ${room.maxOccupancy} khách',
              style: const TextStyle(color: Colors.white70, fontSize: 10),
            ),
          ],
        ),
      ),
    );
  }

  void _showRoomDetails(RoomModel room) {
    showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(room.tenPhong),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Trạng thái: ${room.statusLabel}'),
            Text(
              'Loại phòng: ${room.roomType.isEmpty ? 'Chưa phân loại' : room.roomType}',
            ),
            Text('Sức chứa: ${room.maxOccupancy} khách'),
            if (room.statusNote.isNotEmpty) Text('Ghi chú: ${room.statusNote}'),
            const SizedBox(height: 12),
            const Text(
              'Sơ đồ chỉ dùng để theo dõi. Nhận phòng, đổi phòng và trả phòng được thực hiện tại mục Check-in để dữ liệu luôn đồng bộ.',
              style: TextStyle(color: Colors.blueGrey),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('Đóng'),
          ),
        ],
      ),
    );
  }

  Color _statusColor(int status) => switch (status) {
    1 => const Color(0xFF278C5A),
    2 => const Color(0xFFD9364E),
    3 => const Color(0xFFF3A712),
    4 => const Color(0xFF6B7280),
    _ => const Color(0xFF607D8B),
  };
}
