class HousekeepingRoom {
  final int id;
  final String name;
  final String roomType;
  final int capacity;
  final String status;
  final String statusLabel;
  final String? statusNote;
  final DateTime? statusUpdatedAt;

  const HousekeepingRoom({
    required this.id,
    required this.name,
    required this.roomType,
    required this.capacity,
    required this.status,
    required this.statusLabel,
    this.statusNote,
    this.statusUpdatedAt,
  });

  factory HousekeepingRoom.fromJson(Map<String, dynamic> json) {
    return HousekeepingRoom(
      id: int.tryParse(json['id']?.toString() ?? '') ?? 0,
      name: json['name']?.toString() ?? 'Phòng',
      roomType: json['roomType']?.toString() ?? 'Chưa phân loại',
      capacity: int.tryParse(json['capacity']?.toString() ?? '') ?? 2,
      status: json['status']?.toString().toUpperCase() ?? 'VACANT',
      statusLabel: json['statusLabel']?.toString() ?? 'Trống',
      statusNote: json['statusNote']?.toString(),
      statusUpdatedAt: DateTime.tryParse(json['statusUpdatedAt']?.toString() ?? ''),
    );
  }
}
