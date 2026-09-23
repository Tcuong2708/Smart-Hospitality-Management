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
@Document(collection = "booking_details")
public class BookingDetail {
    
    @Id
    private Long id;
    
    @Field("booking_id")
    private Long bookingId;
    
    @Field("room_id")
    private Long roomId;
    
    @Field("unit_price")
    private Double unitPrice;
    
    @Field("actual_in")
    private Date actualIn;
    
    @Field("actual_out")
    private Date actualOut;
    
    @Field("late_fee")
    private Double lateFee;
}
