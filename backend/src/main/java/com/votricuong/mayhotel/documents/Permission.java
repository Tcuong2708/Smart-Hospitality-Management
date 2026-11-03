package com.votricuong.mayhotel.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "permissions")
public class Permission {
    @Id
    private String code;
    private String module;
    private String action;
    private String name;
    private String platform;
    private int sortOrder;

    @Transient
    public String getModuleDisplayName() {
        if (module == null) return "Chức năng hệ thống";
        return switch (module) {
            case "SYSTEM.PERMISSIONS" -> "Phân quyền và vai trò";
            case "SYSTEM.USERS" -> "Quản lý tài khoản";
            case "SYSTEM.BACKUP" -> "Sao lưu và phục hồi";
            case "MANAGER.ROOMS" -> "Quản lý phòng";
            case "MANAGER.ROOM_TYPES" -> "Quản lý loại phòng";
            case "MANAGER.SERVICES" -> "Quản lý dịch vụ";
            case "MANAGER.STAFF" -> "Quản lý nhân viên";
            case "MANAGER.CUSTOMERS" -> "Quản lý khách hàng";
            case "MANAGER.PROMOTIONS" -> "Quản lý khuyến mãi";
            case "MANAGER.REVIEWS" -> "Quản lý đánh giá";
            case "MANAGER.LOYALTY" -> "Quản lý điểm và hạng";
            case "REPORT.REVENUE" -> "Báo cáo doanh thu";
            case "RECEPTION.ROOM_MAP" -> "Sơ đồ phòng";
            case "RECEPTION.CHECK_IN" -> "Nhận phòng";
            case "RECEPTION.CHECK_OUT" -> "Trả phòng";
            case "RECEPTION.BOOKINGS" -> "Quản lý đặt phòng";
            case "RECEPTION.INVOICES" -> "Quản lý hóa đơn";
            case "RECEPTION.NO_SHOW" -> "Dự đoán khách không đến";
            case "MOBILE.HOUSEKEEPING" -> "Nghiệp vụ buồng phòng";
            case "MOBILE.SERVICE" -> "Nghiệp vụ nhân viên dịch vụ";
            default -> "Chức năng hệ thống";
        };
    }

    @Transient
    public String getActionDisplayName() {
        if (action == null) return "Thao tác";
        return switch (action) {
            case "VIEW" -> "Xem";
            case "CREATE" -> "Thêm mới";
            case "UPDATE" -> "Cập nhật";
            case "DELETE" -> "Xóa";
            case "PRINT" -> "In";
            case "EXPORT" -> "Xuất báo cáo";
            default -> "Thao tác";
        };
    }

    @Transient
    public String getPlatformDisplayName() {
        return "MOBILE".equalsIgnoreCase(platform) ? "Ứng dụng di động" : "Trang web";
    }

    @Transient
    public String getDisplayPath() {
        return getModuleDisplayName() + " · " + getActionDisplayName() + " · " + getPlatformDisplayName();
    }
}
