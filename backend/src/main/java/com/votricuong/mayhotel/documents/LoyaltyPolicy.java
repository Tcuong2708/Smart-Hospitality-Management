package com.votricuong.mayhotel.documents;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@Document(collection = "loyalty_policy")
public class LoyaltyPolicy {

    public static final int DEFAULT_SILVER_THRESHOLD = 1000;
    public static final int DEFAULT_GOLD_THRESHOLD = 5000;
    public static final int DEFAULT_PLATINUM_THRESHOLD = 10000;

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

    public LoyaltyPolicy(String id, Double moneyPerPoint, Integer silverThreshold,
                         Integer goldThreshold, Integer platinumThreshold) {
        this.id = id;
        this.moneyPerPoint = moneyPerPoint;
        this.silverThreshold = silverThreshold;
        this.goldThreshold = goldThreshold;
        this.platinumThreshold = platinumThreshold;
    }

    public static LoyaltyPolicy defaultPolicy() {
        return LoyaltyPolicy.builder()
                .id("singleton")
                .moneyPerPoint(100000.0)
                .silverThreshold(DEFAULT_SILVER_THRESHOLD)
                .goldThreshold(DEFAULT_GOLD_THRESHOLD)
                .platinumThreshold(DEFAULT_PLATINUM_THRESHOLD)
                .build();
    }

    public boolean usesLegacyThresholds() {
        return silverThreshold != null && goldThreshold != null && platinumThreshold != null
                && silverThreshold <= 100
                && goldThreshold <= 500
                && platinumThreshold <= 2000;
    }

    public void upgradeLegacyThresholds() {
        silverThreshold = DEFAULT_SILVER_THRESHOLD;
        goldThreshold = DEFAULT_GOLD_THRESHOLD;
        platinumThreshold = DEFAULT_PLATINUM_THRESHOLD;
    }
}
