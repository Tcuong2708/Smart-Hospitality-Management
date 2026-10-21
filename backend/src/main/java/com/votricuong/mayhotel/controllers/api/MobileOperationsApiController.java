package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.LoyalCustomer;
import com.votricuong.mayhotel.documents.LoyaltyPolicy;
import com.votricuong.mayhotel.documents.Review;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.dto.ApiResponse;
import com.votricuong.mayhotel.enums.RoomStatus;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.LoyalCustomerRepository;
import com.votricuong.mayhotel.repositories.LoyaltyPolicyRepository;
import com.votricuong.mayhotel.repositories.ReviewRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.services.BookingAvailabilityService;
import com.votricuong.mayhotel.services.BookingCancellationService;
import com.votricuong.mayhotel.services.LoyaltyService;
import com.votricuong.mayhotel.services.NoShowPredictionService;
import com.votricuong.mayhotel.services.MobileTokenService;
import com.votricuong.mayhotel.services.RoomStatusService;
import com.votricuong.mayhotel.services.SequenceGeneratorService;
import com.votricuong.mayhotel.utils.InputValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Mobile-only workflow API.  It intentionally does not reuse the MVC
 * controllers so that mobile authentication and ownership checks cannot be
 * bypassed by posting identifiers that came from the device.
 */
@RestController
@RequestMapping("/api/v1/mobile/operations")
public class MobileOperationsApiController {
    private static final Set<String> CHECKED_IN = Set.of(
            "đã nhận phòng", "đang ở", "đang lưu trú", "checked-in", "occupied", "in use");
    private static final Set<String> CHECKED_OUT = Set.of(
            "đã trả phòng", "completed", "checked-out");

    private final MobileTokenService tokenService;
    private final BookingOrderRepository bookingOrderRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReviewRepository reviewRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final LoyalCustomerRepository loyalCustomerRepository;
    private final LoyaltyPolicyRepository loyaltyPolicyRepository;
    private final BookingCancellationService cancellationService;
    private final BookingAvailabilityService availabilityService;
    private final RoomStatusService roomStatusService;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final LoyaltyService loyaltyService;
    private final NoShowPredictionService noShowPredictionService;

    public MobileOperationsApiController(
            MobileTokenService tokenService,
            BookingOrderRepository bookingOrderRepository,
            BookingDetailRepository bookingDetailRepository,
            CustomerRepository customerRepository,
            InvoiceRepository invoiceRepository,
            ReviewRepository reviewRepository,
            RoomRepository roomRepository,
            RoomTypeRepository roomTypeRepository,
            LoyalCustomerRepository loyalCustomerRepository,
            LoyaltyPolicyRepository loyaltyPolicyRepository,
            BookingCancellationService cancellationService,
            BookingAvailabilityService availabilityService,
            RoomStatusService roomStatusService,
            SequenceGeneratorService sequenceGeneratorService,
            LoyaltyService loyaltyService,
            NoShowPredictionService noShowPredictionService) {
        this.tokenService = tokenService;
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.customerRepository = customerRepository;
        this.invoiceRepository = invoiceRepository;
        this.reviewRepository = reviewRepository;
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.loyalCustomerRepository = loyalCustomerRepository;
        this.loyaltyPolicyRepository = loyaltyPolicyRepository;
        this.cancellationService = cancellationService;
        this.availabilityService = availabilityService;
        this.roomStatusService = roomStatusService;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.loyaltyService = loyaltyService;
        this.noShowPredictionService = noShowPredictionService;
    }

    @GetMapping("/customer/loyalty")
    public ResponseEntity<?> loyalty(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return customerAction(authorization, (claims, customer) -> {
            LoyaltyPolicy policy = loyaltyPolicyRepository.findById("singleton")
                    .orElse(LoyaltyPolicy.builder().id("singleton").moneyPerPoint(100000D)
                            .silverThreshold(100).goldThreshold(500).platinumThreshold(1000).build());
            LoyalCustomer loyal = loyalCustomerRepository.findByCustomerId(customer.getId()).orElse(null);
            int points = customer.getPoints() == null ? 0 : customer.getPoints();
            String tier = loyal == null || loyal.getTier() == null ? tierName(customer.getTierId()) : loyal.getTier();

            List<Map<String, Object>> history = new ArrayList<>();
            for (Invoice invoice : invoiceRepository.findByUserId(claims.userId())) {
                if (invoice.getEarnedPoints() != null && invoice.getEarnedPoints() > 0) {
                    history.add(pointHistory(invoice, "EARN", invoice.getEarnedPoints()));
                }
                if (invoice.getRedeemedPoints() != null && invoice.getRedeemedPoints() > 0) {
                    history.add(pointHistory(invoice, "REDEEM", -invoice.getRedeemedPoints()));
                }
            }
            history.sort(Comparator.comparing(
                    value -> (Date) value.get("occurredAt"), Comparator.nullsLast(Comparator.reverseOrder())));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("points", points);
            data.put("tier", tier);
            data.put("nextTierPoints", nextTierPoints(points, policy));
            data.put("moneyPerPoint", policy.getMoneyPerPoint());
            data.put("history", history);
            return ok("Tải điểm thành viên thành công", data);
        });
    }

