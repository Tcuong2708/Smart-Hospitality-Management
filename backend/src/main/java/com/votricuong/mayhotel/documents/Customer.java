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
@Document(collection = "customers")
public class Customer {
    
    @Id
    private Long id;
    
    @Field("user_id")
    private Long userId;
    
    @Field("full_name")
    private String fullName;
    
    private String phone;
    
    private String cccd;
    
    private String email;
    
    @Field("dob")
    private java.util.Date dob;
    
    @Field("points")
    private Integer points;
    
    @Field("expiry_date")
    private java.util.Date expiryDate;
    
    @Field("tier_id")
    private Long tierId;
    
    // --- CÁC THUỘC TÍNH BỔ SUNG CHO NGHIỆP VỤ CHECK-IN AI ---
    @Field("quoc_tich")
    private String quocTich;
    
    @Field("que_quan")
    private String queQuan;
    
    @Field("ngay_het_han_cccd")
    private String ngayHetHanCCCD;
    
    @Field("anh_face_id")
    private String anhFaceID;
}
