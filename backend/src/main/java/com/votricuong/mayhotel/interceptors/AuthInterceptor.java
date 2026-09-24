package com.votricuong.mayhotel.interceptors;

import com.votricuong.mayhotel.documents.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

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
        }
        // Tuyến khách hàng
        if (uri.startsWith("/booking/")) {
            if (!"CUSTOMER".equals(role)) {
                // Nhân viên không được vào giao diện đặt phòng dành cho khách hàng
                request.getSession().setAttribute("error", "Bạn không có quyền xem trang này");
                response.sendRedirect("/account/login");
                return false;
            }
            return true;
        }

        // Tuyến Admin/Manager (Ban giám đốc, Kế toán, Nhân sự, Admin)
        if (uri.startsWith("/admin/")) {
            List<String> adminRoles = Arrays.asList("ADMIN", "MANAGER", "ACCOUNTANT", "HR");
            
            // Các trang cụ thể
            if (uri.startsWith("/admin/statistical") && !Arrays.asList("ADMIN", "MANAGER", "ACCOUNTANT").contains(role)) {
                redirectDenied(request, response);
                return false;
            }
            
            if (uri.startsWith("/admin/users") && !"ADMIN".equals(role)) {
                redirectDenied(request, response);
                return false;
            }

            if (!adminRoles.contains(role) && !"RECEPTIONIST".equals(role) && !"HOUSEKEEPER".equals(role)) {
                // Nếu là khách hàng vào admin -> Chặn
                redirectDenied(request, response);
                return false;
            }
            
            // Lễ tân, Buồng phòng được truy cập vào admin nhưng bị giới hạn module hiển thị trên giao diện
            return true;
        }

        return true;
    }

    private void redirectDenied(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request.getSession().setAttribute("error", "Bạn không có quyền xem trang này");
        response.sendRedirect("/account/login");
    }
}