    @PostMapping("/customer/bookings/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Long bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) Map<String, Object> payload) {
        return customerAction(authorization, (claims, customer) -> {
            BookingOrder order = ownedBooking(bookingId, customer);
            cancellationService.validateCancellable(order);
            double penaltyRate = cancellationPenalty(order);
            boolean confirmed = payload != null && Boolean.parseBoolean(String.valueOf(payload.getOrDefault("confirmed", false)));
            if (!confirmed) {
                return ok("Xác nhận chính sách hủy phòng", Map.of(
                        "bookingId", bookingId,
                        "penaltyRate", penaltyRate,
                        "requiresConfirmation", true));
            }
            String reason = payload == null ? null : string(payload.get("reason"));
            BookingOrder cancelled = cancellationService.cancel(
                    bookingId, BookingCancellationService.CancellationType.CUSTOMER, reason);
            Invoice invoice = firstInvoice(bookingId).orElse(null);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("bookingId", bookingId);
            data.put("status", cancelled.getStatus());
            data.put("penaltyRate", invoice == null ? penaltyRate : value(invoice.getPenaltyRate()));
            data.put("penaltyAmount", invoice == null ? 0D : value(invoice.getPenaltyAmount()));
            data.put("refundAmount", invoice == null ? 0D : value(invoice.getRefundAmount()));
            return ok("Hủy đặt phòng thành công", data);
        });
    }

    @PostMapping("/customer/bookings/{bookingId}/reviews")
    public ResponseEntity<?> createReview(
            @PathVariable Long bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> payload) {
        return customerAction(authorization, (claims, customer) -> {
            BookingOrder order = ownedBooking(bookingId, customer);
            if (!hasStatus(order, CHECKED_OUT)) {
                throw new IllegalStateException("Chỉ được đánh giá sau khi hoàn tất lưu trú");
            }
            if (reviewRepository.findByBookingId(bookingId).isPresent()) {
                throw new IllegalStateException("Kỳ lưu trú này đã được đánh giá");
            }
            int rating = intValue(payload.get("rating"), "Điểm đánh giá");
            if (rating < 1 || rating > 5) throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5");
            String content = InputValidator.optionalText(string(payload.get("content")), "Nội dung", 1000);
            Review review = Review.builder()
                    .id(sequenceGeneratorService.generateSequence("reviews_sequence"))
                    .bookingId(bookingId)
                    .customerId(customer.getId())
                    .rating(rating)
                    .content(content)
                    .reviewDate(new Date())
                    .status("Hiện")
                    .build();
            return ok("Đánh giá thành công", reviewRepository.save(review));
        });
    }

