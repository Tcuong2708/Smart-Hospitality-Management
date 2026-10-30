package com.votricuong.mayhotel.utils;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class InputValidator {
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{9,15}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{4,50}$");
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");

    private InputValidator() {}

    public static String requiredText(String value, String field, int maxLength) {
        String normalized = normalize(value);
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " không được để trống");
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " không được vượt quá " + maxLength + " ký tự");
        }
        if (CONTROL.matcher(normalized).find()) throw new IllegalArgumentException(field + " chứa ký tự không hợp lệ");
        return normalized;
    }

    public static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) return "";
        return requiredText(value, field, maxLength);
    }

    public static String phone(String value) {
        String normalized = requiredText(value, "Số điện thoại", 20)
                .replace(" ", "").replace("-", "").replace("(", "").replace(")", "");
        if (!PHONE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Số điện thoại phải gồm 9-15 chữ số và chỉ có thể bắt đầu bằng dấu +");
        }
        return normalized;
    }

    public static String email(String value, boolean required) {
        if (!required && (value == null || value.isBlank())) return "";
        String normalized = requiredText(value, "Email", 120).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalized).matches()) throw new IllegalArgumentException("Email không đúng định dạng");
        return normalized;
    }

    public static String username(String value) {
        String normalized = requiredText(value, "Tên đăng nhập", 50);
        if (!USERNAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Tên đăng nhập phải dài 4-50 ký tự và chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch ngang");
        }
        return normalized;
    }

    public static String password(String value) {
        String password = requiredText(value, "Mật khẩu", 100);
        if (password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự, gồm chữ và số");
        }
        return password;
    }

    /**
     * Quy tắc dành riêng cho màn hình quản trị tài khoản /admin/users.
     * Tách khỏi password() để không làm yếu chính sách đăng ký/đổi mật khẩu công khai.
     */
    public static String adminUserPassword(String value) {
        String password = requiredText(value, "Mật khẩu", 100);
        if (password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        }
        return password;
    }

    public static void stay(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) throw new IllegalArgumentException("Ngày nhận và ngày trả phòng không được để trống");
        if (checkIn.isBefore(LocalDate.now())) throw new IllegalArgumentException("Ngày nhận phòng không được ở trong quá khứ");
        if (!checkOut.isAfter(checkIn)) throw new IllegalArgumentException("Ngày trả phòng phải sau ngày nhận phòng");
        if (checkOut.isAfter(checkIn.plusDays(365))) throw new IllegalArgumentException("Thời gian lưu trú không được vượt quá 365 ngày");
    }

    public static double positiveMoney(Double value, String field) {
        if (value == null || !Double.isFinite(value) || value <= 0 || value > 1_000_000_000D) {
            throw new IllegalArgumentException(field + " phải lớn hơn 0 và không vượt quá 1 tỷ đồng");
        }
        return value;
    }

    public static double nonNegativeMoney(Double value, String field) {
        if (value == null || !Double.isFinite(value) || value < 0 || value > 1_000_000_000D) {
            throw new IllegalArgumentException(field + " phải từ 0 đến 1 tỷ đồng");
        }
        return value;
    }

    public static int range(Integer value, String field, int min, int max) {
        if (value == null || value < min || value > max) {
            throw new IllegalArgumentException(field + " phải từ " + min + " đến " + max);
        }
        return value;
    }

    public static String oneOf(String value, String field, Set<String> allowed) {
        String normalized = requiredText(value, field, 50).toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) throw new IllegalArgumentException(field + " không hợp lệ");
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
