package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.*;
import com.votricuong.mayhotel.dto.ApiResponse;
import com.votricuong.mayhotel.repositories.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api/booking")
@CrossOrigin(origins = "*")
public class BookingApiController {

    private final com.votricuong.mayhotel.repositories.BookingOrderRepository bookingOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final RoomRepository roomRepository;
    private final com.votricuong.mayhotel.repositories.RoomTypeRepository roomTypeRepository;

    public BookingApiController(
            com.votricuong.mayhotel.repositories.BookingOrderRepository bookingOrderRepository,
            InvoiceRepository invoiceRepository,
            RoomRepository roomRepository,
            com.votricuong.mayhotel.repositories.RoomTypeRepository roomTypeRepository) {
        this.bookingOrderRepository = bookingOrderRepository;
        this.invoiceRepository = invoiceRepository;
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> createBooking(@RequestBody Map<String, Object> payload) {
        try {
            Long maPhong = Long.valueOf(payload.get("maPhong").toString());
            Long maKH = Long.valueOf(payload.get("maKH").toString());
            String ghiChu = (String) payload.get("ghiChu");
            
            // Lấy chuỗi ISO 8601 từ Flutter
            String ngayNhanStr = (String) payload.get("ngayNhanPhong");
            String ngayTraStr = (String) payload.get("ngayTraPhong");
            
            if (!ngayNhanStr.endsWith("Z")) ngayNhanStr += "Z";
            if (!ngayTraStr.endsWith("Z")) ngayTraStr += "Z";
            
            Date ngayNhan = Date.from(Instant.parse(ngayNhanStr));
            Date ngayTra = Date.from(Instant.parse(ngayTraStr));
            
            // Logic Web Validation: Ngày nhận >= hiện tại (bỏ qua ms), Trả > Nhận
            if (ngayTra.before(ngayNhan) || ngayTra.equals(ngayNhan)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Ngày trả phòng phải sau ngày nhận phòng"));
            }

            Optional<Room> roomOpt = roomRepository.findById(maPhong);
            if (roomOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Phòng không tồn tại"));
            }

            Room room = roomOpt.get();

            // Tính tiền
            long diffInMillies = Math.abs(ngayTra.getTime() - ngayNhan.getTime());
            long diff = java.util.concurrent.TimeUnit.DAYS.convert(diffInMillies, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (diff == 0) diff = 1;
            double total = room.getPrice() * diff;

            // Lưu BookingOrder
            BookingOrder order = new BookingOrder();
            order.setId(System.currentTimeMillis() % 1000000000L);
            order.setCustomerId(maKH);
            order.setBookingDate(new Date());
            order.setExpectedIn(ngayNhan);
            order.setExpectedOut(ngayTra);
            order.setStatus("Đã xác nhận");
            order.setNotes(ghiChu);
            bookingOrderRepository.save(order);

            // Lưu Invoice để tham chiếu sau này
            Invoice invoice = new Invoice();
            invoice.setId(System.currentTimeMillis() % 1000000000L);
            invoice.setBookingId(order.getId());
            invoice.setTotalAmount(total);
            invoice.setInvoiceStatus("Chờ thanh toán");
            invoice.setCreatedAt(new Date());
            invoiceRepository.save(invoice);
            
            // Đổi trạng thái phòng
            room.setStatus("Reserved");
            roomRepository.save(room);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("maHD", invoice.getId());
            response.put("bookingId", order.getId());
            response.put("tongTien", total);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi Server: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(@RequestParam("maKH") Long maKH) {
        try {
            List<BookingOrder> orders = bookingOrderRepository.findAll().stream()
                    .filter(o -> maKH.equals(o.getCustomerId()))
                    .toList();

            List<Map<String, Object>> data = new ArrayList<>();
            for (BookingOrder o : orders) {
                // Lấy giá tổng từ Invoice (nếu có)
                double tongTien = 0.0;
                Optional<Invoice> invOpt = invoiceRepository.findAll().stream()
                        .filter(inv -> o.getId().equals(inv.getBookingId()))
                        .findFirst();
                if (invOpt.isPresent()) {
                    tongTien = invOpt.get().getTotalAmount();
                }

                Map<String, Object> map = new HashMap<>();
                map.put("tenPhong", "Phòng VIP May Hotel");
                map.put("hinhAnh", "assets/images/h2.jpg");
                map.put("ngayNhanPhong", o.getExpectedIn().toInstant().toString());
                map.put("ngayTraPhong", o.getExpectedOut().toInstant().toString());
                map.put("tongTien", tongTien);
                map.put("trangThai", o.getStatus() != null ? o.getStatus() : "Chờ xác nhận");
                
                data.add(map);
            }

            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi Server"));
        }
    }
}
