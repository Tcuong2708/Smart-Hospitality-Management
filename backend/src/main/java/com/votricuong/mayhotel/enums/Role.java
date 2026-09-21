package com.votricuong.mayhotel.enums;

/**
 * Phân quyền người dùng trong hệ thống
 */
public enum Role {
    ADMIN("ADMIN"),
    MANAGER("MANAGER"),
    RECEPTIONIST("RECEPTIONIST"),
    HOUSEKEEPER("HOUSEKEEPER"),
    ACCOUNTANT("ACCOUNTANT"),
    HR("HR"),
    CUSTOMER("CUSTOMER");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
