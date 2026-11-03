package com.votricuong.mayhotel.interceptors;

import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.repositories.UserRepository;
import com.votricuong.mayhotel.services.MobileTokenService;
import com.votricuong.mayhotel.services.NotificationService;
import com.votricuong.mayhotel.services.PermissionRouteResolver;
import com.votricuong.mayhotel.services.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final PermissionService permissionService;
    private final PermissionRouteResolver permissionRouteResolver;
    private final MobileTokenService mobileTokenService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AuthInterceptor(PermissionService permissionService, PermissionRouteResolver permissionRouteResolver,
                           MobileTokenService mobileTokenService, UserRepository userRepository,
                           NotificationService notificationService) {
        this.permissionService = permissionService;
        this.permissionRouteResolver = permissionRouteResolver;
        this.mobileTokenService = mobileTokenService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        
        // Bỏ qua kiểm tra đối với các route tĩnh hoặc công khai
        if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/") || uri.startsWith("/frontend/") 
            || uri.startsWith("/account/") || uri.equals("/") || uri.equals("/info")
            || uri.startsWith("/rooms") || uri.startsWith("/review")
            || (uri.startsWith("/api/") && permissionRouteResolver.resolve(request) == null) || uri.equals("/error")) {
            return true;
        }

        String permissionCode = permissionRouteResolver.resolve(request);
        if (uri.startsWith("/api/") && permissionCode != null) {
            return authorizeMobile(request, response, permissionCode);
        }

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        // Chưa đăng nhập mà truy cập route yêu cầu bảo mật -> Về trang đăng nhập
        if (user == null) {
            response.sendRedirect("/account/login?error=timeout");
            return false;
        }

        request.setAttribute("staffNotifications", notificationService.getLatestByUser(user.getId()));
        request.setAttribute("staffUnreadCount", notificationService.countUnreadByUser(user.getId()));

        if (permissionCode != null) {
            if (!permissionService.hasPermission(user, permissionCode)) {
                redirectDenied(request, response, user.getRole());
                return false;
            }
            return true;
        }

        String role = "CUSTOMER";
        if (user.getRoleId() != null) {
            if (user.getRoleId() == 8L) role = "SUPER_ADMIN";
            else if (user.getRoleId() == 1L) role = "ADMIN";
            else if (user.getRoleId() == 2L) role = "MANAGER";
            else if (user.getRoleId() == 3L) role = "RECEPTIONIST";
            else if (user.getRoleId() == 4L) role = "HOUSEKEEPER";
            else if (user.getRoleId() == 5L) role = "CUSTOMER";
            else if (user.getRoleId() == 6L) role = "SERVICE_STAFF";
            else if (user.getRoleId() == 7L) role = "ACCOUNTANT";
        }

        // Kế toán là vai trò chuyên biệt theo sơ đồ use case: chỉ được xem,
        // lọc và xuất báo cáo doanh thu. Mọi nghiệp vụ quản trị/lễ tân khác
        // đều quay lại màn hình thống kê.
        if ("ACCOUNTANT".equals(role) && !uri.startsWith("/admin/statistical")) {
            redirectDenied(request, response, role);
            return false;
        }

        // Tuyến khách hàng
        if (uri.startsWith("/booking/")) {
            if (!"CUSTOMER".equals(role)) {
                redirectDenied(request, response, role);
                return false;
            }
            return true;
        }

        if (uri.startsWith("/manager/") && !"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role) && !"MANAGER".equals(role)) {
            redirectDenied(request, response, role);
            return false;
        }
        if (uri.startsWith("/receptionist/") && !"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role) && !"RECEPTIONIST".equals(role)) {
            redirectDenied(request, response, role);
            return false;
        }
        if (uri.startsWith("/admin/statistical")
                && !"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role) && !"MANAGER".equals(role) && !"ACCOUNTANT".equals(role)) {
            redirectDenied(request, response, role);
            return false;
        }
        if (uri.startsWith("/admin/") && !uri.startsWith("/admin/statistical") && !"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role)) {
            redirectDenied(request, response, role);
            return false;
        }

        return true;
    }

    private boolean authorizeMobile(HttpServletRequest request, HttpServletResponse response, String permissionCode) throws Exception {
        try {
            MobileTokenService.Claims claims = mobileTokenService.verifyAuthorization(request.getHeader("Authorization"));
            User user = userRepository.findById(claims.userId()).orElseThrow(() -> new SecurityException("Tài khoản không tồn tại"));
            if (!permissionService.hasPermission(user, permissionCode)) {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"status\":\"error\",\"code\":\"PERMISSION_DENIED\",\"message\":\"Bạn không có quyền truy cập chức năng này\",\"permission\":\"" + permissionCode + "\"}");
                return false;
            }
            return true;
        } catch (SecurityException ex) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":\"error\",\"code\":\"UNAUTHORIZED\",\"message\":\"Phiên đăng nhập không hợp lệ\"}");
            return false;
        }
    }

    private void redirectDenied(HttpServletRequest request, HttpServletResponse response, String role) throws Exception {
        request.getSession().setAttribute("error", "Bạn không có quyền xem chức năng này");
        String home = switch (role) {
            case "SUPER_ADMIN" -> "/admin/roles";
            case "ADMIN" -> "/admin/users";
            case "MANAGER" -> "/manager/rooms";
            case "RECEPTIONIST" -> "/receptionist/room-map";
            case "ACCOUNTANT" -> "/admin/statistical";
            case "CUSTOMER" -> "/";
            default -> "/account/login";
        };
        response.sendRedirect(home);
    }
}
