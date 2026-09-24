package com.votricuong.mayhotel.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "room_types")
public class RoomType {
    
    @Id
    private Long id;
    
    private String name;
    
    @org.springframework.data.mongodb.core.mapping.Field("max_occupancy")
    private Integer maxOccupancy;
    
    private String imageUrl;
    private Integer quantity;
    
    @org.springframework.data.annotation.Transient
    private Integer availableRoomsCount = 0;
    
    @org.springframework.data.annotation.Transient
    private Integer totalRoomsCount = 0;
}
