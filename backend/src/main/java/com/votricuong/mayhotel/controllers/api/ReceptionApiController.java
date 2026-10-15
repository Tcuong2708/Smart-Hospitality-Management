package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/v1/reception")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ReceptionApiController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingOrderRepository bookingOrderRepository;

    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @Autowired
    private CustomerRepository customerRepository;

    // 1. Lấy Sơ đồ Phòng (Phân loại theo hướng)
    @GetMapping("/rooms/map")
    public ResponseEntity<?> getRoomMap() {
        try {
            List<Room> rooms = roomRepository.findAll();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", rooms);
            response.put("message", "Lấy dữ liệu sơ đồ phòng thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // 2. Kiểm tra trạng thái Khách hàng quét FaceID (Polling API)
    @GetMapping("/bookings/{id}/verification-status")
    public ResponseEntity<?> getFaceVerificationStatus(@PathVariable("id") Long bookingId) {
        try {
            Optional<BookingOrder> bookingOpt = bookingOrderRepository.findById(bookingId);
            if (bookingOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Không tìm thấy Booking."));
            }

            BookingOrder booking = bookingOpt.get();
            boolean isVerified = Boolean.TRUE.equals(booking.getIsFaceVerified());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("bookingId", bookingId);
            response.put("isFaceVerified", isVerified);
            response.put("verificationMethod", booking.getIdentityVerificationMethod() == null
                    ? (isVerified ? "MOBILE_AI" : "PENDING")
                    : booking.getIdentityVerificationMethod());
            if (booking.getIdentityVerifiedAt() != null) {
                response.put("verifiedAt", booking.getIdentityVerifiedAt());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/bookings/{id}/manual-verification")
    public ResponseEntity<?> saveManualVerification(@PathVariable("id") Long bookingId,
                                                     @RequestBody Map<String, Object> payload) {
        try {
            BookingOrder booking = bookingOrderRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Booking."));
            if (booking.getCustomerId() == null) {
                throw new IllegalArgumentException("Booking chưa có hồ sơ khách hàng để cập nhật.");
            }

            String fullName = requiredText(payload, "fullName", "Họ và tên");
            String documentNumber = requiredText(payload, "documentNumber", "Số hộ chiếu / ID");
            String nationality = requiredText(payload, "nationality", "Quốc tịch");
            String dateOfBirth = requiredText(payload, "dateOfBirth", "Ngày sinh");
            String reason = requiredText(payload, "reason", "Lý do nhập tay");

            if (!List.of("FOREIGN_GUEST", "NO_VALID_CCCD", "MOBILE_VERIFICATION_FAILED").contains(reason)) {
                throw new IllegalArgumentException("Lý do nhập tay không hợp lệ.");
            }

            Date dob;
            try {
                dob = Date.from(LocalDate.parse(dateOfBirth)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant());
            } catch (Exception ex) {
                throw new IllegalArgumentException("Ngày sinh không hợp lệ.");
            }

            Customer customer = customerRepository.findById(booking.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ khách hàng."));
            customer.setFullName(fullName);
            customer.setCccd(documentNumber);
            customer.setNationality(nationality);
            customer.setQuocTich(nationality);
            customer.setDob(dob);
            customerRepository.save(customer);

            booking.setIsFaceVerified(true);
            booking.setIdentityVerificationMethod("MANUAL_EXCEPTION");
            booking.setIdentityVerificationReason(reason);
            booking.setIdentityVerifiedAt(new Date());
            bookingOrderRepository.save(booking);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("bookingId", bookingId);
            response.put("isFaceVerified", true);
            response.put("verificationMethod", "MANUAL_EXCEPTION");
            response.put("message", "Đã lưu xác thực nhập tay ngoại lệ.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/bookings/{id}/simulate-verification")
    public ResponseEntity<?> simulateVerification(@PathVariable("id") Long bookingId) {
        try {
            BookingOrder booking = bookingOrderRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Booking."));
            booking.setIsFaceVerified(true);
            booking.setIdentityVerificationMethod("DEMO_SIMULATION");
            booking.setIdentityVerificationReason(null);
            booking.setIdentityVerifiedAt(new Date());
            bookingOrderRepository.save(booking);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "bookingId", bookingId,
                    "isFaceVerified", true,
                    "verificationMethod", "DEMO_SIMULATION"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private String requiredText(Map<String, Object> payload, String key, String label) {
        Object value = payload.get(key);
        String text = value == null ? "" : value.toString().trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(label + " là thông tin bắt buộc.");
        }
        return text;
    }

    // 3. Duyệt nhận phòng & Xếp phòng (Cập nhật Khóa ngoại roomId vào BookingDetail)
    @PostMapping("/bookings/{id}/assign-room")
    public ResponseEntity<?> assignRoomAndCheckIn(@PathVariable("id") Long bookingId, @RequestBody Map<String, Object> payload) {
        try {
            Long roomId = Long.valueOf(payload.get("roomId").toString());
            boolean depositPaid = Boolean.parseBoolean(payload.getOrDefault("depositPaid", "false").toString());

            Optional<BookingOrder> bookingOpt = bookingOrderRepository.findById(bookingId);
            Optional<Room> roomOpt = roomRepository.findById(roomId);

            if (bookingOpt.isEmpty() || roomOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "Không tìm thấy Booking hoặc Phòng."));
            }

            BookingOrder booking = bookingOpt.get();
            Room room = roomOpt.get();

            // Nếu phòng không trống
            if (!"Trống".equalsIgnoreCase(room.getStatus())) {
                return ResponseEntity.status(400).body(Map.of("status", "error", "message", "Phòng đã có người ở hoặc đang bảo trì."));
            }

            // Cập nhật trạng thái phòng (Room)
            room.setStatus("Đang ở");
            roomRepository.save(room);

            // Cập nhật trạng thái phiếu đặt (BookingOrder)
            booking.setStatus("Đang ở");
            bookingOrderRepository.save(booking);

            // Cập nhật Khóa ngoại (roomId) vào BookingDetail và thời gian check-in thực tế
            List<BookingDetail> details = bookingDetailRepository.findByBookingId(bookingId);
            if (!details.isEmpty()) {
                BookingDetail detail = details.get(0);
                detail.setRoomId(roomId); // Thiết lập FK
                detail.setActualIn(new Date());
                bookingDetailRepository.save(detail);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Nhận phòng thành công. Phòng " + room.getName() + " đã được gán.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
