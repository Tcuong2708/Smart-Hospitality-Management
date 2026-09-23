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
@Document(collection = "booking_orders")
public class BookingOrder {

    @Id
    private Long id;
    
    @org.springframework.data.mongodb.core.mapping.Field("customer_id")
    private Long customerId;
    
    @org.springframework.data.mongodb.core.mapping.Field("booking_date")
    private Date bookingDate;
    
    @org.springframework.data.mongodb.core.mapping.Field("expected_in")
    private Date expectedIn;
    
    @org.springframework.data.mongodb.core.mapping.Field("expected_out")
    private Date expectedOut;
    
    private String status;
}
