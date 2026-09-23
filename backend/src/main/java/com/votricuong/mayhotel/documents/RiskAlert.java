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
@Document(collection = "risk_alerts")
public class RiskAlert {
    
    @Id
    private Long id;
    
    @Field("booking_id")
    private Long bookingId;
    
    @Field("risk_rate")
    private Double riskRate;
    
    @Field("analyze_date")
    private Date analyzeDate;
    
    private String reason;
}
