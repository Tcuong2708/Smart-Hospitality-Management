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
@Document(collection = "service_tickets")
public class ServiceTicket {
    
    @Id
    private Long id;
    
    @Field("booking_id")
    private Long bookingId;
    
    @Field("service_id")
    private Long serviceId;
    
    private Integer quantity;
    
    private Double price;
}
