// fix_rooms.js
db = db.getSiblingDB('MayHotelDB');

// 1. Chuẩn hóa trạng thái (status)
db.rooms.updateMany({ status: { $in: ["Trống", "Vacant", "Available"] } }, { $set: { status: "Available" } });
db.rooms.updateMany({ status: "Reserved" }, { $set: { status: "Occupied" } });

// 2. Cập nhật view_direction và location_zone dựa trên số đuôi của mã phòng (name)
let rooms = db.rooms.find().toArray();
let bulkOps = [];

rooms.forEach(room => {
    let name = room.name || "";
    // Lấy 2 số cuối của tên phòng (ví dụ P101 -> 01, P720 -> 20, p2001 -> 01)
    let match = name.match(/(\d{2})$/);
    if (match) {
        let suffix = parseInt(match[1], 10);
        let viewDir = "";
        let locZone = "";
        
        if (suffix >= 1 && suffix <= 5) {
            viewDir = "CITY";
            locZone = "LEFT_WING";
        } else if (suffix >= 16 && suffix <= 20) {
            viewDir = "CITY";
            locZone = "RIGHT_WING";
        } else if (suffix >= 6 && suffix <= 10) {
            viewDir = "SEA";
            locZone = "LEFT_WING";
        } else if (suffix >= 11 && suffix <= 15) {
            viewDir = "SEA";
            locZone = "RIGHT_WING";
        } else {
            viewDir = "MOUNTAIN"; // Giả định các phòng khác (như đuôi 21, 22...)
            locZone = "CENTER";
        }

        bulkOps.push({
            updateOne: {
                filter: { _id: room._id },
                update: { $set: { view_direction: viewDir, location_zone: locZone } }
            }
        });
    }
});

if (bulkOps.length > 0) {
    let result = db.rooms.bulkWrite(bulkOps);
    print("Đã cập nhật hướng và khu vực cho " + result.modifiedCount + " phòng.");
} else {
    print("Không có phòng nào cần cập nhật hướng.");
}

// 3. Đảm bảo collection customers và booking_orders có sẵn để tránh lỗi khi lookup
db.createCollection("customers");
db.createCollection("booking_orders");
db.createCollection("booking_details");

print("Hoàn tất chuẩn hóa collection rooms.");
