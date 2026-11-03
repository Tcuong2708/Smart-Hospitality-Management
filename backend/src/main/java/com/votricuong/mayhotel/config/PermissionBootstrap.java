package com.votricuong.mayhotel.config;

import com.votricuong.mayhotel.documents.Permission;
import com.votricuong.mayhotel.documents.Role;
import com.votricuong.mayhotel.documents.RolePermission;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.repositories.PermissionRepository;
import com.votricuong.mayhotel.repositories.RolePermissionRepository;
import com.votricuong.mayhotel.repositories.RoleRepository;
import com.votricuong.mayhotel.repositories.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component
@Order(30)
public class PermissionBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PermissionBootstrap.class);
    private static final String SUPER_ADMIN_USERNAME = "superadmin";
    private static final Map<String, String> MODULES = new LinkedHashMap<>();
    static {
        MODULES.put("SYSTEM.PERMISSIONS", "Phân quyền vai trò");
        MODULES.put("SYSTEM.USERS", "Quản lý tài khoản");
        MODULES.put("SYSTEM.BACKUP", "Sao lưu và phục hồi");
        MODULES.put("MANAGER.ROOMS", "Quản lý phòng");
        MODULES.put("MANAGER.ROOM_TYPES", "Quản lý loại phòng");
        MODULES.put("MANAGER.SERVICES", "Quản lý dịch vụ");
        MODULES.put("MANAGER.STAFF", "Quản lý nhân viên");
        MODULES.put("MANAGER.CUSTOMERS", "Quản lý khách hàng");
        MODULES.put("MANAGER.PROMOTIONS", "Quản lý khuyến mãi");
        MODULES.put("MANAGER.REVIEWS", "Quản lý đánh giá");
        MODULES.put("MANAGER.LOYALTY", "Quản lý điểm và hạng");
        MODULES.put("REPORT.REVENUE", "Thống kê doanh thu");
        MODULES.put("RECEPTION.ROOM_MAP", "Sơ đồ phòng");
        MODULES.put("RECEPTION.CHECK_IN", "Nhận phòng");
        MODULES.put("RECEPTION.CHECK_OUT", "Trả phòng");
        MODULES.put("RECEPTION.BOOKINGS", "Quản lý đặt phòng");
        MODULES.put("RECEPTION.INVOICES", "Quản lý hóa đơn");
        MODULES.put("RECEPTION.NO_SHOW", "Dự đoán khách không đến");
        MODULES.put("MOBILE.HOUSEKEEPING", "Mobile buồng phòng");
        MODULES.put("MOBILE.SERVICE", "Mobile nhân viên dịch vụ");
    }
    private static final List<String> ACTIONS = List.of("VIEW", "CREATE", "UPDATE", "DELETE", "PRINT", "EXPORT");

    private final PermissionRepository permissions;
    private final RolePermissionRepository rolePermissions;
    private final RoleRepository roles;
    private final UserRepository users;

    public PermissionBootstrap(PermissionRepository permissions, RolePermissionRepository rolePermissions,
                               RoleRepository roles, UserRepository users) {
        this.permissions = permissions;
        this.rolePermissions = rolePermissions;
        this.roles = roles;
        this.users = users;
    }

    @Override
    public void run(ApplicationArguments args) {
        roles.findById(8L).orElseGet(() -> roles.save(Role.builder().id(8L)
                .roleName("Super Admin").description("Quản trị viên cấp cao có toàn quyền hệ thống").build()));

        int order = 0;
        List<Permission> catalog = new ArrayList<>();
        for (Map.Entry<String, String> module : MODULES.entrySet()) {
            for (String action : ACTIONS) {
                if (!supports(module.getKey(), action)) continue;
                String code = module.getKey() + "." + action;
                Permission item = Permission.builder().code(code).module(module.getKey()).action(action)
                        .name(label(action) + " " + module.getValue().toLowerCase(Locale.forLanguageTag("vi")))
                        .platform(module.getKey().startsWith("MOBILE") ? "MOBILE" : "WEB")
                        .sortOrder(++order).build();
                catalog.add(item);
                if (!permissions.existsById(code)) permissions.save(item);
            }
        }

        // Chỉ seed ma trận mặc định lần đầu; các lần khởi động sau tuyệt đối không cấp lại quyền đã bị thu hồi.
        if (rolePermissions.count() == 0) {
            for (Permission permission : catalog) {
                grant(1L, permission.getCode());
                if (permission.getCode().startsWith("MANAGER.") || permission.getCode().startsWith("REPORT.")) grant(2L, permission.getCode());
                if (permission.getCode().startsWith("RECEPTION.")) grant(3L, permission.getCode());
                if (permission.getCode().startsWith("MOBILE.HOUSEKEEPING")) grant(4L, permission.getCode());
                if (permission.getCode().startsWith("MOBILE.SERVICE")) grant(6L, permission.getCode());
                if (permission.getCode().startsWith("REPORT.")
                        || permission.getCode().equals("RECEPTION.INVOICES.VIEW")
                        || permission.getCode().equals("RECEPTION.INVOICES.PRINT")) grant(7L, permission.getCode());
            }
        }

        promotePrimaryAdmin();
    }

    private void promotePrimaryAdmin() {
        List<User> allUsers = users.findAll();
        Optional<User> candidate = allUsers.stream()
                .filter(u -> Objects.equals(u.getRoleId(), 8L))
                .min(Comparator.comparing(User::getId));

        if (candidate.isEmpty()) candidate = allUsers.stream()
                .filter(u -> Objects.equals(u.getRoleId(), 1L) && "admin".equalsIgnoreCase(u.getUsername()))
                .min(Comparator.comparing(User::getId));
        if (candidate.isEmpty()) candidate = allUsers.stream()
                .filter(u -> Objects.equals(u.getRoleId(), 1L))
                .min(Comparator.comparing(User::getId));

        candidate.ifPresent(user -> {
            Optional<User> usernameOwner = users.findFirstByUsername(SUPER_ADMIN_USERNAME);
            if (usernameOwner.isPresent() && !Objects.equals(usernameOwner.get().getId(), user.getId())) {
                log.error("Khong the doi username Super Admin thanh '{}': username da thuoc tai khoan id={}",
                        SUPER_ADMIN_USERNAME, usernameOwner.get().getId());
                return;
            }
            user.setRoleId(8L);
            user.setUsername(SUPER_ADMIN_USERNAME);
            users.save(user);
            log.info("Da chuan hoa tai khoan Super Admin: id={}, username={}", user.getId(), SUPER_ADMIN_USERNAME);
        });
    }

    private void grant(Long roleId, String code) {
        String id = roleId + ":" + code;
        if (!rolePermissions.existsById(id)) rolePermissions.save(RolePermission.builder()
                .id(id).roleId(roleId).permissionCode(code).build());
    }

    private boolean supports(String module, String action) {
        if (module.equals("RECEPTION.ROOM_MAP")) return action.equals("VIEW");
        if (module.equals("REPORT.REVENUE")) return action.equals("VIEW") || action.equals("EXPORT");
        if (module.equals("RECEPTION.INVOICES")) return action.equals("VIEW") || action.equals("UPDATE") || action.equals("DELETE") || action.equals("PRINT");
        if (module.equals("RECEPTION.NO_SHOW")) return action.equals("VIEW") || action.equals("UPDATE");
        if (module.startsWith("MOBILE.")) return action.equals("VIEW") || action.equals("UPDATE");
        return !action.equals("PRINT") && !action.equals("EXPORT");
    }

    private String label(String action) {
        return switch (action) {
            case "VIEW" -> "Xem"; case "CREATE" -> "Thêm"; case "UPDATE" -> "Cập nhật";
            case "DELETE" -> "Xóa"; case "PRINT" -> "In"; case "EXPORT" -> "Xuất"; default -> action;
        };
    }
}
