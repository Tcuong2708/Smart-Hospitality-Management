package com.votricuong.mayhotel.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Ảnh chụp điểm đánh giá tại một thời điểm: điểm theo từng loại phòng + điểm
 * chung toàn khách sạn.
 * Dùng để hiển thị điểm và xếp hạng "lựa chọn hàng đầu".
 */
public class RatingSnapshot {

    /**
     * Mỗi loại phòng được coi như đã có sẵn từng này lượt đánh giá "trung bình"
     * (chống thiên lệch).
     */
    public static final int PRIOR_WEIGHT = 3;
    /** Điểm nền khi toàn hệ thống chưa có đánh giá nào (thang 5). */
    public static final double DEFAULT_PRIOR = 4.0;

    private final Map<Long, RatingStat> byRoomType;
    private final RatingStat overall; // null nếu chưa có đánh giá nào

    public RatingSnapshot(Map<Long, RatingStat> byRoomType, RatingStat overall) {
        this.byRoomType = byRoomType == null ? new HashMap<>() : byRoomType;
        this.overall = overall;
    }

    public static RatingSnapshot empty() {
        return new RatingSnapshot(Collections.emptyMap(), null);
    }

    public Map<Long, RatingStat> getByRoomType() {
        return byRoomType;
    }

    public RatingStat getOverall() {
        return overall;
    }

    /**
     * Điểm xếp hạng (trung bình có trọng số Bayesian, thang 5).
     * 1 đánh giá 5 sao không vượt được loại phòng có 20 đánh giá 4,8 sao;
     * loại chưa có đánh giá nhận điểm trung bình chung.
     */
    public double rankOfType(Long roomTypeId) {
        double prior = (overall == null || overall.getCount() == 0) ? DEFAULT_PRIOR : overall.getRawAverage();
        RatingStat s = roomTypeId == null ? null : byRoomType.get(roomTypeId);
        if (s == null || s.getCount() == 0) {
            return prior;
        }
        return (PRIOR_WEIGHT * prior + s.getSum()) / (PRIOR_WEIGHT + s.getCount());
    }
}