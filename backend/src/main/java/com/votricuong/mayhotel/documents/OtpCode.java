package com.votricuong.mayhotel.documents;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Customer pendingCustomer;

    // Thời điểm tạo yêu cầu OTP, phục vụ đối soát.
    private Date createdAt;

    // MongoDB chỉ tự xóa bản ghi OTP đang chờ. Khi hoàn tất đăng ký, trường này
    // được đặt về null để giữ lại lịch sử đối soát.
    @Indexed(name = "idx_otp_pending_expiry", expireAfter = "0s")
    private Date expiresAt;

    // Chỉ cho phép đăng ký sau khi mã đã được xác thực thành công.
    private Date verifiedAt;

    private Date completedAt;
    private String purpose;
    private String status;
    private Long userId;
    private Long customerId;
    private String source;
}
