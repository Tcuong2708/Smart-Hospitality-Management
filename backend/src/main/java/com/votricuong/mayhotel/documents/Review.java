package com.votricuong.mayhotel.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

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

    @Field("room_ids")
    private List<Long> roomIds;

    @Field("room_type_ids")
    private List<Long> roomTypeIds;
    
    private Integer rating;
    
    private String content;

    @Field("review_date")
    private java.util.Date reviewDate;

    private String status; // "Hiện" hoặc "Ẩn"

    private String reply; // Phản hồi của quản lý
}
