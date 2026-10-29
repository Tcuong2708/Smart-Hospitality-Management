package com.votricuong.mayhotel.documents;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "promotions")
public class Promotion {
    
    @Id
    private Long id;
    
    private String name;
    
    @Field("discount_percent")
    private Double discountPercent;
    
    @Field("start_date")
    private Date startDate;
    
    @Field("end_date")
    private Date endDate;

    @Field("created_at")
    private Date createdAt;

    private String code;
    private String type;
    private String discountType;
    private Double discountAmount;
    private Double minimumOrderAmount;
    private Integer minimumRooms;
    private Integer minimumGuests;
    private Integer minimumNights;
    private Integer advanceDays;
    private List<Long> eligibleRoomTypeIds;
    private List<Integer> eligibleWeekdays;
    private List<Map<String, Object>> tiers;
    private Integer maxUses;
    private Integer usedCount;
    private Integer maxUsesPerCustomer;
    private String status;
    private Integer priority;
    private Boolean combinable;
    private Boolean applyOnBookingDate;
    private Boolean applyOnStayDate;
    private String description;
    /** PUBLIC: áp dụng công khai; PRIVATE: chỉ khách được cấp mới sử dụng. */
    private String visibility;
    private Integer assignedCount;

    public Promotion(Long id, String name, Double discountPercent, Date startDate, Date endDate) {
        this.id = id;
        this.name = name;
        this.discountPercent = discountPercent;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = "SPECIAL_DATE";
        this.discountType = "PERCENT";
        this.status = "ACTIVE";
        this.usedCount = 0;
        this.priority = 0;
        this.combinable = false;
        this.applyOnStayDate = true;
        this.visibility = "PUBLIC";
        this.assignedCount = 0;
    }
}
