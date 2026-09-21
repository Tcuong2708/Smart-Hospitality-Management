package com.votricuong.mayhotel.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    
    @Id
    private Long id;
    
    // Tên đăng nhập (Thường dùng cho Admin)
    private String username;
    
    // Mật khẩu đã mã hóa
    private String password;
    
    // Họ và tên
    private String fullName;
    
    // Số điện thoại (Dùng làm key đăng nhập khách hàng)
    private String phone;
    
    // Địa chỉ liên hệ
    private String address;
    
    // Phân quyền (Ví dụ: ADMIN, CUSTOMER, STAFF)
    private String role;
    
    // Quốc tịch
    private String nationality;
    
    // Email (Dùng nhận OTP và đăng nhập Google)
    private String email;
    
    // Trạng thái kích hoạt tài khoản
    private Boolean isActive;
    
    // Căn cước công dân (Dành cho Khách hàng/Staff)
    @Builder.Default
    private String cccd = "";
    
    // Ngày hết hạn CCCD (Dùng cho luồng Check-in tự động AI)
    private java.util.Date expiryDate;
    
    // Hạng thành viên (Ví dụ: STANDARD, VIP)
    @Builder.Default
    private String tier = "STANDARD";
    
    // Tổng điểm tích lũy
    @Builder.Default
    private Integer totalPoints = 0;
}
