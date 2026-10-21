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

    public static final String SEQUENCE_NAME = "invoices";

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

    @org.springframework.data.mongodb.core.mapping.Field("loyalty_points_awarded")
    private Boolean loyaltyPointsAwarded;

    @org.springframework.data.mongodb.core.mapping.Field("earned_points")
    private Integer earnedPoints;

    @org.springframework.data.mongodb.core.mapping.Field("redeemed_points")
    private Integer redeemedPoints;

    @org.springframework.data.mongodb.core.mapping.Field("points_discount")
    private Double pointsDiscount;

    @org.springframework.data.mongodb.core.mapping.Field("penalty_rate")
    private Double penaltyRate;

    @org.springframework.data.mongodb.core.mapping.Field("penalty_amount")
    private Double penaltyAmount;

    @org.springframework.data.mongodb.core.mapping.Field("refund_amount")
    private Double refundAmount;

    @org.springframework.data.mongodb.core.mapping.Field("cancellation_type")
    private String cancellationType;
}
