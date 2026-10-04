// init_all_collections.js
db = db.getSiblingDB('MayHotelDB');

// Mảng chứa tên tất cả các collection theo Sơ đồ lớp (ERD & Class Diagram)
const collections = [
    "customers",            // KhachHang
    "customer_types",       // LoaiKhachHang (Mới)
    "booking_orders",       // PhieuDatPhong
    "booking_details",      // CT_DatPhong
    "rooms",                // Phong
    "room_types",           // LoaiPhong
    "reviews",              // DanhGia
    "invoices",             // HoaDon
    "services",             // DichVu
    "service_tickets",      // PhieuDichVu
    "service_ticket_details", // CT_DichVu (Mới)
    "promotions",           // ChuongTrinhKhuyenMai
    "promotion_types",      // LoaiKhuyenMai (Mới)
    "gift_vouchers",        // PhieuQuaTang
    "risk_alerts",          // CanhBaoRuiRo
    "roles",                // PhanQuyen
    "role_details",         // ChiTietQuyen (Mới)
    "accounts",             // TaiKhoan
    "staffs",               // NhanVien
    "backup_histories"      // QuanLySaoLuu
];

let created = 0;
let existed = 0;

collections.forEach(colName => {
    let colExists = db.getCollectionNames().indexOf(colName) > -1;
    if (!colExists) {
        db.createCollection(colName);
        print("Đã tạo mới collection: " + colName);
        created++;
    } else {
        existed++;
    }
});

print("=== TỔNG KẾT ===");
print("Tạo mới: " + created + " collections.");
print("Đã tồn tại: " + existed + " collections.");
print("Cơ sở dữ liệu MayHotelDB đã được đồng bộ 100% với Sơ đồ lớp!");
