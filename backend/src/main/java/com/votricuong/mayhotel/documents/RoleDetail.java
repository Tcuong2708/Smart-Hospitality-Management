package com.votricuong.mayhotel.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "role_details")
public class RoleDetail {
    
    @Id
    private Long id; // Mã chi tiết quyền
    
    @Field("user_id")
    private Long userId; // FK trỏ tới Users (TaiKhoan)
    
    @Field("role_id")
    private Long roleId; // FK trỏ tới Roles (PhanQuyen)
}
