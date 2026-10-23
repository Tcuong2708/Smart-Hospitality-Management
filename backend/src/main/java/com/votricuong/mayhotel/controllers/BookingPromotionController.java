package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.services.PromotionService;
import com.votricuong.mayhotel.dto.BookingSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/booking")
public class BookingPromotionController {

    private final PromotionService promotionService;

    public BookingPromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping("/quote-voucher")
    public ResponseEntity<?> quoteVoucherFromSession(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "ngayNhan", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkIn,
            @RequestParam(value = "ngayTra", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkOut,
            HttpSession session) {
        BookingSession booking = (BookingSession) session.getAttribute("CurrentBooking");
        if (booking == null || booking.getLoaiPhong() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false, "discount", 0, "message", "Phiên đặt phòng đã hết."));
        }
        if (code == null || code.isBlank()) {
            return ResponseEntity.ok(Map.of("valid", true, "discount", 0, "message", ""));
        }
        LocalDate arrival = checkIn == null ? booking.getNgayNhan() : checkIn;
        LocalDate departure = checkOut == null ? booking.getNgayTra() : checkOut;
        if (arrival == null || departure == null || !departure.isAfter(arrival)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false, "discount", 0, "message", "Ngày lưu trú không hợp lệ."));
        }
        int nights = (int) ChronoUnit.DAYS.between(arrival, departure);
        double roomAmount = (booking.getLoaiPhong().getPrice() == null ? 0D : booking.getLoaiPhong().getPrice())
                * Math.max(1, booking.getSoLuong()) * nights;
        PromotionService.Quote quote = promotionService.bestQuote(
                roomAmount, Math.max(1, booking.getSoLuong()), Math.max(1, booking.getGuestCount()), nights,
                new Date(), Date.from(arrival.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                booking.getLoaiPhong().getMaLoai(), code.trim());
        if (quote.promotion() == null) {
            return ResponseEntity.ok(Map.of("valid", false, "discount", 0,
                    "message", "Mã voucher không hợp lệ hoặc không đủ điều kiện."));
        }
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "discount", quote.discount(),
                "promotionId", quote.promotion().getId(),
                "message", "Đã áp dụng mã " + quote.promotion().getCode() + "."));
    }

    @PostMapping("/quote-voucher")
    public ResponseEntity<?> quoteVoucher(@RequestBody QuoteRequest request) {
        if (request.roomAmount() < 0 || request.roomCount() < 1 || request.guestCount() < 1
                || request.nights() < 1 || request.stayDate() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Thông tin lưu trú hoặc giá phòng không hợp lệ."));
        }

        PromotionService.Quote quote = promotionService.bestQuote(
                request.roomAmount(), request.roomCount(), request.guestCount(), request.nights(),
                request.bookingDate() == null ? new Date() : request.bookingDate(), request.stayDate(),
                request.roomTypeId(), request.voucherCode());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        data.put("promotionId", quote.promotion() == null ? null : quote.promotion().getId());
        data.put("promotionCode", quote.promotion() == null ? null : quote.promotion().getCode());
        data.put("promotionName", quote.promotion() == null ? null : quote.promotion().getName());
        data.put("discount", quote.discount());
        data.put("status", quote.status());
        return ResponseEntity.ok(data);
    }

    public record QuoteRequest(double roomAmount, int roomCount, int guestCount, int nights,
                               Date bookingDate, Date stayDate, Long roomTypeId, String voucherCode) {
    }
}
