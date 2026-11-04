package com.votricuong.mayhotel.utils;

import org.springframework.stereotype.Component;

/**
 * Tạo URL phân phối ảnh Cloudinary phù hợp với kích thước hiển thị.
 * Ảnh gốc trong MongoDB không bị thay đổi; Cloudinary tự chọn AVIF/WebP/JPEG
 * và chất lượng phù hợp với trình duyệt tại thời điểm tải.
 */
@Component("cloudinaryUrlUtil")
public class CloudinaryUrlUtil {
    private static final String CLOUDINARY_HOST = "res.cloudinary.com";
    private static final String UPLOAD_MARKER = "/image/upload/";
    private static final String DEFAULT_PUBLIC_IMAGE = "/images/a1.jpg";

    /**
     * Chuẩn hóa đường dẫn ảnh lấy từ MongoDB.
     *
     * Dữ liệu cũ có thể lưu tên tệp (a1.jpg, Deluxe1.jpg), đường dẫn tương đối,
     * đường dẫn tuyệt đối trong ứng dụng hoặc URL Cloudinary. Các ảnh tĩnh của dự án
     * đều được công khai dưới /images nên không được ép ảnh loại phòng vào
     * /images/rooms như trước đây.
     */
    public String publicImage(String url) {
        if (url == null || url.isBlank()) return DEFAULT_PUBLIC_IMAGE;

        String value = url.trim().replace('\\', '/');
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("data:")) {
            return value;
        }
        if (value.startsWith("/")) return value;
        if (value.startsWith("images/")) return "/" + value;
        return "/images/" + value;
    }

    public String cardFromDatabase(String url) {
        return card(publicImage(url));
    }

    public String fallbackImage() {
        return DEFAULT_PUBLIC_IMAGE;
    }

    public String thumbnail(String url, int width, int height) {
        return optimize(url, width, height, true);
    }

    public String card(String url) {
        return optimize(url, 720, 480, true);
    }

    public String hero(String url) {
        return optimize(url, 1600, 900, true);
    }

    public String optimize(String url, int width, int height, boolean crop) {
        if (url == null || url.isBlank()) return url;
        String value = url.trim();
        if (!value.contains(CLOUDINARY_HOST) || !value.contains(UPLOAD_MARKER)) return value;

        int safeWidth = clamp(width, 32, 2000);
        int safeHeight = clamp(height, 0, 1400);
        String resize = crop && safeHeight > 0
                ? "c_fill,g_auto,w_" + safeWidth + ",h_" + safeHeight
                : "c_limit,w_" + safeWidth + (safeHeight > 0 ? ",h_" + safeHeight : "");
        String transformation = "f_auto,q_auto:eco,dpr_auto," + resize;

        int insertionPoint = value.indexOf(UPLOAD_MARKER) + UPLOAD_MARKER.length();
        String deliveryPath = value.substring(insertionPoint);
        if (deliveryPath.startsWith(transformation + "/")) return value;
        return value.substring(0, insertionPoint) + transformation + "/" + deliveryPath;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
