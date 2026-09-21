package com.votricuong.mayhotel.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "otp_codes")
public class OtpCode {

    @Id
    private String id;
    
    // Khóa liên kết: Email hoặc Số điện thoại
    @Indexed
    private String identifier;
    
    // Mã OTP 6 chữ số
    private String code;
    
    // Thông tin đăng ký lưu tạm thời
    private User pendingUser;

    // Tự động xóa sau 5 phút (300 giây)
    @Indexed(expireAfterSeconds = 300)
    private Date createdAt;
}
