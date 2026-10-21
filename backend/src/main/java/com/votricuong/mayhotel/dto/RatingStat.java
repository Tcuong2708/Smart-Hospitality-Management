package com.votricuong.mayhotel.dto;

import java.util.Locale;

/**
 * Tổng hợp điểm đánh giá của một đối tượng (ví dụ một loại phòng).
 * Đánh giá của khách là 1–5 sao; hiển thị quy đổi sang thang 10 (giống các
 * trang đặt phòng).
 */
public class RatingStat {

    private final int count; // số lượt đánh giá
    private final int sum; // tổng số sao

    public RatingStat(int count, int sum) {
        this.count = count;
        this.sum = sum;
    }

    public int getCount() {
        return count;
    }

    public int getSum() {
        return sum;
    }

    /** Điểm trung bình 1–5 chưa làm tròn (dùng để xếp hạng). */
    public double getRawAverage() {
        return count == 0 ? 0 : (double) sum / count;
    }

    /** Điểm trung bình 1–5, làm tròn 1 chữ số. */
    public double getAverage() {
        return Math.round(getRawAverage() * 10.0) / 10.0;
    }

    /** Điểm thang 10, làm tròn 1 chữ số. */
    public double getScore10() {
        return Math.round(getRawAverage() * 20.0) / 10.0;
    }

    /** Điểm thang 10 dạng chữ, ví dụ "9,2". */
    public String getScoreText() {
        return String.format(Locale.forLanguageTag("vi-VN"), "%.1f", getScore10());
    }

    /** Nhãn mô tả theo điểm thang 10. */
    public String getLabel() {
        double s = getScore10();
        if (s >= 9.0)
            return "Tuyệt vời";
        if (s >= 8.0)
            return "Rất tốt";
        if (s >= 7.0)
            return "Tốt";
        if (s >= 6.0)
            return "Khá tốt";
        return "Trung bình";
    }
}