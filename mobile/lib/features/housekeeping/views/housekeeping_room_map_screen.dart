import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../models/housekeeping_room.dart';
import '../services/housekeeping_api_service.dart';

class HousekeepingRoomMapScreen extends StatefulWidget {
  const HousekeepingRoomMapScreen({super.key});

  @override
  State<HousekeepingRoomMapScreen> createState() => _HousekeepingRoomMapScreenState();
}

class _HousekeepingRoomMapScreenState extends State<HousekeepingRoomMapScreen> {
  final HousekeepingApiService _api = HousekeepingApiService();
  final Set<int> _updatingRoomIds = <int>{};
  List<HousekeepingRoom> _rooms = const [];
  bool _loading = true;
  String? _error;
  String _filter = 'ALL';

  @override
  void initState() {
    super.initState();
    _loadRooms();
  }

  Future<void> _loadRooms() async {
    if (mounted) setState(() { _loading = true; _error = null; });
    try {
      final rooms = await _api.getRooms();
      if (mounted) setState(() { _rooms = rooms; _loading = false; });
    } catch (error) {
      if (mounted) {
        setState(() {
          _loading = false;
          _error = HousekeepingApiService.errorMessage(error);
        });
      }
    }
  }

  List<HousekeepingRoom> get _visibleRooms {
    if (_filter == 'ALL') return _rooms;
    return _rooms.where((room) => room.status == _filter).toList();
  }

