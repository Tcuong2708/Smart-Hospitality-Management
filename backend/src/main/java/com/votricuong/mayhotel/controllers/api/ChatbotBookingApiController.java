package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.services.BookingAvailabilityService;
import com.votricuong.mayhotel.services.OnlineBookingPolicy;
import com.votricuong.mayhotel.services.SequenceGeneratorService;
import com.votricuong.mayhotel.services.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/chatbot")
public class ChatbotBookingApiController {

    private final CustomerRepository customerRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingOrderRepository bookingOrderRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final InvoiceRepository invoiceRepository;
    private final BookingAvailabilityService availabilityService;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final VNPayService vnPayService;
    private final String internalToken;

    public ChatbotBookingApiController(CustomerRepository customerRepository,
                                       RoomRepository roomRepository,
                                       RoomTypeRepository roomTypeRepository,
                                       BookingOrderRepository bookingOrderRepository,
                                       BookingDetailRepository bookingDetailRepository,
                                       InvoiceRepository invoiceRepository,
                                       BookingAvailabilityService availabilityService,
                                       SequenceGeneratorService sequenceGeneratorService,
                                       VNPayService vnPayService,
                                       @Value("${chatbot.internal.token:}") String internalToken) {
        this.customerRepository = customerRepository;
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.invoiceRepository = invoiceRepository;
        this.availabilityService = availabilityService;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.vnPayService = vnPayService;
        this.internalToken = internalToken;
    }

    @PostMapping("/availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestBody Map<String, Object> payload) {
        if (internalToken.isBlank()) {
            return ResponseEntity.status(503).body(Map.of("message", "Chatbot nội bộ chưa được cấu hình"));
        }
        if (!internalToken.equals(token)) {
            return ResponseEntity.status(403).body(Map.of("message", "Internal token không hợp lệ"));
        }
        try {
            LocalDate checkIn = LocalDate.parse(required(payload, "checkIn"));
            LocalDate checkOut = LocalDate.parse(required(payload, "checkOut"));
            int guestCount = com.votricuong.mayhotel.utils.InputValidator.range(
                    number(payload.get("guestCount"), 1), "Số khách", 1, 20);
            com.votricuong.mayhotel.utils.InputValidator.stay(checkIn, checkOut);
            Date expectedIn = Date.from(checkIn.atStartOfDay(ZoneOffset.UTC).toInstant());
            Date expectedOut = Date.from(checkOut.atStartOfDay(ZoneOffset.UTC).toInstant());
            availabilityService.validateStay(expectedIn, expectedOut);

            List<Map<String, Object>> availableTypes = new ArrayList<>();
            for (RoomType roomType : roomTypeRepository.findAll()) {
                if (roomType.getMaxOccupancy() != null && roomType.getMaxOccupancy() < guestCount) {
                    continue;
                }
                List<Room> rooms = availabilityService.findAvailableRooms(
                        roomType.getId(), expectedIn, expectedOut);
                if (rooms.isEmpty()) continue;

                double minPrice = rooms.stream()
                        .map(Room::getPrice)
                        .filter(java.util.Objects::nonNull)
                        .min(Double::compareTo)
                        .orElse(0.0);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("roomTypeId", roomType.getId());
                row.put("name", roomType.getName());
                row.put("maxOccupancy", roomType.getMaxOccupancy());
                row.put("availableCount", rooms.size());
                row.put("price", minPrice);
                availableTypes.add(row);
            }
            return ResponseEntity.ok(Map.of("roomTypes", availableTypes));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", ex.getMessage() == null ? "Không thể kiểm tra phòng trống" : ex.getMessage()));
        }
    }

