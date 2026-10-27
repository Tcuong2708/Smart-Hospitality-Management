package com.votricuong.mayhotel.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;
import java.util.Map;

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

    @Field("booking_source")
    private String bookingSource;

    @Field("risk_analysis_status")
    private String riskAnalysisStatus;

    @Field("late_arrival_approved")
    private Boolean lateArrivalApproved;

    @Field("late_arrival_reason")
    private String lateArrivalReason;

    @Field("late_arrival_at")
    private Date lateArrivalAt;

    @Field("late_arrival_approved_by")
    private Long lateArrivalApprovedBy;

    @Field("cancellation_reason")
    private String cancellationReason;

    @Field("cancelled_at")
    private Date cancelledAt;

    // Các trường phục vụ UC10 - Trí tuệ nhân tạo dự đoán No-show
    @Field("no_show_risk_level")
    private String noShowRiskLevel; // Thấp, Trung bình, Cao

    @Field("no_show_risk_percentage")
    private Double noShowRiskPercentage; // VD: 85.5%

    @Field("auto_cancel_time")
    private Date autoCancelTime; // Thời gian sẽ tự động hủy phiếu (vd: 14:00 hoặc 18:00)
    
    // --- THUỘC TÍNH BỔ SUNG CHO NGHIỆP VỤ CHECK-IN AI ---
    @Field("is_face_verified")
    private Boolean isFaceVerified; // Cờ xác định khách đã quét khuôn mặt thành công qua Mobile App

    @Field("identity_verification_method")
    private String identityVerificationMethod;

    @Field("identity_verification_reason")
    private String identityVerificationReason;

    @Field("identity_verified_at")
    private Date identityVerifiedAt;

    @Field("guest_count")
    private Integer guestCount;

    @Field("adult_count")
    private Integer adultCount;

    @Field("child_count")
    private Integer childCount;

    @Field("infant_count")
    private Integer infantCount;

    @Field("payment_expires_at")
    private Date paymentExpiresAt;

    @Field("verified_guest_count")
    private Integer verifiedGuestCount;

    @Field("guest_verifications")
    private List<Map<String, Object>> guestVerifications;
}
