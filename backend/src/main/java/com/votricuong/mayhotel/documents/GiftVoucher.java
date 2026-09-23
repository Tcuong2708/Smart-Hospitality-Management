package com.votricuong.mayhotel.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "gift_vouchers")
public class GiftVoucher {
    
    @Id
    private Long id;
    
    @Field("customer_id")
    private Long customerId;
    
    private Double value;
    
    @Field("expiry_date")
    private Date expiryDate;
    
    private String status;
}
