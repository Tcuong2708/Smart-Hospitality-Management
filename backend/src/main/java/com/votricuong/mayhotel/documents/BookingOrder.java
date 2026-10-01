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
    
    private String notes;

    // Các trường phục vụ UC10 - Trí tuệ nhân tạo dự đoán No-show
    @Field("no_show_risk_level")
    private String noShowRiskLevel; // Thấp, Trung bình, Cao

    @Field("no_show_risk_percentage")
    private Double noShowRiskPercentage; // VD: 85.5%

    @Field("auto_cancel_time")
    private Date autoCancelTime; // Thời gian sẽ tự động hủy phiếu (vd: 14:00 hoặc 18:00)
}
