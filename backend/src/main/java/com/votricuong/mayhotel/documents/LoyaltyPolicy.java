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
@Document(collection = "loyalty_policy")
public class LoyaltyPolicy {

    @Id
    private String id; // Luôn dùng ID="singleton" vì chỉ có 1 cấu hình chung

    @Field("money_per_point")
    private Double moneyPerPoint; // Ví dụ: 100000 (100k = 1 điểm)

    @Field("silver_threshold")
    private Integer silverThreshold; // Mốc điểm lên thẻ Bạc

    @Field("gold_threshold")
    private Integer goldThreshold; // Mốc điểm lên thẻ Vàng

    @Field("platinum_threshold")
    private Integer platinumThreshold; // Mốc điểm lên thẻ Bạch Kim
}
