class HousekeepingRoom {
  final int id;
  final String name;
  final String roomType;
  final int capacity;
  final String status;
  final String statusLabel;
  final String? statusNote;
  final DateTime? statusUpdatedAt;
  final bool hasCleaningOrder;
  final DateTime? cleaningRequestedAt;

  const HousekeepingRoom({
    required this.id,
    required this.name,
    required this.roomType,
    required this.capacity,
    required this.status,
    required this.statusLabel,
    this.statusNote,
    this.statusUpdatedAt,
    this.hasCleaningOrder = false,
    this.cleaningRequestedAt,
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
      hasCleaningOrder: json['hasCleaningOrder'] == true ||
          json['hasCleaningOrder']?.toString().toLowerCase() == 'true',
      cleaningRequestedAt: DateTime.tryParse(
        json['cleaningRequestedAt']?.toString() ?? '',
      ),
    );
  }
}

class HousekeepingRoomSnapshot {
  final List<HousekeepingRoom> rooms;
  final DateTime hotelDate;
  final DateTime? generatedAt;

  const HousekeepingRoomSnapshot({
    required this.rooms,
    required this.hotelDate,
    this.generatedAt,
  });
}
