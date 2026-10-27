package com.votricuong.mayhotel.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "rooms")
public class Room implements java.io.Serializable {

    @Id
    private Long id;
    
    private String name;
    private Double price;
    private String detail;
    private String imageUrl;
    @Field("room_type_id")
    private Long roomTypeId;
    
    private RoomType roomType;
    private String status;

    /** Trạng thái tính theo ngày đang xem, không ghi xuống MongoDB. */
    @Transient
    private String displayStatus;
    private String note;
    private Integer maxExtraBeds;

    @Field("status_note")
    private String statusNote;

    @Field("status_updated_at")
    private Date statusUpdatedAt;

    @Field("status_updated_by")
    private Long statusUpdatedBy;

    @Field("housekeeping_order_active")
    private Boolean housekeepingOrderActive;

    @Field("housekeeping_order_created_at")
    private Date housekeepingOrderCreatedAt;

    @Field("housekeeping_order_completed_at")
    private Date housekeepingOrderCompletedAt;
    
    // --- THUỘC TÍNH BỔ SUNG THEO YÊU CẦU ---
    @Field("view_direction")
    private String viewDirection; // "CITY", "SEA", "MOUNTAIN"
    
    @Field("location_zone")
    private String locationZone; // "LEFT", "RIGHT"

    /**
     * Alias used by the existing check-in view, which reads the JSON property
     * named "view" instead of "viewDirection".
     */
    public String getView() {
        return viewDirection;
    }
}