    @GetMapping("/reception/bookings")
    public ResponseEntity<?> receptionBookings(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "pending") String view) {
        return roleAction(authorization, Set.of(3L), claims -> {
            List<Map<String, Object>> items = bookingOrderRepository.findAll().stream()
                    .filter(order -> "active".equalsIgnoreCase(view) ? hasStatus(order, CHECKED_IN) : isPending(order))
                    .sorted(Comparator.comparing(
                            BookingOrder::getExpectedIn, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(this::bookingDto)
                    .toList();
            return ok("Tải danh sách lễ tân thành công", items);
        });
    }

    @GetMapping("/reception/room-types")
    public ResponseEntity<?> receptionRoomTypes(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return roleAction(authorization, Set.of(3L), claims -> ok(
                "Tải loại phòng thành công",
                roomTypeRepository.findAll().stream().map(type -> Map.of(
                        "id", type.getId(),
                        "name", type.getName() == null ? "Loại phòng #" + type.getId() : type.getName(),
                        "maxOccupancy", type.getMaxOccupancy() == null ? 1 : type.getMaxOccupancy()
                )).toList()));
    }

    @GetMapping("/reception/available-rooms")
    public ResponseEntity<?> receptionAvailableRooms(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam Long roomTypeId,
            @RequestParam String checkIn,
            @RequestParam String checkOut) {
        return roleAction(authorization, Set.of(3L), claims -> {
            Date from = date(checkIn, true);
            Date to = date(checkOut, true);
            List<Map<String, Object>> rooms = availabilityService.findAvailableRooms(roomTypeId, from, to).stream()
                    .filter(room -> RoomStatus.fromStored(room.getStatus()) == RoomStatus.VACANT)
                    .map(this::roomDto)
                    .toList();
            return ok("Tải phòng trống thành công", rooms);
        });
    }

    @PostMapping("/reception/direct-bookings")
    public synchronized ResponseEntity<?> createDirectBooking(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> payload) {
        return roleAction(authorization, Set.of(3L), claims -> {
            String guestName = InputValidator.requiredText(string(payload.get("guestName")), "Họ tên", 100);
            String phone = InputValidator.phone(string(payload.get("phone")));
            String note = InputValidator.optionalText(string(payload.get("note")), "Ghi chú", 500);
            Long roomId = longValue(payload.get("roomId"), "Mã phòng");
            int guestCount = intValue(payload.get("guestCount"), "Số khách");
            if (guestCount < 1 || guestCount > 20) {
                throw new IllegalArgumentException("Số khách phải từ 1 đến 20");
            }
            Date from = date(string(payload.get("checkIn")), true);
            Date to = date(string(payload.get("checkOut")), true);
            availabilityService.validateStay(from, to);

            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng"));
            RoomType roomType = roomTypeRepository.findById(room.getRoomTypeId())
                    .orElseThrow(() -> new IllegalStateException("Phòng chưa có loại phòng hợp lệ"));
            int capacity = roomType.getMaxOccupancy() == null ? 1 : roomType.getMaxOccupancy();
            if (guestCount > capacity) {
                throw new IllegalArgumentException("Số khách vượt sức chứa tối đa " + capacity + " người");
            }
            if (RoomStatus.fromStored(room.getStatus()) != RoomStatus.VACANT
                    || !availabilityService.isRoomAvailable(roomId, from, to)) {
                throw new IllegalStateException("Phòng không còn trống trong thời gian đã chọn");
            }

            Customer customer = customerRepository.findByPhone(phone).stream().findFirst().orElse(null);
            if (customer == null) {
                customer = Customer.builder()
                        .id(sequenceGeneratorService.generateSequence("customers_sequence"))
                        .fullName(guestName)
                        .phone(phone)
                        .build();
            } else {
                customer.setFullName(guestName);
            }
            customerRepository.save(customer);

            long nights = java.time.temporal.ChronoUnit.DAYS.between(
                    from.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    to.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            double unitPrice = InputValidator.positiveMoney(room.getPrice(), "Giá phòng");
            long bookingId = sequenceGeneratorService.generateSequence("booking_orders_sequence");
            BookingOrder order = BookingOrder.builder()
                    .id(bookingId)
                    .customerId(customer.getId())
                    .bookingDate(new Date())
                    .expectedIn(from)
                    .expectedOut(to)
                    .status("Reserved")
                    .notes(note)
                    .bookingSource("RECEPTION_MOBILE")
                    .riskAnalysisStatus("PENDING")
                    .guestCount(guestCount)
                    .verifiedGuestCount(0)
                    .isFaceVerified(false)
                    .guestVerifications(new ArrayList<>())
                    .build();
            bookingOrderRepository.save(order);

            BookingDetail detail = BookingDetail.builder()
                    .id(sequenceGeneratorService.generateSequence("booking_details_sequence"))
                    .bookingId(bookingId)
                    .roomId(roomId)
                    .unitPrice(unitPrice)
                    .build();
            bookingDetailRepository.save(detail);

            Invoice invoice = Invoice.builder()
                    .id(sequenceGeneratorService.generateSequence("invoices_sequence"))
                    .bookingId(bookingId)
                    .staffId(claims.userId())
                    .userId(customer.getUserId())
                    .createdAt(new Date())
                    .totalAmount(unitPrice * nights)
                    .payMethod("PAY_AT_HOTEL")
                    .invoiceStatus("Reserved")
                    .guestName(guestName)
                    .phone(phone)
                    .roomId(roomId)
                    .checkInDate(from)
                    .checkOutDate(to)
                    .isPaid(false)
                    .surcharge(0D)
                    .note(note)
                    .build();
            invoiceRepository.save(invoice);
            noShowPredictionService.analyzeAndSave(bookingId);
            return ok("Đặt phòng trực tiếp thành công", Map.of(
                    "booking", bookingDto(order),
                    "invoice", invoiceDto(invoice)));
        });
    }

    @GetMapping("/reception/bookings/{bookingId}/available-rooms")
    public ResponseEntity<?> availableRooms(
            @PathVariable Long bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return roleAction(authorization, Set.of(3L), claims -> {
            BookingOrder order = requiredBooking(bookingId);
            Long requiredType = bookedRoomType(bookingId);
            if (requiredType == null) throw new IllegalStateException("Phiếu chưa xác định loại phòng đã đặt");
            List<Map<String, Object>> rooms = availabilityService
                    .findAvailableRoomsExcludingBooking(
                            requiredType, order.getExpectedIn(), order.getExpectedOut(), bookingId).stream()
                    .filter(room -> RoomStatus.fromStored(room.getStatus()) == RoomStatus.VACANT)
                    .map(this::roomDto)
                    .toList();
            return ok("Tải phòng phù hợp thành công", rooms);
        });
    }

    @GetMapping("/reception/bookings/{bookingId}/change-room-options")
    public ResponseEntity<?> changeRoomOptions(
            @PathVariable Long bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return roleAction(authorization, Set.of(3L), claims -> {
            BookingOrder order = requiredBooking(bookingId);
            if (!hasStatus(order, CHECKED_IN)) {
                throw new IllegalStateException("Chỉ đổi phòng cho khách đang lưu trú");
            }
            Date from = new Date();
            Date to = order.getExpectedOut();
            if (to == null || !to.after(from)) {
                to = Date.from(from.toInstant().plus(java.time.Duration.ofDays(1)));
            }
            List<Map<String, Object>> rooms = availabilityService
                    .findAvailableRoomsExcludingBooking(null, from, to, bookingId).stream()
                    .filter(room -> RoomStatus.fromStored(room.getStatus()) == RoomStatus.VACANT)
                    .map(this::roomDto)
                    .toList();
            return ok("Tải phòng có thể đổi thành công", rooms);
        });
    }

    @PostMapping("/reception/bookings/{bookingId}/assign-room")
    public synchronized ResponseEntity<?> assignRoom(
            @PathVariable Long bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> payload) {
        return roleAction(authorization, Set.of(3L), claims -> {
            BookingOrder order = requiredBooking(bookingId);
            if (Boolean.FALSE.equals(order.getIsFaceVerified())
                    || effectiveVerifiedGuests(order) < effectiveGuestCount(order)) {
                throw new IllegalStateException("Chưa xác thực đủ danh tính của tất cả khách lưu trú");
            }
            Invoice invoice = firstInvoice(bookingId)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy hóa đơn đặt phòng"));
            if (!Boolean.TRUE.equals(invoice.getIsPaid()) && !isPayAtHotel(invoice)) {
                throw new IllegalStateException("Tiền cọc chưa được xác nhận");
            }
            Long roomId = longValue(payload.get("roomId"), "Mã phòng");
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng"));
            Long requiredType = bookedRoomType(bookingId);
            if (!Objects.equals(requiredType, room.getRoomTypeId())) {
                throw new IllegalStateException("Phòng được chọn không đúng loại phòng khách đã đặt");
            }
            if (RoomStatus.fromStored(room.getStatus()) != RoomStatus.VACANT) {
                throw new IllegalStateException("Phòng không còn ở trạng thái trống");
            }
            boolean available = availabilityService.findAvailableRoomsExcludingBooking(
                            requiredType, order.getExpectedIn(), order.getExpectedOut(), bookingId).stream()
                    .anyMatch(candidate -> roomId.equals(candidate.getId()));
            if (!available) throw new IllegalStateException("Phòng vừa được đặt, vui lòng chọn phòng khác");

            List<BookingDetail> details = bookingDetailRepository.findByBookingId(bookingId);
            if (details.isEmpty()) throw new IllegalStateException("Phiếu không có chi tiết phòng");
            BookingDetail detail = details.get(0);
            detail.setRoomId(roomId);
            detail.setActualIn(new Date());
            bookingDetailRepository.save(detail);
            room.setStatus(RoomStatus.OCCUPIED.getLabel());
            roomRepository.save(room);
            order.setStatus("Đã nhận phòng");
            order.setAutoCancelTime(null);
            bookingOrderRepository.save(order);
            return ok("Duyệt nhận phòng thành công", bookingDto(order));
        });
    }

    @PostMapping("/reception/bookings/{bookingId}/manual-verification")
    public synchronized ResponseEntity<?> manualVerification(
            @PathVariable Long bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> payload) {
        return roleAction(authorization, Set.of(3L), claims -> {
            BookingOrder order = requiredBooking(bookingId);
            if (!isPending(order)) throw new IllegalStateException("Phiếu không còn chờ nhận phòng");
            int guestCount = effectiveGuestCount(order);
            int guestIndex = intValue(payload.get("guestIndex"), "Vị trí khách");
            if (guestIndex < 0 || guestIndex >= guestCount) {
                throw new IllegalArgumentException("Vị trí khách xác thực không hợp lệ");
            }
            String fullName = InputValidator.requiredText(string(payload.get("fullName")), "Họ tên", 100);
            String documentNumber = InputValidator.requiredText(
                    string(payload.get("documentNumber")), "Số hộ chiếu/giấy tờ", 30);
            if (!documentNumber.matches("[A-Za-z0-9.-]{4,30}")) {
                throw new IllegalArgumentException("Số giấy tờ chỉ gồm 4-30 chữ, số, dấu chấm hoặc gạch ngang");
            }
            String nationality = InputValidator.requiredText(
                    string(payload.get("nationality")), "Quốc tịch", 60);
            String reason = InputValidator.requiredText(
                    string(payload.get("reason")), "Lý do xác thực thủ công", 300);
            Date dob = null;
            String dobRaw = string(payload.get("dob"));
            if (dobRaw != null && !dobRaw.isBlank()) {
                LocalDate birthDate = LocalDate.parse(dobRaw);
                if (birthDate.isAfter(LocalDate.now())) {
                    throw new IllegalArgumentException("Ngày sinh không được ở tương lai");
                }
                dob = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            }

            List<Map<String, Object>> verifications = order.getGuestVerifications() == null
                    ? new ArrayList<>() : new ArrayList<>(order.getGuestVerifications());
            boolean duplicate = verifications.stream().anyMatch(item ->
                    !Objects.equals(asInteger(item.get("guestIndex")), guestIndex)
                            && documentNumber.equalsIgnoreCase(string(item.get("documentNumber"))));
            if (duplicate) throw new IllegalArgumentException("Giấy tờ đã dùng cho một khách khác");

            Map<String, Object> verification = new LinkedHashMap<>();
            verification.put("guestIndex", guestIndex);
            verification.put("fullName", fullName);
            verification.put("documentNumber", documentNumber);
            verification.put("nationality", nationality);
            if (dob != null) verification.put("dob", dob);
            verification.put("reason", reason);
            verification.put("method", "MANUAL_EXCEPTION");
            verification.put("verifiedAt", new Date());
            verification.put("verifiedBy", claims.userId());
            verifications.removeIf(item -> Objects.equals(asInteger(item.get("guestIndex")), guestIndex));
            verifications.add(verification);

            int verifiedCount = (int) verifications.stream()
                    .map(item -> asInteger(item.get("guestIndex")))
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
            order.setGuestVerifications(verifications);
            order.setVerifiedGuestCount(verifiedCount);
            order.setIsFaceVerified(verifiedCount >= guestCount);
            order.setIdentityVerificationMethod("MANUAL_EXCEPTION");
            order.setIdentityVerificationReason(reason);
            order.setIdentityVerifiedAt(new Date());
            bookingOrderRepository.save(order);

            Date verifiedDob = dob;
            if (guestIndex == 0) {
                customerRepository.findById(order.getCustomerId()).ifPresent(customer -> {
                    customer.setFullName(fullName);
                    customer.setCccd(documentNumber);
                    customer.setNationality(nationality);
                    customer.setQuocTich(nationality);
                    if (verifiedDob != null) customer.setDob(verifiedDob);
                    customerRepository.save(customer);
                });
            }
            return ok("Đã ghi nhận xác thực thủ công", bookingDto(order));
        });
    }

    @GetMapping("/reception/invoices")
    public ResponseEntity<?> invoices(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status) {
        return roleAction(authorization, Set.of(3L), claims -> {
            String keyword = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
            String state = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
            List<Map<String, Object>> data = invoiceRepository.findAll().stream()
                    .filter(invoice -> state.isEmpty() || string(invoice.getInvoiceStatus()).toLowerCase(Locale.ROOT).contains(state))
                    .filter(invoice -> keyword.isEmpty()
                            || string(invoice.getId()).contains(keyword)
                            || string(invoice.getGuestName()).toLowerCase(Locale.ROOT).contains(keyword)
                            || string(invoice.getPhone()).contains(keyword))
                    .sorted(Comparator.comparing(Invoice::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(this::invoiceDto)
                    .toList();
            return ok("Tải hóa đơn thành công", data);
        });
    }

    @PostMapping("/reception/invoices/{invoiceId}/cash-payment")
    public synchronized ResponseEntity<?> cashPayment(
            @PathVariable Long invoiceId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> payload) {
        return roleAction(authorization, Set.of(3L), claims -> {
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
            if (Boolean.TRUE.equals(invoice.getIsPaid())) throw new IllegalStateException("Hóa đơn đã thanh toán");
            double total = value(invoice.getTotalAmount());
            double received = doubleValue(payload.get("amountReceived"), "Số tiền nhận");
            if (received < total) throw new IllegalArgumentException("Số tiền khách đưa chưa đủ");
            invoice.setPayMethod("CASH");
            invoice.setIsPaid(true);
            invoice.setInvoiceStatus("Paid");
            int earnedPoints = Boolean.parseBoolean(String.valueOf(payload.getOrDefault("earnPoints", false)))
                    ? loyaltyService.awardPointsForPaidInvoice(invoice) : 0;
            invoiceRepository.save(invoice);
            return ok("Thanh toán tiền mặt thành công", Map.of(
                    "invoice", invoiceDto(invoice),
                    "change", received - total,
                    "earnedPoints", earnedPoints));
        });
    }

    @PostMapping("/reception/bookings/{bookingId}/checkout")
    public synchronized ResponseEntity<?> checkout(
            @PathVariable Long bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) Map<String, Object> payload) {
        return roleAction(authorization, Set.of(3L), claims -> {
            BookingOrder order = requiredBooking(bookingId);
            if (!hasStatus(order, CHECKED_IN)) throw new IllegalStateException("Khách không ở trạng thái đang lưu trú");
            Invoice invoice = firstInvoice(bookingId)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy hóa đơn"));
            double surcharge = payload == null ? 0D : optionalDouble(payload.get("surcharge"));
            if (surcharge < 0) throw new IllegalArgumentException("Phụ thu không được âm");
            if (surcharge > 0) {
                invoice.setSurcharge(value(invoice.getSurcharge()) + surcharge);
                invoice.setTotalAmount(value(invoice.getTotalAmount()) + surcharge);
                invoice.setIsPaid(false);
                invoice.setInvoiceStatus("Pending");
            }
            String damageNote = payload == null ? null : InputValidator.optionalText(
                    string(payload.get("damageNote")), "Ghi chú hư hỏng", 500);
            if (damageNote != null && !damageNote.isBlank()) {
                invoice.setNote(append(invoice.getNote(), "[HƯ HỎNG] " + damageNote));
            }
            invoiceRepository.save(invoice);
            if (!Boolean.TRUE.equals(invoice.getIsPaid())) {
                throw new IllegalStateException("Hóa đơn còn khoản chưa thanh toán: " + value(invoice.getTotalAmount()));
            }

            Date now = new Date();
            for (BookingDetail detail : bookingDetailRepository.findByBookingId(bookingId)) {
                detail.setActualOut(now);
                bookingDetailRepository.save(detail);
                if (detail.getRoomId() != null) roomStatusService.markCleaningAfterCheckout(detail.getRoomId(), claims.userId());
            }
            order.setStatus("Đã trả phòng");
            bookingOrderRepository.save(order);
            invoice.setInvoiceStatus("Completed");
            invoiceRepository.save(invoice);
            return ok("Trả phòng thành công; phòng đã chuyển sang chờ dọn", bookingDto(order));
        });
    }

    @PostMapping("/reception/bookings/{bookingId}/change-room")
    public synchronized ResponseEntity<?> changeRoom(
            @PathVariable Long bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> payload) {
        return roleAction(authorization, Set.of(3L), claims -> {
            BookingOrder order = requiredBooking(bookingId);
            if (!hasStatus(order, CHECKED_IN)) throw new IllegalStateException("Chỉ đổi phòng cho khách đang lưu trú");
            Long newRoomId = longValue(payload.get("roomId"), "Mã phòng mới");
            String reason = InputValidator.requiredText(string(payload.get("reason")), "Lý do đổi phòng", 300);
            Room newRoom = roomRepository.findById(newRoomId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng mới"));
            if (RoomStatus.fromStored(newRoom.getStatus()) != RoomStatus.VACANT) {
                throw new IllegalStateException("Phòng mới không còn trống");
            }
            List<BookingDetail> details = bookingDetailRepository.findByBookingId(bookingId);
            if (details.isEmpty()) throw new IllegalStateException("Không tìm thấy phòng hiện tại");
            BookingDetail detail = details.get(0);
            Room oldRoom = roomRepository.findById(detail.getRoomId())
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy phòng hiện tại"));
            if (oldRoom.getId().equals(newRoomId)) throw new IllegalArgumentException("Phòng mới phải khác phòng hiện tại");

            Date now = new Date();
            Date availabilityEnd = order.getExpectedOut();
            if (availabilityEnd == null || !availabilityEnd.after(now)) {
                availabilityEnd = Date.from(now.toInstant().plus(java.time.Duration.ofDays(1)));
            }
            boolean available = availabilityService.findAvailableRoomsExcludingBooking(
                            null, now, availabilityEnd, bookingId).stream()
                    .anyMatch(room -> Objects.equals(room.getId(), newRoomId));
            if (!available) throw new IllegalStateException("Phòng mới đã được đặt trong thời gian còn lại");

            double difference = value(newRoom.getPrice()) - value(oldRoom.getPrice());
            Invoice invoice = firstInvoice(bookingId)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy hóa đơn"));
            invoice.setTotalAmount(Math.max(0D, value(invoice.getTotalAmount()) + difference));
            invoice.setNote(append(invoice.getNote(), "[ĐỔI PHÒNG] " + oldRoom.getName() + " -> "
                    + newRoom.getName() + "; " + reason + "; chênh lệch " + difference));
            if (difference > 0) {
                invoice.setIsPaid(false);
                invoice.setInvoiceStatus("Pending");
            }
            invoiceRepository.save(invoice);

            detail.setRoomId(newRoomId);
            detail.setUnitPrice(newRoom.getPrice());
            bookingDetailRepository.save(detail);
            oldRoom.setStatus(reason.toLowerCase(Locale.ROOT).contains("hỏng")
                    || reason.toLowerCase(Locale.ROOT).contains("bảo trì")
                    ? RoomStatus.MAINTENANCE.getLabel() : RoomStatus.CLEANING.getLabel());
            oldRoom.setStatusNote(reason);
            oldRoom.setStatusUpdatedAt(new Date());
            oldRoom.setStatusUpdatedBy(claims.userId());
            roomRepository.save(oldRoom);
            newRoom.setStatus(RoomStatus.OCCUPIED.getLabel());
            newRoom.setStatusUpdatedAt(new Date());
            newRoom.setStatusUpdatedBy(claims.userId());
            roomRepository.save(newRoom);
            return ok("Đổi phòng thành công", Map.of(
                    "booking", bookingDto(order),
                    "oldRoom", roomDto(oldRoom),
                    "newRoom", roomDto(newRoom),
                    "priceDifference", difference,
                    "invoiceTotal", value(invoice.getTotalAmount())));
        });
    }

    @GetMapping("/reception/no-show")
    public ResponseEntity<?> noShow(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return roleAction(authorization, Set.of(3L), claims -> ok("Tải cảnh báo no-show thành công",
                bookingOrderRepository.findAll().stream()
                        .filter(this::isPending)
                        .filter(order -> order.getAutoCancelTime() != null)
                        .sorted(Comparator.comparing(BookingOrder::getAutoCancelTime))
                        .map(this::bookingDto)
                        .toList()));
    }

    @PatchMapping("/reception/bookings/{bookingId}/late-arrival")
    public ResponseEntity<?> approveLateArrival(
            @PathVariable Long bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> payload) {
        return roleAction(authorization, Set.of(3L), claims -> {
            BookingOrder order = requiredBooking(bookingId);
            if (!isPending(order)) throw new IllegalStateException("Phiếu không còn chờ nhận phòng");
            order.setLateArrivalApproved(true);
            order.setLateArrivalReason(InputValidator.requiredText(
                    string(payload.get("reason")), "Lý do nhận phòng trễ", 300));
            order.setLateArrivalAt(new Date());
            order.setLateArrivalApprovedBy(claims.userId());
            order.setAutoCancelTime(null);
            return ok("Đã ghi nhận khách nhận phòng trễ", bookingOrderRepository.save(order));
        });
    }

    @GetMapping("/reports/revenue")
    public ResponseEntity<?> revenueReport(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return roleAction(authorization, Set.of(2L, 7L), claims ->
                ok("Tải báo cáo doanh thu thành công", revenueData(date(from, true), date(to, false))));
    }

    @GetMapping(value = "/reports/revenue.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<?> revenueCsv(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return roleAction(authorization, Set.of(2L, 7L), claims -> {
            Date start = date(from, true);
            Date end = date(to, false);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) revenueData(start, end).get("invoices");
            StringBuilder csv = new StringBuilder("\uFEFFinvoice_id,booking_id,date,guest,total,payment_method,status\r\n");
            for (Map<String, Object> row : rows) {
                csv.append(csv(row.get("id"))).append(',')
                        .append(csv(row.get("bookingId"))).append(',')
                        .append(csv(row.get("createdAt"))).append(',')
                        .append(csv(row.get("guestName"))).append(',')
                        .append(csv(row.get("totalAmount"))).append(',')
                        .append(csv(row.get("payMethod"))).append(',')
                        .append(csv(row.get("status"))).append("\r\n");
            }
            return ResponseEntity.ok()
                    .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                    .header("Content-Disposition", "attachment; filename=may-hotel-revenue.csv")
                    .body(csv.toString());
        });
    }

    private Map<String, Object> revenueData(Date from, Date to) {
        List<Invoice> invoices = invoiceRepository.findAll().stream()
                .filter(invoice -> Boolean.TRUE.equals(invoice.getIsPaid()))
                .filter(invoice -> within(invoice.getCreatedAt(), from, to))
                .sorted(Comparator.comparing(Invoice::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        double revenue = invoices.stream().mapToDouble(invoice -> value(invoice.getTotalAmount())).sum();
        Map<String, Double> byMonth = new LinkedHashMap<>();
        for (Invoice invoice : invoices) {
            LocalDate day = invoice.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            byMonth.merge(String.format("%04d-%02d", day.getYear(), day.getMonthValue()),
                    value(invoice.getTotalAmount()), Double::sum);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from", from);
        data.put("to", to);
        data.put("totalRevenue", revenue);
        data.put("paidInvoiceCount", invoices.size());
        data.put("averageInvoice", invoices.isEmpty() ? 0D : revenue / invoices.size());
        data.put("revenueByMonth", byMonth);
        data.put("invoices", invoices.stream().map(this::invoiceDto).toList());
        return data;
    }

    private Map<String, Object> bookingDto(BookingOrder order) {
        Customer customer = order.getCustomerId() == null ? null
                : customerRepository.findById(order.getCustomerId()).orElse(null);
        List<BookingDetail> details = bookingDetailRepository.findByBookingId(order.getId());
        Room room = details.isEmpty() || details.get(0).getRoomId() == null ? null
                : roomRepository.findById(details.get(0).getRoomId()).orElse(null);
        RoomType roomType = room == null || room.getRoomTypeId() == null ? null
                : roomTypeRepository.findById(room.getRoomTypeId()).orElse(null);
        Invoice invoice = firstInvoice(order.getId()).orElse(null);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bookingId", order.getId());
        data.put("customerName", customer == null ? "" : customer.getFullName());
        data.put("phone", customer == null ? "" : customer.getPhone());
        data.put("nationality", customer == null ? "" : firstText(customer.getNationality(), customer.getQuocTich()));
        data.put("roomId", room == null ? null : room.getId());
        data.put("roomName", room == null ? "Chưa xếp phòng" : room.getName());
        data.put("roomType", roomType == null ? "Chưa phân loại" : roomType.getName());
        data.put("expectedIn", order.getExpectedIn());
        data.put("expectedOut", order.getExpectedOut());
        data.put("status", order.getStatus());
        data.put("guestCount", effectiveGuestCount(order));
        data.put("verifiedGuestCount", effectiveVerifiedGuests(order));
        data.put("identityVerified", effectiveVerifiedGuests(order) >= effectiveGuestCount(order));
        data.put("verificationMethod", order.getIdentityVerificationMethod());
        data.put("riskLevel", order.getNoShowRiskLevel());
        data.put("riskPercentage", order.getNoShowRiskPercentage());
        data.put("autoCancelTime", order.getAutoCancelTime());
        data.put("lateArrivalApproved", Boolean.TRUE.equals(order.getLateArrivalApproved()));
        data.put("invoice", invoice == null ? null : invoiceDto(invoice));
        return data;
    }

    private Map<String, Object> roomDto(Room room) {
        RoomType type = room.getRoomTypeId() == null ? null
                : roomTypeRepository.findById(room.getRoomTypeId()).orElse(null);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", room.getId());
        data.put("name", room.getName());
        data.put("roomTypeId", room.getRoomTypeId());
        data.put("roomType", type == null ? "" : type.getName());
        data.put("price", value(room.getPrice()));
        data.put("status", RoomStatus.fromStored(room.getStatus()).name());
        data.put("statusLabel", RoomStatus.fromStored(room.getStatus()).getLabel());
        data.put("viewDirection", room.getViewDirection());
        return data;
    }

    private Map<String, Object> invoiceDto(Invoice invoice) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", invoice.getId());
        data.put("bookingId", invoice.getBookingId());
        data.put("guestName", invoice.getGuestName());
        data.put("phone", invoice.getPhone());
        data.put("createdAt", invoice.getCreatedAt());
        data.put("totalAmount", value(invoice.getTotalAmount()));
        data.put("surcharge", value(invoice.getSurcharge()));
        data.put("payMethod", invoice.getPayMethod());
        data.put("status", invoice.getInvoiceStatus());
        data.put("isPaid", Boolean.TRUE.equals(invoice.getIsPaid()));
        data.put("note", invoice.getNote());
        data.put("earnedPoints", invoice.getEarnedPoints());
        data.put("redeemedPoints", invoice.getRedeemedPoints());
        return data;
    }

    private BookingOrder ownedBooking(Long bookingId, Customer customer) {
        BookingOrder order = requiredBooking(bookingId);
        if (!Objects.equals(customer.getId(), order.getCustomerId())) {
            throw new SecurityException("Bạn không có quyền thao tác phiếu đặt phòng này");
        }
        return order;
    }

    private BookingOrder requiredBooking(Long bookingId) {
        return bookingOrderRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt phòng"));
    }

    private Long bookedRoomType(Long bookingId) {
        return bookingDetailRepository.findByBookingId(bookingId).stream()
                .map(BookingDetail::getRoomId)
                .filter(Objects::nonNull)
                .map(roomRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Room::getRoomTypeId)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private Optional<Invoice> firstInvoice(Long bookingId) {
        return invoiceRepository.findByBookingId(bookingId).stream().findFirst();
    }

    private ResponseEntity<?> customerAction(String authorization, CustomerAction action) {
        try {
            MobileTokenService.Claims claims = tokenService.verifyAuthorization(authorization);
            if (claims.roleId() != 5L && !"CUSTOMER".equalsIgnoreCase(claims.role())) {
                return forbidden("Chức năng chỉ dành cho khách hàng");
            }
            Customer customer = customerRepository.findFirstByUserIdOrderByIdAsc(claims.userId())
                    .orElseThrow(() -> new IllegalStateException("Tài khoản chưa có hồ sơ khách hàng"));
            return action.run(claims, customer);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ex.getMessage()));
        }
    }

    private ResponseEntity<?> roleAction(String authorization, Set<Long> roleIds, RoleAction action) {
        try {
            MobileTokenService.Claims claims = tokenService.verifyAuthorization(authorization);
            if (!roleIds.contains(claims.roleId())) return forbidden("Bạn không có quyền sử dụng chức năng này");
            return action.run(claims);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ex.getMessage()));
        }
    }

    private ResponseEntity<?> ok(String message, Object data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    private ResponseEntity<?> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
    }

    private boolean isPending(BookingOrder order) {
        String status = normalized(order.getStatus());
        return status.contains("pending") || status.contains("chờ nhận") || status.contains("xác nhận cọc")
                || status.contains("reserved") || status.contains("đã xác thực");
    }

    private boolean hasStatus(BookingOrder order, Set<String> statuses) {
        return statuses.contains(normalized(order.getStatus()));
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private int effectiveGuestCount(BookingOrder order) {
        return order.getGuestCount() == null || order.getGuestCount() < 1 ? 1 : order.getGuestCount();
    }

    private int effectiveVerifiedGuests(BookingOrder order) {
        if (order.getVerifiedGuestCount() != null) return order.getVerifiedGuestCount();
        return Boolean.TRUE.equals(order.getIsFaceVerified()) ? effectiveGuestCount(order) : 0;
    }

    private boolean isPayAtHotel(Invoice invoice) {
        String method = normalized(invoice.getPayMethod());
        return method.contains("hotel") || method.contains("quầy") || method.contains("cod") || method.contains("cash");
    }

    private Map<String, Object> pointHistory(Invoice invoice, String type, int points) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("invoiceId", invoice.getId());
        value.put("bookingId", invoice.getBookingId());
        value.put("type", type);
        value.put("points", points);
        value.put("occurredAt", invoice.getCreatedAt());
        return value;
    }

    private int nextTierPoints(int points, LoyaltyPolicy policy) {
        if (policy.getSilverThreshold() != null && points < policy.getSilverThreshold()) return policy.getSilverThreshold() - points;
        if (policy.getGoldThreshold() != null && points < policy.getGoldThreshold()) return policy.getGoldThreshold() - points;
        if (policy.getPlatinumThreshold() != null && points < policy.getPlatinumThreshold()) return policy.getPlatinumThreshold() - points;
        return 0;
    }

    private String tierName(Long tierId) {
        if (Long.valueOf(4).equals(tierId)) return "Bạch Kim";
        if (Long.valueOf(3).equals(tierId)) return "Vàng";
        if (Long.valueOf(2).equals(tierId)) return "Bạc";
        return "Đồng";
    }

    private double cancellationPenalty(BookingOrder order) {
        if (order.getExpectedIn() == null) return 0D;
        long remaining = order.getExpectedIn().getTime() - System.currentTimeMillis();
        if (remaining <= 0) return 100D;
        return remaining < 24L * 60 * 60 * 1000 ? 50D : 0D;
    }

    private boolean within(Date value, Date from, Date to) {
        if (value == null) return false;
        return (from == null || !value.before(from)) && (to == null || !value.after(to));
    }

    private Date date(String raw, boolean start) {
        if (raw == null || raw.isBlank()) return null;
        try {
            LocalDate day = LocalDate.parse(raw);
            Instant instant = start ? day.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    : day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
            return Date.from(instant);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Ngày phải có định dạng yyyy-MM-dd");
        }
    }

    private String csv(Object value) {
        String text = string(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private String firstText(String first, String second) {
        return first != null && !first.isBlank() ? first : string(second);
    }

    private String append(String current, String extra) {
        return current == null || current.isBlank() ? extra : current + " | " + extra;
    }

    private String string(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private double value(Double value) {
        return value == null ? 0D : value;
    }

    private double optionalDouble(Object value) {
        if (value == null || value.toString().isBlank()) return 0D;
        return doubleValue(value, "Giá trị");
    }

    private double doubleValue(Object value, String label) {
        try {
            double parsed = Double.parseDouble(string(value));
            if (!Double.isFinite(parsed)) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " không hợp lệ");
        }
    }

    private int intValue(Object value, String label) {
        try {
            return Integer.parseInt(string(value));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " không hợp lệ");
        }
    }

    private Integer asInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Long longValue(Object value, String label) {
        try {
            return Long.valueOf(string(value));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " không hợp lệ");
        }
    }

    @FunctionalInterface
    private interface CustomerAction {
        ResponseEntity<?> run(MobileTokenService.Claims claims, Customer customer) throws Exception;
    }

    @FunctionalInterface
    private interface RoleAction {
        ResponseEntity<?> run(MobileTokenService.Claims claims) throws Exception;
    }
}
