package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.dto.ApiResponse;
import com.votricuong.mayhotel.repositories.UserRepository;
import com.votricuong.mayhotel.services.MobileTokenService;
import com.votricuong.mayhotel.services.NotificationService;
import com.votricuong.mayhotel.services.PermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mobile/permissions")
public class PermissionApiController {
    private final MobileTokenService tokens;
    private final UserRepository users;
    private final PermissionService permissions;
    private final NotificationService notifications;

    public PermissionApiController(MobileTokenService tokens, UserRepository users,
                                   PermissionService permissions, NotificationService notifications) {
        this.tokens = tokens;
        this.users = users;
        this.permissions = permissions;
        this.notifications = notifications;
    }

    @GetMapping("/capabilities")
    public ResponseEntity<?> capabilities(@RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            User user = currentUser(authorization);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("roleId", user.getRoleId());
            data.put("role", user.getRole());
            data.put("permissions", permissions.getEffectivePermissionCodes(user));
            data.put("unreadNotifications", notifications.countUnreadByUser(user.getId()));
            return ResponseEntity.ok(ApiResponse.success("Tải quyền chức năng thành công", data));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage()));
        }
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> notificationList(@RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            User user = currentUser(authorization);
            return ResponseEntity.ok(ApiResponse.success("Tải thông báo thành công", notifications.getLatestByUser(user.getId())));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<?> readAll(@RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            User user = currentUser(authorization);
            notifications.markAllUserNotificationsAsRead(user.getId());
            return ResponseEntity.ok(ApiResponse.success("Đã đọc tất cả thông báo", Map.of()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage()));
        }
    }

    private User currentUser(String authorization) {
        MobileTokenService.Claims claims = tokens.verifyAuthorization(authorization);
        return users.findById(claims.userId()).orElseThrow(() -> new SecurityException("Tài khoản không tồn tại"));
    }
}
