package com.votricuong.mayhotel.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Ngăn trình duyệt giữ giao diện của bản build cũ sau khi triển khai lên VPS.
 * Tài nguyên tĩnh được phép lưu nhưng phải xác thực lại; HTML/API không được
 * dùng lại trực tiếp từ bộ nhớ đệm.
 */
@Component
public class BrowserCacheControlFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            String path = request.getRequestURI();
            boolean staticResource = path.startsWith("/css/")
                    || path.startsWith("/js/")
                    || path.startsWith("/images/")
                    || path.startsWith("/webjars/")
                    || path.equals("/favicon.ico");
            response.setHeader("Cache-Control", staticResource
                    ? "no-cache, must-revalidate, max-age=0"
                    : "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
        }
        filterChain.doFilter(request, response);
    }
}
