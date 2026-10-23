package com.votricuong.mayhotel.documents;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@Document(collection = "point_transactions")
public class PointTransaction {
    @Id
    private String id;

    @Field("customer_id")
    private Long customerId;

    @Field("invoice_id")
    private Long invoiceId;

    @Indexed(unique = true)
    @Field("reference_id")
    private String referenceId;

    private String type;
    private Integer points;

    @Field("balance_after")
    private Integer balanceAfter;

    private String description;

    @Field("created_at")
    private Date createdAt;

    public PointTransaction(String id, Long customerId, Long invoiceId, String referenceId,
                            String type, Integer points, Integer balanceAfter,
                            String description, Date createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.invoiceId = invoiceId;
        this.referenceId = referenceId;
        this.type = type;
        this.points = points;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.createdAt = createdAt;
    }
}
