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
}
