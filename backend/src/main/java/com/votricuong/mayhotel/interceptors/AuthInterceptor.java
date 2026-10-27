package com.votricuong.mayhotel.interceptors;

import com.votricuong.mayhotel.documents.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        
        // Bỏ qua kiểm tra đối với các route tĩnh hoặc công khai
        if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/") || uri.startsWith("/frontend/") 
            || uri.startsWith("/account/") || uri.equals("/") || uri.equals("/info")
            || uri.startsWith("/rooms") || uri.startsWith("/review") || uri.startsWith("/api/") || uri.equals("/error")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        // Chưa đăng nhập mà truy cập route yêu cầu bảo mật -> Về trang đăng nhập
        if (user == null) {
            response.sendRedirect("/account/login?error=timeout");
            return false;
        }

        String role = "CUSTOMER";
        if (user.getRoleId() != null) {
            if (user.getRoleId() == 1L) role = "ADMIN";
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

        if (uri.startsWith("/manager/") && !"ADMIN".equals(role) && !"MANAGER".equals(role)) {
            redirectDenied(request, response, role);
            return false;
        }
        if (uri.startsWith("/receptionist/") && !"ADMIN".equals(role) && !"RECEPTIONIST".equals(role)) {
            redirectDenied(request, response, role);
            return false;
        }
        if (uri.startsWith("/admin/statistical")
                && !"ADMIN".equals(role) && !"MANAGER".equals(role) && !"ACCOUNTANT".equals(role)) {
            redirectDenied(request, response, role);
            return false;
        }
        if (uri.startsWith("/admin/") && !uri.startsWith("/admin/statistical") && !"ADMIN".equals(role)) {
            redirectDenied(request, response, role);
            return false;
        }

        return true;
    }

    private void redirectDenied(HttpServletRequest request, HttpServletResponse response, String role) throws Exception {
        request.getSession().setAttribute("error", "Bạn không có quyền xem chức năng này");
        String home = switch (role) {
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