  int _count(String status) => _rooms.where((room) => room.status == status).length;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      appBar: AppBar(
        title: const Text('Sơ đồ buồng phòng', style: TextStyle(fontWeight: FontWeight.bold)),
        centerTitle: true,
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        actions: [IconButton(onPressed: _loadRooms, icon: const Icon(Icons.refresh_rounded))],
      ),
      body: Column(
        children: [
          _buildSummary(),
          _buildFilter(),
          Expanded(child: _buildBody()),
        ],
      ),
    );
  }

  Widget _buildSummary() {
    return Container(
      color: Colors.white,
      padding: const EdgeInsets.fromLTRB(12, 14, 12, 10),
      child: Row(
        children: [
          _summaryItem('Chờ kiểm tra', _count('CLEANING'), Colors.orange),
          _summaryItem('Sẵn sàng', _count('VACANT'), Colors.green),
          _summaryItem('Bảo trì', _count('MAINTENANCE'), Colors.blueGrey),
        ],
      ),
    );
  }

  Widget _summaryItem(String label, int value, Color color) {
    return Expanded(
      child: Column(
        children: [
          Text('$value', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: color)),
          Text(label, textAlign: TextAlign.center, style: const TextStyle(fontSize: 11, color: Colors.black54)),
        ],
      ),
    );
  }

  Widget _buildFilter() {
    const filters = <String, String>{
      'ALL': 'Tất cả',
      'CLEANING': 'Chờ kiểm tra',
      'MAINTENANCE': 'Bảo trì',
      'VACANT': 'Sẵn sàng',
    };
    return SizedBox(
      height: 54,
      child: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        scrollDirection: Axis.horizontal,
        children: filters.entries.map((entry) {
          final selected = _filter == entry.key;
          return Padding(
            padding: const EdgeInsets.only(right: 8),
            child: ChoiceChip(
              label: Text(entry.value),
              selected: selected,
              onSelected: (_) => setState(() => _filter = entry.key),
              selectedColor: AppColors.primary,
              labelStyle: TextStyle(color: selected ? Colors.white : Colors.black87),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildBody() {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.cloud_off_rounded, size: 52, color: Colors.grey),
              const SizedBox(height: 12),
              Text(_error!, textAlign: TextAlign.center),
              const SizedBox(height: 12),
              FilledButton.icon(onPressed: _loadRooms, icon: const Icon(Icons.refresh), label: const Text('Thử lại')),
            ],
          ),
        ),
      );
    }
    final rooms = _visibleRooms;
    if (rooms.isEmpty) return const Center(child: Text('Không có phòng phù hợp'));
    return RefreshIndicator(
      onRefresh: _loadRooms,
      child: LayoutBuilder(
        builder: (context, constraints) {
          final columns = constraints.maxWidth >= 900 ? 5 : constraints.maxWidth >= 600 ? 4 : 2;
          return GridView.builder(
            padding: const EdgeInsets.all(12),
            gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: columns,
              crossAxisSpacing: 12,
              mainAxisSpacing: 12,
              childAspectRatio: 0.9,
            ),
            itemCount: rooms.length,
            itemBuilder: (context, index) => _roomCard(rooms[index]),
          );
        },
      ),
    );
  }

  Widget _roomCard(HousekeepingRoom room) {
    final color = _statusColor(room.status);
    final actionable = room.status == 'CLEANING' || room.status == 'MAINTENANCE';
    final updating = _updatingRoomIds.contains(room.id);
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(16),
      elevation: 1,
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: actionable && !updating ? () => _showRoomActions(room) : null,
        child: Container(
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border(left: BorderSide(color: color, width: 7)),
          ),
          padding: const EdgeInsets.all(13),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(_statusIcon(room.status), color: color),
                  const Spacer(),
                  if (updating) const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2)),
                ],
              ),
              const Spacer(),
              Text(room.name, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              Text(room.roomType, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(color: Colors.black54)),
              const SizedBox(height: 8),
              Text(room.statusLabel, style: TextStyle(color: color, fontWeight: FontWeight.w700)),
              if (room.statusNote?.isNotEmpty == true) ...[
                const SizedBox(height: 4),
                Text(room.statusNote!, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 11, color: Colors.black54)),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _showRoomActions(HousekeepingRoom room) async {
    if (room.status == 'MAINTENANCE') {
      final confirmed = await showDialog<bool>(
        context: context,
        builder: (context) => AlertDialog(
          title: Text('Hoàn tất bảo trì ${room.name}?'),
          content: const Text('Phòng sẽ được chuyển về trạng thái trống và sẵn sàng nhận khách.'),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Hủy')),
            FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Hoàn tất')),
          ],
        ),
      );
      if (confirmed == true) await _updateRoom(room, 'VACANT', 'Đã hoàn tất bảo trì');
      return;
    }

    final action = await showModalBottomSheet<String>(
      context: context,
      showDragHandle: true,
      builder: (context) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 4, 20, 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(room.name, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
              const SizedBox(height: 6),
              const Text('Chọn kết quả sau khi kiểm tra phòng.'),
              const SizedBox(height: 18),
              FilledButton.icon(
                onPressed: () => Navigator.pop(context, 'VACANT'),
                icon: const Icon(Icons.check_circle_outline),
                label: const Text('Đã kiểm tra/dọn xong'),
              ),
              const SizedBox(height: 10),
              OutlinedButton.icon(
                onPressed: () => Navigator.pop(context, 'MAINTENANCE'),
                icon: const Icon(Icons.build_outlined),
                label: const Text('Báo phòng cần bảo trì'),
              ),
            ],
          ),
        ),
      ),
    );
    if (action == 'VACANT') {
      await _updateRoom(room, 'VACANT', 'Đã kiểm tra và dọn phòng xong');
    } else if (action == 'MAINTENANCE' && mounted) {
      final note = await _askMaintenanceNote(room);
      if (note != null) await _updateRoom(room, 'MAINTENANCE', note);
    }
  }

  Future<String?> _askMaintenanceNote(HousekeepingRoom room) async {
    final controller = TextEditingController();
    final result = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Báo bảo trì ${room.name}'),
        content: TextField(
          controller: controller,
          maxLength: 500,
          maxLines: 4,
          autofocus: true,
          decoration: const InputDecoration(
            labelText: 'Nội dung cần bảo trì *',
            hintText: 'Ví dụ: Điều hòa không hoạt động',
            border: OutlineInputBorder(),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Hủy')),
          FilledButton(
            onPressed: () {
              final note = controller.text.trim();
              if (note.isNotEmpty) Navigator.pop(context, note);
            },
            child: const Text('Xác nhận'),
          ),
        ],
      ),
    );
    controller.dispose();
    return result;
  }

  Future<void> _updateRoom(HousekeepingRoom room, String status, String note) async {
    setState(() => _updatingRoomIds.add(room.id));
    try {
      final updated = await _api.updateStatus(room: room, status: status, statusNote: note);
      if (!mounted) return;
      setState(() {
        _rooms = _rooms.map((item) => item.id == updated.id ? updated : item).toList();
      });
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('${updated.name}: ${updated.statusLabel}')));
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(HousekeepingApiService.errorMessage(error))));
      await _loadRooms();
    } finally {
      if (mounted) setState(() => _updatingRoomIds.remove(room.id));
    }
  }

  Color _statusColor(String status) => switch (status) {
    'VACANT' => Colors.green,
    'OCCUPIED' => Colors.red,
    'RESERVED' => Colors.blue,
    'CLEANING' => Colors.orange,
    'MAINTENANCE' => Colors.blueGrey,
    _ => Colors.grey,
  };

  IconData _statusIcon(String status) => switch (status) {
    'CLEANING' => Icons.cleaning_services_rounded,
    'MAINTENANCE' => Icons.build_circle_outlined,
    'OCCUPIED' => Icons.person_rounded,
    'RESERVED' => Icons.event_available_rounded,
    _ => Icons.meeting_room_rounded,
  };
}
