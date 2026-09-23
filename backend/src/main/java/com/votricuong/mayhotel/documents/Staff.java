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
@Document(collection = "staffs")
public class Staff {
    
    @Id
    private Long id;
    
    @Field("user_id")
    private Long userId;
    
    @Field("full_name")
    private String fullName;
    
    private String phone;
    
    private String position;
    
    @Field("dob")
    private java.util.Date dob;
    
    private String gender;
}
