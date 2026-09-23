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
@Document(collection = "rooms")
public class Room {

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
    private String note;
    private Integer maxExtraBeds;
}
