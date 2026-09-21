package com.votricuong.mayhotel.dto;

import com.votricuong.mayhotel.controllers.HomeController.RoomTypeDTO;
import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingSession {
    private RoomTypeDTO loaiPhong;
    private int soLuong;
    private LocalDate ngayNhan;
    private LocalDate ngayTra;

    public long getSoDem() {
        if (ngayNhan != null && ngayTra != null && ngayTra.isAfter(ngayNhan)) {
            return java.time.temporal.ChronoUnit.DAYS.between(ngayNhan, ngayTra);
        }
        return 0;
    }

    public Double getThanhTien() {
        if (loaiPhong == null || loaiPhong.getPrice() == null) {
            return 0.0;
        }
        return loaiPhong.getPrice() * (soLuong * getSoDem());
    }
}
