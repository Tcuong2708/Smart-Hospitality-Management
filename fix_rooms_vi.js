// fix_rooms_vi.js
db = db.getSiblingDB('MayHotelDB');

// 1. Chuyển đổi Trạng thái (status)
db.rooms.updateMany({ status: "Available" }, { $set: { status: "Trống" } });
db.rooms.updateMany({ status: "Occupied" }, { $set: { status: "Đang ở" } });

// 2. Chuyển đổi Hướng nhìn (view_direction)
db.rooms.updateMany({ view_direction: "CITY" }, { $set: { view_direction: "Hướng Thành Phố" } });
db.rooms.updateMany({ view_direction: "SEA" }, { $set: { view_direction: "Hướng Biển" } });
db.rooms.updateMany({ view_direction: "MOUNTAIN" }, { $set: { view_direction: "Hướng Núi" } });

// 3. Chuyển đổi Khu vực (location_zone)
db.rooms.updateMany({ location_zone: "LEFT_WING" }, { $set: { location_zone: "Cánh Trái" } });
db.rooms.updateMany({ location_zone: "RIGHT_WING" }, { $set: { location_zone: "Cánh Phải" } });
db.rooms.updateMany({ location_zone: "CENTER" }, { $set: { location_zone: "Khu Trung Tâm" } });

print("Đã chuyển đổi toàn bộ dữ liệu trạng thái và hướng phòng sang tiếng Việt!");
