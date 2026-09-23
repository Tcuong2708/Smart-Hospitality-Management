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
    
    private String username;
    
    private String password;
    
    private String email;
    
    @org.springframework.data.mongodb.core.mapping.Field("status")
    private String status;
    
    // Reference to Role
    @org.springframework.data.mongodb.core.mapping.Field("role_id")
    private Long roleId;
}
