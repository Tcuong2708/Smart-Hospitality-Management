package com.votricuong.mayhotel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.votricuong.mayhotel.interceptors.AuthInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                // Loại trừ các URL công khai khỏi AuthInterceptor
                .excludePathPatterns(
                        "/booking/payment-vnpay/**",  // Luồng thanh toán VNPay từ Chatbot
                        "/booking/vnpay-return/**",   // URL VNPay trả về sau thanh toán
                        "/vnpay-return/**",
                        "/api/v1/internal/chatbot/**", // API nội bộ Chatbot
                        "/login",
                        "/register",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/favicon.ico"
                );
    }
}