    @PostMapping("/bookings")
    public ResponseEntity<Map<String, Object>> createBooking(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        if (internalToken.isBlank()) {
            return ResponseEntity.status(503).body(Map.of("message", "Chatbot nội bộ chưa được cấu hình"));
        }
        if (!internalToken.equals(token)) {
            return ResponseEntity.status(403).body(Map.of("message", "Internal token không hợp lệ"));
        }
        if (!vnPayService.isEnabled()) {
            return ResponseEntity.status(503).body(Map.of(
                    "message", "Cổng VNPay chưa được cấu hình; chưa thể tạo đặt phòng trực tuyến"));
        }
        try {
            String rawName = required(payload, "customerName");
            String rawPhone = required(payload, "phone");
            String rawRequestedType = required(payload, "roomType");
            LocalDate checkIn = LocalDate.parse(required(payload, "checkIn"));
            LocalDate checkOut = LocalDate.parse(required(payload, "checkOut"));
            final String name = com.votricuong.mayhotel.utils.InputValidator.requiredText(rawName, "Họ và tên", 100);
            final String phone = com.votricuong.mayhotel.utils.InputValidator.phone(rawPhone);
            final String requestedType = com.votricuong.mayhotel.utils.InputValidator.requiredText(rawRequestedType, "Loại phòng", 100)
                    .toLowerCase(Locale.ROOT);
            com.votricuong.mayhotel.utils.InputValidator.stay(checkIn, checkOut);
            int adultCount = number(payload.get("adultCount"), number(payload.get("guestCount"), 1));
            int childCount = nonNegativeNumber(payload.get("childCount"), 0);
            int infantCount = nonNegativeNumber(payload.get("infantCount"), 0);
            int guestCount = OnlineBookingPolicy.totalGuests(adultCount, childCount, infantCount);
            com.votricuong.mayhotel.utils.InputValidator.range(guestCount, "Số khách", 1, 20);
            String note = com.votricuong.mayhotel.utils.InputValidator.optionalText(
                    payload.get("note") == null ? null : payload.get("note").toString(), "Ghi chú", 500);
            Date expectedIn = Date.from(checkIn.atStartOfDay(ZoneOffset.UTC).toInstant());
            Date expectedOut = Date.from(checkOut.atStartOfDay(ZoneOffset.UTC).toInstant());
            availabilityService.validateStay(expectedIn, expectedOut);

            RoomType roomType = roomTypeRepository.findAll().stream()
                    .filter(type -> type.getName() != null && (type.getName().toLowerCase(Locale.ROOT).contains(requestedType)
                            || requestedType.contains(type.getName().toLowerCase(Locale.ROOT))))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại phòng phù hợp"));
            int maxOccupancy = roomType.getMaxOccupancy() == null ? 1 : roomType.getMaxOccupancy();
            if (guestCount > maxOccupancy) {
                throw new IllegalArgumentException(
                        "Số khách vượt quá sức chứa tối đa " + maxOccupancy + " người của loại phòng");
            }
            Room room = availabilityService.findAvailableRooms(roomType.getId(), expectedIn, expectedOut).stream()
                    .findFirst().orElseThrow(() -> new IllegalStateException("Loại phòng đã hết trong khoảng ngày yêu cầu"));

            Customer customer = customerRepository.findByPhone(phone).stream().findFirst().orElseGet(() -> {
                Customer created = new Customer();
                created.setId(sequenceGeneratorService.generateSequence("customers_sequence"));
                created.setFullName(name);
                created.setPhone(phone);
                return customerRepository.save(created);
            });
            if (customer.getFullName() == null || customer.getFullName().isBlank()) {
                customer.setFullName(name);
                customerRepository.save(customer);
            }

            BookingOrder order = new BookingOrder();
            order.setId(sequenceGeneratorService.generateSequence("booking_orders_sequence"));
            order.setCustomerId(customer.getId());
            order.setBookingDate(new Date());
            order.setExpectedIn(expectedIn);
            order.setExpectedOut(expectedOut);
            order.setStatus(OnlineBookingPolicy.BOOKING_AWAITING_DEPOSIT);
            order.setBookingSource("CHATBOT");
            order.setRiskAnalysisStatus("PENDING");
            order.setGuestCount(guestCount);
            order.setAdultCount(adultCount);
            order.setChildCount(childCount);
            order.setInfantCount(infantCount);
            order.setVerifiedGuestCount(0);
            order.setPaymentExpiresAt(OnlineBookingPolicy.paymentExpiryFromNow());
            order.setNotes(note.isBlank() ? "[CHATBOT]" : "[CHATBOT] " + note);
            bookingOrderRepository.save(order);

            BookingDetail detail = new BookingDetail();
            detail.setId(sequenceGeneratorService.generateSequence("booking_details_sequence"));
            detail.setBookingId(order.getId());
            detail.setRoomId(room.getId());
            detail.setUnitPrice(room.getPrice());
            bookingDetailRepository.save(detail);

            long nights = Math.max(1, ChronoUnit.DAYS.between(checkIn, checkOut));
            Invoice invoice = new Invoice();
            invoice.setId(sequenceGeneratorService.generateSequence("invoices_sequence"));
            invoice.setBookingId(order.getId());
            invoice.setCreatedAt(new Date());
            invoice.setGuestName(customer.getFullName());
            invoice.setPhone(customer.getPhone());
            invoice.setRoomId(room.getId());
            invoice.setCheckInDate(expectedIn);
            invoice.setCheckOutDate(expectedOut);
            invoice.setTotalAmount((room.getPrice() == null ? 0.0 : room.getPrice()) * nights);
            long depositAmount = OnlineBookingPolicy.depositAmount(invoice.getTotalAmount());
            invoice.setPayMethod("VNPAY_DEPOSIT");
            invoice.setInvoiceStatus("Awaiting Deposit");
            invoice.setIsPaid(false);
            invoice.setDepositRate(OnlineBookingPolicy.DEPOSIT_RATE);
            invoice.setDepositBaseAmount(Math.round(invoice.getTotalAmount()));
            invoice.setDepositRequiredAmount(depositAmount);
            invoice.setDepositPaidAmount(0L);
            invoice.setRemainingAmount(Math.max(0L, Math.round(invoice.getTotalAmount()) - depositAmount));
            invoice.setDepositStatus(OnlineBookingPolicy.DEPOSIT_PENDING);
            invoice.setSurcharge(0.0);
            invoiceRepository.save(invoice);

            String paymentUrl = vnPayService.createPaymentUrl(invoice, request);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("invoiceId", invoice.getId());
            response.put("depositAmount", depositAmount);
            response.put("remainingAmount", invoice.getRemainingAmount());
            response.put("totalAmount", invoice.getTotalAmount());
            response.put("paymentUrl", paymentUrl);
            response.put("paymentExpiresAt", order.getPaymentExpiresAt());
            response.put("status", order.getStatus());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage() == null ? "Không thể tạo đặt phòng" : ex.getMessage()));
        }
    }

    private String required(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || value.toString().isBlank()) throw new IllegalArgumentException("Thiếu " + key);
        return value.toString().trim();
    }

    private int number(Object value, int fallback) {
        if (value instanceof Number) return Math.max(((Number) value).intValue(), 1);
        try { return Math.max(Integer.parseInt(String.valueOf(value)), 1); }
        catch (Exception ignored) { return fallback; }
    }

    private int nonNegativeNumber(Object value, int fallback) {
        if (value instanceof Number) return Math.max(((Number) value).intValue(), 0);
        try { return Math.max(Integer.parseInt(String.valueOf(value)), 0); }
        catch (Exception ignored) { return fallback; }
    }
}
