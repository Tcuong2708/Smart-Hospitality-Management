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
@Document(collection = "users")
public class User implements java.io.Serializable {
    
    @Id
    private Long id;
    
    private String username;
    
    private String password;
    
    private String email;
    
    private String phone;

    @Field("full_name")
    private String fullName;

    private String address;

    private String nationality;
    
    @Field("status")
    private String status;
    
    @Field("quoc_tich")
    private String quocTich;
    
    // Reference to Role
    @org.springframework.data.mongodb.core.mapping.Field("role_id")
    private Long roleId;

    public String getRole() {
        if (roleId == null) return "CUSTOMER";
        if (roleId == 1L) return "ADMIN";
        if (roleId == 2L) return "MANAGER";
        if (roleId == 3L) return "RECEPTIONIST";
        if (roleId == 4L) return "HOUSEKEEPER";
        if (roleId == 5L) return "CUSTOMER";
        return "CUSTOMER";
    }

    public String getFullName() {
        if (fullName != null && !fullName.isBlank()) return fullName;
        return username != null ? username : email;
    }

    public String getNationality() {
        if (nationality != null && !nationality.isBlank()) return nationality;
        return quocTich;
    }
}
