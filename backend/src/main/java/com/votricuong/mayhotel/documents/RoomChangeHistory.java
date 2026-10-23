package com.votricuong.mayhotel.documents;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "room_change_histories")
public class RoomChangeHistory {
    @Id
    private Long id;
    @Field("invoice_id")
    private Long invoiceId;
    @Field("booking_id")
    private Long bookingId;
    @Field("old_room_id")
    private Long oldRoomId;
    @Field("new_room_id")
    private Long newRoomId;
    @Field("old_room_price")
    private Double oldRoomPrice;
    @Field("new_room_price")
    private Double newRoomPrice;
    @Field("remaining_nights")
    private long remainingNights;
    @Field("price_difference")
    private Double priceDifference;
    private String reason;
    @Field("performed_by")
    private Long performedBy;
    @Field("changed_at")
    private Date changedAt;
}
