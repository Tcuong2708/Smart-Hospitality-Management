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
@Document(collection = "bookingRequests")
public class BookingRequest {

    @Id
    private Long id;
    
    private String guestName;
    private String phone;
    private String roomTypeRequested;
    private Date requestedAt;
    private String checkInDate; // stored as string DD/MM/YYYY in JSON
    private String checkOutDate;
    private String status;
    private Long userId;
}
