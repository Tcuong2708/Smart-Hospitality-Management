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
@Document(collection = "promotion_types")
public class PromotionType {
    
    @Id
    private Long id; // Mã loại khuyến mãi
    
    @Field("name")
    private String name; // Tên loại khuyến mãi
    
    @Field("description")
    private String description; // Mô tả
}
