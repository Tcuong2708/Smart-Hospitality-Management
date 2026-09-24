package com.votricuong.mayhotel.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "invoices")
public class Invoice {

    @Id
    private Long id;

    @org.springframework.data.mongodb.core.mapping.Field("booking_id")
    private Long bookingId;
    
    @org.springframework.data.mongodb.core.mapping.Field("staff_id")
    private Long staffId;
    
    @org.springframework.data.mongodb.core.mapping.Field("promo_id")
    private Long promoId;
    
    @org.springframework.data.mongodb.core.mapping.Field("created_at")
    private Date createdAt;
    
    @org.springframework.data.mongodb.core.mapping.Field("total_amount")
    private Double totalAmount;
    
    @org.springframework.data.mongodb.core.mapping.Field("pay_method")
    private String payMethod;
    
    @org.springframework.data.mongodb.core.mapping.Field("invoice_status")
    private String invoiceStatus;
    
    private String note;

    private String guestName;
    private String phone;
    private Long roomId;
    private Date checkInDate;
    private Date checkOutDate;
    private Long userId;
    private Boolean isPaid;
    private Double surcharge;
}
