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
@Document(collection = "reviews")
public class Review {
    
    @Id
    private Long id;
    
    @Field("booking_id")
    private Long bookingId;
    
    @Field("customer_id")
    private Long customerId;
    
    private Integer rating;
    
    private String content;
}
