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
@Document(collection = "customer_promotions")
public class CustomerPromotion {
    @Id
    private Long id;
    @Field("customer_id")
    private Long customerId;
    @Field("promotion_id")
    private Long promotionId;
    @Field("promotion_code")
    private String promotionCode;
    private String status; // UNUSED | USED | EXPIRED
    @Field("assigned_at")
    private Date assignedAt;
    @Field("used_at")
    private Date usedAt;
    @Field("used_booking_id")
    private Long usedBookingId;
    @Field("used_invoice_id")
    private Long usedInvoiceId;
}
