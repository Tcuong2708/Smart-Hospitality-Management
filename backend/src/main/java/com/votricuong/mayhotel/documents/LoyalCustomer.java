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
@Document(collection = "loyal_customers")
public class LoyalCustomer {
    
    @Id
    private Long id;
    
    @Field("customer_id")
    private Long customerId;
    
    private String tier;
    
    @Field("total_points")
    private Integer totalPoints;
}
