package com.votricuong.mayhotel.services;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Hàm gửi email OTP cho khách hàng.
     * <br>
     * Dựa theo quy tắc đặt tên: Động từ + danh từ (sendOtpEmail). 
     * Lưu ý: Hiện tại hệ thống đang giả lập in ra màn hình. Khi có Gmail cấu hình, hàm này sẽ tự động gọi gửi email thật.
     * 
     * @param toEmail Địa chỉ email người nhận
     * @param otp Mã OTP gồm 6 chữ số
     */
    public void sendOtpEmail(String toEmail, String otp) {
        // [NOTE]: Bạn hãy cấu hình Gmail trong application.properties
        // spring.mail.username=your_email@gmail.com
        // spring.mail.password=your_app_password
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Mã OTP Đăng Ký Tài Khoản - MAY HOTEL");
            message.setText("Chào bạn,\n\nMã OTP của bạn là: " + otp + "\nMã này có hiệu lực trong vòng 5 phút.\n\nTrân trọng,\nMAY HOTEL");
            
            // Bỏ comment dòng dưới để gửi email thật (Yêu cầu cấu hình properties)
            mailSender.send(message);
            
            System.out.println("========== MÃ OTP (GỬI ĐẾN " + toEmail + ") ==========");
            System.out.println("OTP CODE: " + otp);
            System.out.println("======================================================");
        } catch (Exception e) {
            System.err.println("Lỗi gửi email OTP: " + e.getMessage());
        }
    }
}
