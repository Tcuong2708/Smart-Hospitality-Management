package com.votricuong.mayhotel.enums;

import java.util.Locale;

public enum RoomStatus {
    VACANT("Trống", 1),
    OCCUPIED("Đang lưu trú", 2),
    CLEANING("Chờ dọn/kiểm tra", 3),
    RESERVED("Đã đặt", 4),
    MAINTENANCE("Đang bảo trì", 5);

    private final String label;
    private final int mapCode;

    RoomStatus(String label, int mapCode) {
        this.label = label;
        this.mapCode = mapCode;
    }

    public String getLabel() {
        return label;
    }

    public int getMapCode() {
        return mapCode;
    }

    public static RoomStatus fromStored(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "vacant", "trống", "phòng trống", "available" -> VACANT;
            case "occupied", "in use", "đang ở", "đang có khách", "đang lưu trú", "đã nhận phòng" -> OCCUPIED;
            case "reserved", "đã đặt", "booked" -> RESERVED;
            case "cleaning", "chờ dọn dẹp", "chờ dọn/kiểm tra", "đang dọn" -> CLEANING;
            case "maintenance", "bảo trì", "đang bảo trì", "out of service" -> MAINTENANCE;
            default -> VACANT;
        };
    }

    public static RoomStatus fromApi(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Trạng thái phòng không được để trống");
        }
        try {
            return RoomStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "trống", "phòng trống", "available" -> VACANT;
                case "in use", "đang ở", "đang có khách", "đang lưu trú", "đã nhận phòng" -> OCCUPIED;
                case "đã đặt", "booked" -> RESERVED;
                case "chờ dọn dẹp", "chờ dọn/kiểm tra", "đang dọn" -> CLEANING;
                case "bảo trì", "đang bảo trì", "out of service" -> MAINTENANCE;
                default -> throw new IllegalArgumentException("Trạng thái phòng không hợp lệ");
            };
        }
    }
}
