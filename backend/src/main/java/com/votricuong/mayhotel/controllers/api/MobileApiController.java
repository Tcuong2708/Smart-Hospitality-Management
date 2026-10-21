package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.Service;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.dto.ApiResponse;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.ServiceRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.votricuong.mayhotel.repositories.UserRepository;
import com.votricuong.mayhotel.documents.OtpCode;
import com.votricuong.mayhotel.repositories.OtpCodeRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.ServiceTicketRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.services.SequenceGeneratorService;
import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.ServiceTicket;
import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.services.EmailService;
import com.votricuong.mayhotel.services.BookingAvailabilityService;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/mobile")
@CrossOrigin(origins = "*") // Allow Flutter app to call APIs
public class MobileApiController {

    private final RoomRepository roomRepository;
    private final ServiceRepository serviceRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final OtpCodeRepository otpCodeRepository;
    private final com.votricuong.mayhotel.repositories.RoomTypeRepository roomTypeRepository;
    private final BookingOrderRepository bookingOrderRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final ServiceTicketRepository serviceTicketRepository;
    private final CustomerRepository customerRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final com.votricuong.mayhotel.services.LoyaltyService loyaltyService;
    private final BookingAvailabilityService bookingAvailabilityService;
    private final com.votricuong.mayhotel.services.NoShowPredictionService noShowPredictionService;
    private final com.votricuong.mayhotel.services.MobileTokenService mobileTokenService;
    private final com.votricuong.mayhotel.services.RoomStatusService roomStatusService;

    public MobileApiController(RoomRepository roomRepository, ServiceRepository serviceRepository, InvoiceRepository invoiceRepository, UserRepository userRepository, EmailService emailService, OtpCodeRepository otpCodeRepository, com.votricuong.mayhotel.repositories.RoomTypeRepository roomTypeRepository, BookingOrderRepository bookingOrderRepository, BookingDetailRepository bookingDetailRepository, ServiceTicketRepository serviceTicketRepository, CustomerRepository customerRepository, SequenceGeneratorService sequenceGeneratorService, com.votricuong.mayhotel.services.LoyaltyService loyaltyService, BookingAvailabilityService bookingAvailabilityService, com.votricuong.mayhotel.services.NoShowPredictionService noShowPredictionService, com.votricuong.mayhotel.services.MobileTokenService mobileTokenService, com.votricuong.mayhotel.services.RoomStatusService roomStatusService) {
        this.roomRepository = roomRepository;
        this.serviceRepository = serviceRepository;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.otpCodeRepository = otpCodeRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.serviceTicketRepository = serviceTicketRepository;
        this.customerRepository = customerRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.loyaltyService = loyaltyService;
        this.bookingAvailabilityService = bookingAvailabilityService;
        this.noShowPredictionService = noShowPredictionService;
        this.mobileTokenService = mobileTokenService;
        this.roomStatusService = roomStatusService;
    }

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return password;
        }
    }

    // ==========================================
    // 1. AUTHENTICATION (Basic dummy for sync)
    // ==========================================
    
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("TenDangNhap");
        String password = credentials.get("MatKhau");
        
        if (username != null && password != null) {
            Optional<User> userOpt = userRepository.findFirstByEmail(username);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findFirstByUsername(username);
            }
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findFirstByPhone(username);
            }
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String dbPassword = user.getPassword();
                String hashedInput = hashPassword(password);
                
                if (dbPassword != null && (dbPassword.equals(password) || dbPassword.equals(hashedInput))) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", user.getId());
                    data.put("name", user.getUsername() != null ? user.getUsername() : username);
                    
                    long databaseRoleId = user.getRoleId() != null ? user.getRoleId() : 5L;
                    data.put("roleId", databaseRoleId);
                    data.put("roleCode", user.getRole());
                    // Keep the legacy field so installed app versions continue to work.
                    int legacyRole = databaseRoleId == 1L ? 1 : databaseRoleId == 3L ? 2 : 3;
                    data.put("role", legacyRole);
                    data.put("accessToken", mobileTokenService.issue(user));
                    data.put("expiresIn", com.votricuong.mayhotel.services.MobileTokenService.EXPIRES_IN_SECONDS);
                    
                    return ResponseEntity.ok(data);
                }
            }
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Tên đăng nhập hoặc mật khẩu không chính xác!"));
    }

    @PostMapping("/auth/google-login")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody Map<String, String> payload) {
        String idToken = payload.get("IdToken");
        // Simplified Google login (without full Firebase validation for now, relying on email from Flutter)
        // Usually, the app sends the email as well for quick mock integration, or we mock success.
        Map<String, Object> data = new HashMap<>();
        data.put("id", System.currentTimeMillis());
        data.put("name", "Google User");
        data.put("role", 3);
        data.put("roleId", 5);
        data.put("roleCode", "CUSTOMER");
        return ResponseEntity.ok(data);
    }

    @PostMapping("/auth/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody Map<String, String> payload) {
        String email = payload.get("Email");
        if (email != null) {
            String otp = String.format("%06d", new Random().nextInt(999999));
            OtpCode otpCode = new OtpCode();
            otpCode.setIdentifier(email);
            otpCode.setCode(otp);
            otpCode.setCreatedAt(new Date());
            otpCodeRepository.save(otpCode);
            emailService.sendOtpEmail(email, otp);
            return ResponseEntity.ok(Map.of("message", "Đã gửi OTP"));
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/auth/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> payload) {
        String email = payload.get("Email");
        String code = payload.get("OTPCode");
        Optional<OtpCode> otpOpt = otpCodeRepository.findByIdentifierAndCode(email, code);
        // OTP in OtpCode has a TTL index based on createdAt (expires after 5 mins)
        if (otpOpt.isPresent()) {
            return ResponseEntity.ok(Map.of("message", "OTP hợp lệ"));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "OTP sai hoặc hết hạn"));
    }

    // ==========================================
    // 2. ROOMS & SERVICES
    // ==========================================
    
    @GetMapping("/room-types")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRoomTypes() {
        try {
            List<Room> allRooms = roomRepository.findAll();
            List<RoomType> allRoomTypes = roomTypeRepository.findAll();

            List<Map<String, Object>> roomTypesData = allRoomTypes.stream().map(rt -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rt.getId());
                map.put("name", rt.getName());
                map.put("maxOccupancy", rt.getMaxOccupancy() != null ? rt.getMaxOccupancy() : 1);
                
                // Tìm 1 phòng mẫu để lấy thông tin giá, mô tả
                Room sample = allRooms.stream()
                        .filter(r -> rt.getId().equals(r.getRoomTypeId()))
                        .findFirst().orElse(null);
                        
                if (sample != null) {
                    map.put("price", sample.getPrice());
                    map.put("detail", sample.getDetail() != null ? sample.getDetail() : "");
                } else {
                    map.put("price", 500000);
                    map.put("detail", "Phòng sang trọng May Hotel");
                }
                map.put("imageUrl", rt.getImageUrl() != null ? rt.getImageUrl() : "");
                
                long soPhongTrong = allRooms.stream()
                    .filter(r -> rt.getId().equals(r.getRoomTypeId()) && "Vacant".equalsIgnoreCase(r.getStatus()))
                    .count();
                map.put("maTrangThai", soPhongTrong > 0 ? 1 : 2); // 1 = Available, 2 = Unavailable
                map.put("availableCount", soPhongTrong);
                
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin loại phòng thành công", roomTypesData));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRooms() {
        try {
            List<Room> allRooms = roomRepository.findAll();
            List<RoomType> allRoomTypes = roomTypeRepository.findAll();
            
            Map<Long, RoomType> typeMap = allRoomTypes.stream().collect(Collectors.toMap(RoomType::getId, t -> t));

            List<Map<String, Object>> roomsData = allRooms.stream().map(r -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", r.getId());
                map.put("name", r.getName());
                map.put("price", r.getPrice());
                map.put("detail", r.getDetail() != null ? r.getDetail() : "");
                
                String img = r.getImageUrl();
                if (img == null || img.isEmpty()) {
                    RoomType rt = typeMap.get(r.getRoomTypeId());
                    if (rt != null && rt.getImageUrl() != null) {
                        img = rt.getImageUrl();
                    } else {
                        img = "";
                    }
                }
                map.put("imageUrl", img);
                com.votricuong.mayhotel.enums.RoomStatus roomStatus =
                        com.votricuong.mayhotel.enums.RoomStatus.fromStored(r.getStatus());
                map.put("maTrangThai", roomStatus.getMapCode());
                map.put("status", roomStatus.name());
                map.put("statusLabel", roomStatus.getLabel());
                
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin phòng thành công", roomsData));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/rooms/search")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchRooms(
            @RequestParam("ngayNhan") String ngayNhanStr,
            @RequestParam("ngayTra") String ngayTraStr,
            @RequestParam(value = "adults", required = false, defaultValue = "2") Integer adults,
            @RequestParam(value = "children", required = false, defaultValue = "0") Integer children) {
        try {
            Date ngayNhan = parseMobileDate(ngayNhanStr);
            Date ngayTra = parseMobileDate(ngayTraStr);
            int adultCount = adults == null ? 2 : adults;
            int childCount = children == null ? 0 : children;
            if (adultCount < 1 || childCount < 0) {
                throw new IllegalArgumentException("Số lượng người lớn hoặc trẻ em không hợp lệ");
            }
            int totalGuests = Math.addExact(adultCount, childCount);
            
            if (ngayTra.before(ngayNhan) || ngayTra.equals(ngayNhan)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Ngày trả phòng phải sau ngày nhận phòng"));
            }

            List<Room> allRooms = bookingAvailabilityService.findAvailableRooms(null, ngayNhan, ngayTra);
            List<RoomType> allRoomTypes = roomTypeRepository.findAll();
            Map<Long, RoomType> typeMap = allRoomTypes.stream().collect(Collectors.toMap(RoomType::getId, t -> t));

            // Gom nhóm các phòng còn trống theo đúng khoảng ngày (ẩn tên phòng cụ thể).
            Map<Long, List<Room>> availableRoomsByType = allRooms.stream()
                .filter(r -> r.getRoomTypeId() != null)
                .collect(Collectors.groupingBy(Room::getRoomTypeId));

            List<Map<String, Object>> roomsData = availableRoomsByType.entrySet().stream()
                .map(entry -> {
                    Long roomTypeId = entry.getKey();
                    List<Room> roomsOfType = entry.getValue();
                    RoomType rt = typeMap.get(roomTypeId);
                    
                    if (rt == null || roomsOfType.isEmpty()) return null;
                    
                    // Lọc theo số lượng người lớn
                    Integer maxOcc = rt.getMaxOccupancy() != null ? rt.getMaxOccupancy() : 2;
                    if (maxOcc < totalGuests) return null;
                    
                    Room firstRoom = roomsOfType.get(0);
                    
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rt.getId()); // Trả về ID của loại phòng
                    map.put("name", rt.getName());
                    map.put("maxOccupancy", maxOcc);
                    map.put("price", firstRoom.getPrice()); // Lấy giá từ phòng đầu tiên
                    
                    // Detail: Hiển thị thông tin tổng quan của loại phòng
                    String detail = firstRoom.getDetail();
                    if (detail == null || detail.isEmpty()) {
                        detail = "Phòng tiêu chuẩn dành cho " + maxOcc + " người.";
                    }
                    map.put("detail", detail);
                    
                    String img = rt.getImageUrl();
                    if (img == null || img.isEmpty()) {
                        img = firstRoom.getImageUrl() != null ? firstRoom.getImageUrl() : "";
                    }
                    map.put("imageUrl", img);
                    map.put("maTrangThai", 1);
                    map.put("availableCount", roomsOfType.size()); // Số lượng phòng trống
                    
                    return map;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Tìm thấy " + roomsData.size() + " phòng trống", roomsData));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<Service>>> getServices() {
        return ResponseEntity.ok(ApiResponse.success("Thành công", serviceRepository.findAll()));
    }

    // ==========================================
    // 3. BOOKING (Phần 2.2, 2.3, 2.4, 2.5)
    // ==========================================

    @PostMapping("/booking/add")
    public synchronized ResponseEntity<ApiResponse<Map<String, Object>>> addBooking(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = requiredLong(payload, "userId");
            Long roomTypeId = requiredLong(payload, "roomTypeId");
            String ngayNhanStr = requiredString(payload, "ngayNhan");
            String ngayTraStr = requiredString(payload, "ngayTra");
            int guestCount = optionalPositiveInt(payload, "soNguoi", 1);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Tài khoản đặt phòng không tồn tại"));
            Date ngayNhan = parseMobileDate(ngayNhanStr);
            Date ngayTra = parseMobileDate(ngayTraStr);
            java.time.LocalDate checkInDate = ngayNhan.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
            java.time.LocalDate checkOutDate = ngayTra.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
            com.votricuong.mayhotel.utils.InputValidator.stay(checkInDate, checkOutDate);
            bookingAvailabilityService.validateStay(ngayNhan, ngayTra);

            List<Room> availableRooms = bookingAvailabilityService.findAvailableRooms(roomTypeId, ngayNhan, ngayTra);
                
            if (availableRooms.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Hết phòng trống cho loại phòng này trong khoảng ngày đã chọn."));
            }
            
            Room selectedRoom = availableRooms.get(0);
            RoomType selectedRoomType = roomTypeRepository.findById(roomTypeId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại phòng"));
            int maxOccupancy = selectedRoomType.getMaxOccupancy() == null
                    ? 1
                    : selectedRoomType.getMaxOccupancy();
            if (guestCount > maxOccupancy) {
                return ResponseEntity.badRequest().body(ApiResponse.error(
                        "Số khách vượt quá sức chứa tối đa " + maxOccupancy + " người của loại phòng."));
            }
            
            // Xử lý Customer
            String customerName = optionalString(payload, "customerName");
            String customerPhone = optionalString(payload, "customerPhone");
            String ghiChu = optionalString(payload, "ghiChu");
            String resolvedName = hasText(customerName)
                    ? customerName
                    : (hasText(user.getFullName()) ? user.getFullName() : user.getUsername());
            String resolvedPhone = hasText(customerPhone) ? customerPhone : user.getPhone();
            resolvedName = com.votricuong.mayhotel.utils.InputValidator.requiredText(resolvedName, "Họ và tên", 100);
            resolvedPhone = com.votricuong.mayhotel.utils.InputValidator.phone(resolvedPhone);
            ghiChu = com.votricuong.mayhotel.utils.InputValidator.optionalText(ghiChu, "Ghi chú", 500);

            List<Customer> customers = customerRepository.findByUserId(userId);
            Customer customer;
            if (customers.isEmpty()) {
                customer = new Customer();
                customer.setId(sequenceGeneratorService.generateSequence("customers_sequence"));
                customer.setUserId(userId);
                
                customer.setFullName(resolvedName);
                customer.setPhone(resolvedPhone);
                customer.setEmail(user.getEmail());
                customerRepository.save(customer);
            } else {
                customer = customers.get(0);
                customer.setFullName(resolvedName);
                customer.setPhone(resolvedPhone);
                if (!hasText(customer.getEmail())) customer.setEmail(user.getEmail());
                customerRepository.save(customer);
            }

            long nights = java.time.temporal.ChronoUnit.DAYS.between(
                    ngayNhan.toInstant().atZone(ZoneOffset.UTC).toLocalDate(),
                    ngayTra.toInstant().atZone(ZoneOffset.UTC).toLocalDate());
            double unitPrice = selectedRoom.getPrice() != null ? selectedRoom.getPrice() : 0.0;
            double total = unitPrice * nights;
            
            BookingOrder order = new BookingOrder();
            order.setId(sequenceGeneratorService.generateSequence("booking_orders_sequence"));
            order.setCustomerId(customer.getId());
            order.setBookingDate(new Date());
            order.setExpectedIn(ngayNhan);
            order.setExpectedOut(ngayTra);
            order.setStatus("Pending");
            order.setBookingSource("MOBILE");
            order.setRiskAnalysisStatus("PENDING");
            order.setNotes(ghiChu);
            order.setGuestCount(guestCount);
            order.setVerifiedGuestCount(0);
            order.setGuestVerifications(new ArrayList<>());
            order.setIsFaceVerified(false);
            order.setAutoCancelTime(calculateAutoCancelTime(ngayNhan, false));
            bookingOrderRepository.save(order);
            
            BookingDetail bd = new BookingDetail();
            bd.setId(sequenceGeneratorService.generateSequence("booking_details_sequence"));
            bd.setBookingId(order.getId());
            bd.setRoomId(selectedRoom.getId());
            bd.setUnitPrice(selectedRoom.getPrice());
            bookingDetailRepository.save(bd);
            
            Invoice invoice = new Invoice();
            invoice.setId(sequenceGeneratorService.generateSequence("invoices_sequence"));
            invoice.setBookingId(order.getId());
            invoice.setTotalAmount(total);
            invoice.setInvoiceStatus("Pending");
            invoice.setCreatedAt(new Date());
            invoice.setUserId(userId);
            invoice.setGuestName(customer.getFullName());
            invoice.setPhone(customer.getPhone());
            invoice.setRoomId(selectedRoom.getId());
            invoice.setCheckInDate(ngayNhan);
            invoice.setCheckOutDate(ngayTra);
            invoice.setIsPaid(false);
            invoice.setSurcharge(0.0);
            invoice.setNote(ghiChu);
            invoiceRepository.save(invoice);
            noShowPredictionService.analyzeAndSave(order.getId());
            
            Map<String, Object> resp = new HashMap<>();
            resp.put("bookingId", order.getId());
            resp.put("invoiceId", invoice.getId());
            resp.put("roomId", selectedRoom.getId());
            resp.put("total", total);
            resp.put("status", order.getStatus());
            
            return ResponseEntity.ok(ApiResponse.success("Đặt phòng thành công", resp));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/booking/history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHistory(@RequestParam("userId") Long userId) {
        try {
            List<Customer> customers = customerRepository.findByUserId(userId);
            if (customers.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("Trống", new ArrayList<>()));
            }
            Long customerId = customers.get(0).getId();
            
            List<BookingOrder> orders = bookingOrderRepository.findAll().stream()
                .filter(o -> customerId.equals(o.getCustomerId()))
                .sorted(Comparator.comparing(
                        BookingOrder::getBookingDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
                
            List<Map<String, Object>> data = new ArrayList<>();
            for (BookingOrder o : orders) {
                double tongTien = 0.0;
                Optional<Invoice> invOpt = invoiceRepository.findByBookingId(o.getId()).stream().findFirst();
                if (invOpt.isPresent() && invOpt.get().getTotalAmount() != null) {
                    tongTien = invOpt.get().getTotalAmount();
                }
                
                Map<String, Object> map = new HashMap<>();
                map.put("bookingId", o.getId());
                map.put("ngayNhan", o.getExpectedIn() != null ? o.getExpectedIn().toInstant().toString() : null);
                map.put("ngayTra", o.getExpectedOut() != null ? o.getExpectedOut().toInstant().toString() : null);
                map.put("tongTien", tongTien);
                map.put("trangThai", o.getStatus() != null ? o.getStatus() : "Chờ xác nhận");
                map.put("isFaceVerified", Boolean.TRUE.equals(o.getIsFaceVerified()));
                map.put("guestCount", effectiveGuestCount(o));
                map.put("verifiedGuestCount", effectiveVerifiedGuestCount(o));
                map.put("daThanhToan", invOpt.map(inv -> Boolean.TRUE.equals(inv.getIsPaid())).orElse(false));
                map.put("phuongThucThanhToan", invOpt.map(Invoice::getPayMethod).orElse(null));
                map.put("tenPhong", "Chưa gán phòng");
                map.put("roomId", null);
                map.put("hinhAnh", "");
                
                // Tìm chi tiết để lấy thông tin phòng
                List<BookingDetail> details = bookingDetailRepository.findByBookingId(o.getId());
                if (!details.isEmpty() && details.get(0).getRoomId() != null) {
                    roomRepository.findById(details.get(0).getRoomId()).ifPresent(r -> {
                        map.put("roomId", r.getId());
                        map.put("tenPhong", r.getName());
                        map.put("hinhAnh", r.getImageUrl() != null ? r.getImageUrl() : "");
                    });
                }
                
                data.add(map);
            }
            return ResponseEntity.ok(ApiResponse.success("Thành công", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/booking/active")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveBooking(@RequestParam("userId") Long userId) {
        try {
            List<Customer> customers = customerRepository.findByUserId(userId);
            if (customers.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("Không có phòng đang ở", Map.of("hasActiveBooking", false, "canOrderService", false)));
            }
            Long customerId = customers.get(0).getId();
            
            Optional<BookingOrder> activeOpt = bookingOrderRepository.findAll().stream()
                .filter(o -> customerId.equals(o.getCustomerId()))
                .filter(o -> isCheckedInStatus(o.getStatus()))
                .findFirst();
                
            if (activeOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("Hiện không có phòng nào đang ở", Map.of("hasActiveBooking", false, "canOrderService", false)));
            }

            BookingOrder activeOrder = activeOpt.get();
            Map<String, Object> data = new HashMap<>();
            data.put("hasActiveBooking", true);
            data.put("canOrderService", true);
            data.put("bookingId", activeOrder.getId());
            data.put("status", activeOrder.getStatus());
            
            List<BookingDetail> details = bookingDetailRepository.findByBookingId(activeOrder.getId());
            if (!details.isEmpty() && details.get(0).getRoomId() != null) {
                roomRepository.findById(details.get(0).getRoomId()).ifPresent(r -> {
                    data.put("roomId", r.getId());
                    data.put("roomName", r.getName());
                });
            }

            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin nhận phòng thành công", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/booking/checkin")
    public synchronized ResponseEntity<ApiResponse<Map<String, Object>>> checkinAIAndNFC(
            @RequestBody Map<String, Object> payload) {
        try {
            Long bookingId = Long.valueOf(payload.get("bookingId").toString());
            String cccdData = (String) payload.getOrDefault("cccdData", "AI_VERIFIED"); 
            int guestIndex = optionalNonNegativeInt(payload, "guestIndex", 0);
            
            Optional<BookingOrder> orderOpt = bookingOrderRepository.findById(bookingId);
            if (orderOpt.isEmpty()) return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy đơn đặt phòng"));
            
            BookingOrder order = orderOpt.get();
            int requiredGuestCount = effectiveGuestCount(order);
            if (guestIndex >= requiredGuestCount) {
                return ResponseEntity.badRequest().body(ApiResponse.error(
                        "Vị trí khách xác thực không hợp lệ. Booking có " + requiredGuestCount + " khách."));
            }

            List<Map<String, Object>> verifications = order.getGuestVerifications() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(order.getGuestVerifications());

            boolean duplicateIdentity = verifications.stream().anyMatch(item ->
                    !Objects.equals(asInteger(item.get("guestIndex")), guestIndex)
                            && Objects.equals(String.valueOf(item.get("cccdData")), cccdData));
            if (duplicateIdentity) {
                return ResponseEntity.badRequest().body(ApiResponse.error(
                        "Giấy tờ này đã được dùng để xác thực cho một khách khác."));
            }

            Map<String, Object> verification = new HashMap<>();
            verification.put("guestIndex", guestIndex);
            verification.put("cccdData", cccdData);
            verification.put("method", "MOBILE_AI_NFC");
            verification.put("verifiedAt", new Date());
            verifications.removeIf(item -> Objects.equals(asInteger(item.get("guestIndex")), guestIndex));
            verifications.add(verification);

            int verifiedGuestCount = (int) verifications.stream()
                    .map(item -> asInteger(item.get("guestIndex")))
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
            boolean allGuestsVerified = verifiedGuestCount >= requiredGuestCount;

            order.setGuestCount(requiredGuestCount);
            order.setGuestVerifications(verifications);
            order.setVerifiedGuestCount(verifiedGuestCount);
            order.setIsFaceVerified(allGuestsVerified);
            order.setIdentityVerificationMethod(allGuestsVerified ? "MOBILE_AI_NFC" : "MOBILE_AI_NFC_PARTIAL");
            order.setIdentityVerificationReason(null);
            order.setIdentityVerifiedAt(new Date());
            bookingOrderRepository.save(order);
            
            List<BookingDetail> details = bookingDetailRepository.findByBookingId(order.getId());
            Long roomId = null;
            if (!details.isEmpty() && details.get(0).getRoomId() != null) {
                roomId = details.get(0).getRoomId();
            }
            
            String nfcCode = allGuestsVerified
                    ? "MAYHOTEL-ROOM-" + (roomId != null ? roomId : "UNKNOWN") + "-" + UUID.randomUUID().toString().substring(0, 8)
                    : null;
            
            Map<String, Object> resp = new HashMap<>();
            resp.put("bookingId", bookingId);
            resp.put("status", allGuestsVerified
                    ? "Đã xác thực đủ khách - chờ lễ tân xác nhận nhận phòng"
                    : "Đã xác thực " + verifiedGuestCount + "/" + requiredGuestCount + " khách");
            resp.put("verificationMethod", order.getIdentityVerificationMethod());
            resp.put("verifiedAt", order.getIdentityVerifiedAt());
            resp.put("verifiedGuestCount", verifiedGuestCount);
            resp.put("requiredGuestCount", requiredGuestCount);
            resp.put("allGuestsVerified", allGuestsVerified);
            resp.put("canOrderService", false);
            if (nfcCode != null) resp.put("nfcAccessCode", nfcCode);
            resp.put("cccdParsed", cccdData);
            
            return ResponseEntity.ok(ApiResponse.success("Xác thực thành công. Vui lòng chờ lễ tân hoàn tất nhận phòng.", resp));
        } catch(Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/booking/checkout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkout(@RequestBody Map<String, Object> payload) {
        try {
            Long bookingId = Long.valueOf(payload.get("bookingId").toString());
            Optional<BookingOrder> orderOpt = bookingOrderRepository.findById(bookingId);
            if (orderOpt.isEmpty()) return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy đơn đặt phòng"));
            
            BookingOrder order = orderOpt.get();
            order.setStatus("Đã trả phòng");
            bookingOrderRepository.save(order);
            
            List<BookingDetail> details = bookingDetailRepository.findByBookingId(order.getId());
            Date checkedOutAt = new Date();
            for (BookingDetail detail : details) {
                detail.setActualOut(checkedOutAt);
                bookingDetailRepository.save(detail);
            }
            details.stream()
                    .map(BookingDetail::getRoomId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .forEach(roomId -> roomStatusService.markCleaningAfterCheckout(roomId, null));
            
            // Xử lý Invoice thành Completed
            invoiceRepository.findByBookingId(bookingId).stream()
                .findFirst()
                .ifPresent(inv -> {
                    inv.setInvoiceStatus("Completed");
                    invoiceRepository.save(inv);
                });
            
            return ResponseEntity.ok(ApiResponse.success("Trả phòng thành công. Quyền đặt dịch vụ đã được khóa.", Map.of("bookingId", bookingId, "canOrderService", false)));
        } catch(Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // ==========================================
    // 4. SERVICES (Phần 2.6)
    // ==========================================
    
    @PostMapping("/services/order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> orderServices(@RequestBody Map<String, Object> payload) {
        try {
            Long bookingId = Long.valueOf(payload.get("bookingId").toString());
            Long serviceId = Long.valueOf(payload.get("serviceId").toString());
            Object quantityValue = payload.getOrDefault("quantity", 1);
            if (!(quantityValue instanceof Number)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Số lượng dịch vụ không hợp lệ."));
            }
            int quantity = ((Number) quantityValue).intValue();
            if (quantity <= 0) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Số lượng dịch vụ phải lớn hơn 0."));
            }
            
            Optional<BookingOrder> orderOpt = bookingOrderRepository.findById(bookingId);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy thông tin đơn đặt phòng."));
            }

            BookingOrder order = orderOpt.get();
            String st = order.getStatus();

            // Kiểm tra chỉ cho phép gọi dịch vụ khi đã Check-In và chưa Check-Out
            boolean isCheckedIn = isCheckedInStatus(st);
            if (!isCheckedIn) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Quý khách chưa Check-in hoặc đã Trả phòng. Dịch vụ tạm thời bị khóa!"));
            }
            
            Optional<Service> srvOpt = serviceRepository.findById(serviceId);
            if (srvOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Dịch vụ không tồn tại."));
            }
            
            Service srv = srvOpt.get();
            if (srv.getPrice() == null || srv.getPrice() < 0) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Giá dịch vụ không hợp lệ."));
            }
            
            ServiceTicket stItem = new ServiceTicket();
            stItem.setId(sequenceGeneratorService.generateSequence("service_tickets_sequence"));
            stItem.setBookingId(bookingId);
            stItem.setServiceId(serviceId);
            stItem.setQuantity(quantity);
            stItem.setStatus("PENDING");
            stItem.setPrice(srv.getPrice());
            stItem.setRequestGroupId(UUID.randomUUID().toString());
            stItem.setOrderedAt(new Date());
            serviceTicketRepository.save(stItem);

            // Cập nhật tổng tiền vào hóa đơn hiện tại (nếu có)
            double serviceAmount = srv.getPrice() * quantity;
            double updatedInvoiceTotal = serviceAmount;
            Optional<Invoice> invoiceOpt = invoiceRepository.findByBookingId(bookingId).stream().findFirst();
            if (invoiceOpt.isPresent()) {
                Invoice invoice = invoiceOpt.get();
                double currentTotal = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : 0.0;
                updatedInvoiceTotal = currentTotal + serviceAmount;
                invoice.setTotalAmount(updatedInvoiceTotal);
                invoiceRepository.save(invoice);
            }
            
            // Generate Receipt Data cho Mobile render
            Map<String, Object> receipt = new HashMap<>();
            receipt.put("title", "HÓA ĐƠN DỊCH VỤ");
            receipt.put("hotelName", "MAY HOTEL");
            receipt.put("address", "Tây Thạnh, TP.HCM");
            receipt.put("hotline", "0123456789");
            receipt.put("serviceName", srv.getName());
            receipt.put("quantity", quantity);
            receipt.put("unitPrice", srv.getPrice());
            receipt.put("totalPrice", serviceAmount);
            receipt.put("totalAmount", updatedInvoiceTotal);
            receipt.put("requestGroupId", stItem.getRequestGroupId());
            receipt.put("message", "Cảm ơn quý khách đã sử dụng dịch vụ!");
            
            return ResponseEntity.ok(ApiResponse.success("Đặt dịch vụ thành công", receipt));
        } catch(Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/services/order-batch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> orderServiceBatch(
            @RequestBody Map<String, Object> payload) {
        try {
            Long bookingId = requiredLong(payload, "bookingId");
            Object rawItems = payload.get("items");
            if (!(rawItems instanceof List<?> items) || items.isEmpty()) {
                throw new IllegalArgumentException("Danh sách dịch vụ không được để trống");
            }

            LinkedHashMap<Long, Integer> requestedQuantities = new LinkedHashMap<>();
            for (Object rawItem : items) {
                if (!(rawItem instanceof Map<?, ?> itemMap)) {
                    throw new IllegalArgumentException("Dữ liệu dịch vụ không hợp lệ");
                }
                Object rawServiceId = itemMap.get("serviceId");
                Object rawQuantity = itemMap.get("quantity");
                if (rawServiceId == null || rawQuantity == null) {
                    throw new IllegalArgumentException("Mỗi dịch vụ phải có serviceId và quantity");
                }

                Long serviceId;
                int quantity;
                try {
                    serviceId = Long.valueOf(rawServiceId.toString());
                    quantity = Integer.parseInt(rawQuantity.toString());
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Mã hoặc số lượng dịch vụ không hợp lệ");
                }
                if (serviceId <= 0 || quantity <= 0) {
                    throw new IllegalArgumentException("Mã và số lượng dịch vụ phải lớn hơn 0");
                }
                requestedQuantities.merge(serviceId, quantity, Math::addExact);
            }

            BookingOrder order = bookingOrderRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin đơn đặt phòng"));
            String bookingStatus = order.getStatus();
            boolean isCheckedIn = isCheckedInStatus(bookingStatus);
            if (!isCheckedIn) {
                throw new IllegalArgumentException(
                        "Quý khách chưa Check-in hoặc đã trả phòng. Dịch vụ tạm thời bị khóa");
            }

            List<Invoice> invoices = invoiceRepository.findByBookingId(bookingId);
            if (invoices.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy hóa đơn của đơn đặt phòng");
            }
            Invoice invoice = invoices.get(0);

            List<ServiceTicket> tickets = new ArrayList<>();
            List<Map<String, Object>> receiptItems = new ArrayList<>();
            double serviceTotal = 0.0;
            String requestGroupId = UUID.randomUUID().toString();
            Date orderedAt = new Date();

            for (Map.Entry<Long, Integer> requested : requestedQuantities.entrySet()) {
                Service service = serviceRepository.findById(requested.getKey())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Dịch vụ #" + requested.getKey() + " không tồn tại"));
                if (service.getPrice() == null || service.getPrice() < 0) {
                    throw new IllegalArgumentException(
                            "Giá của dịch vụ " + service.getName() + " không hợp lệ");
                }

                int quantity = requested.getValue();
                double lineTotal = service.getPrice() * quantity;
                serviceTotal += lineTotal;

                tickets.add(ServiceTicket.builder()
                        .id(sequenceGeneratorService.generateSequence("service_tickets_sequence"))
                        .bookingId(bookingId)
                        .serviceId(service.getId())
                        .quantity(quantity)
                        .price(service.getPrice())
                        .requestGroupId(requestGroupId)
                        .orderedAt(orderedAt)
                        .status("PENDING")
                        .build());

                Map<String, Object> receiptItem = new LinkedHashMap<>();
                receiptItem.put("serviceId", service.getId());
                receiptItem.put("serviceName", service.getName());
                receiptItem.put("quantity", quantity);
                receiptItem.put("unitPrice", service.getPrice());
                receiptItem.put("lineTotal", lineTotal);
                receiptItems.add(receiptItem);
            }

            serviceTicketRepository.saveAll(tickets);
            double currentInvoiceTotal = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : 0.0;
            double updatedInvoiceTotal = currentInvoiceTotal + serviceTotal;
            invoice.setTotalAmount(updatedInvoiceTotal);
            invoiceRepository.save(invoice);

            Map<String, Object> receipt = new LinkedHashMap<>();
            receipt.put("title", "HÓA ĐƠN DỊCH VỤ");
            receipt.put("hotelName", "MAY HOTEL");
            receipt.put("address", "Tây Thạnh, TP.HCM");
            receipt.put("hotline", "0123456789");
            receipt.put("bookingId", bookingId);
            receipt.put("invoiceId", invoice.getId());
            receipt.put("requestGroupId", requestGroupId);
            receipt.put("orderedAt", orderedAt);
            receipt.put("items", receiptItems);
            receipt.put("serviceTotal", serviceTotal);
            receipt.put("invoiceTotal", updatedInvoiceTotal);
            receipt.put("totalAmount", updatedInvoiceTotal);
            receipt.put("message", "Cảm ơn quý khách đã sử dụng dịch vụ!");

            return ResponseEntity.ok(ApiResponse.success(
                    "Đặt nhiều dịch vụ thành công trên cùng hóa đơn", receipt));
        } catch (ArithmeticException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Tổng số lượng dịch vụ vượt giới hạn"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping("/booking/confirm-payment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmPayment(@RequestBody Map<String, Object> payload) {
        try {
            Long bookingId = requiredLong(payload, "bookingId");
            Long paymentMethodId = requiredLong(payload, "paymentMethodId");
            Optional<Invoice> invoiceOpt = invoiceRepository.findByBookingId(bookingId).stream().findFirst();
            if (invoiceOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy hóa đơn của đơn đặt phòng"));
            }
            BookingOrder order = bookingOrderRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng"));

            Invoice invoice = invoiceOpt.get();
            boolean payAtHotel = paymentMethodId == 8L;
            invoice.setPayMethod(resolvePaymentMethod(paymentMethodId));
            invoice.setIsPaid(!payAtHotel);
            // Trạng thái Reserved là đầu vào của màn hình nhận phòng; trạng thái thanh toán nằm ở isPaid.
            invoice.setInvoiceStatus("Reserved");
            invoiceRepository.save(invoice);

            order.setStatus(payAtHotel ? "Chờ nhận phòng" : "Đã xác nhận cọc");
            bookingOrderRepository.save(order);

            int earnedPoints = payAtHotel ? 0 : loyaltyService.awardPointsForPaidInvoice(invoice);

            Map<String, Object> result = new HashMap<>();
            result.put("bookingId", bookingId);
            result.put("status", payAtHotel ? "PAY_AT_HOTEL" : "PAID");
            result.put("bookingStatus", order.getStatus());
            result.put("totalAmount", invoice.getTotalAmount() != null ? invoice.getTotalAmount() : 0.0);
            result.put("earnedPoints", earnedPoints);
            String message = payAtHotel
                    ? "Đã ghi nhận thanh toán tại quầy khi nhận phòng"
                    : "Thanh toán thành công";
            return ResponseEntity.ok(ApiResponse.success(message, result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi xác nhận thanh toán: " + e.getMessage()));
        }
    }

    private Long requiredLong(Map<String, Object> payload, String field) {
        Object value = payload.get(field);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(field + " không được để trống");
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(field + " không hợp lệ");
        }
    }

    private boolean isCheckedInStatus(String status) {
        if (status == null) return false;
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return Set.of(
                "đã nhận phòng",
                "occupied",
                "in use",
                "đang ở",
                "đang lưu trú"
        ).contains(normalized);
    }

    @GetMapping("/service-staff/tickets")
    public ResponseEntity<?> getServiceTicketsForStaff(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        ResponseEntity<?> denied = requireServiceStaff(authorization);
        if (denied != null) return denied;
        Map<Long, Service> services = serviceRepository.findAll().stream()
                .collect(Collectors.toMap(Service::getId, service -> service));
        Map<Long, BookingOrder> bookings = bookingOrderRepository.findAll().stream()
                .collect(Collectors.toMap(BookingOrder::getId, order -> order, (first, ignored) -> first));
        Map<Long, Customer> customers = customerRepository.findAll().stream()
                .collect(Collectors.toMap(Customer::getId, customer -> customer, (first, ignored) -> first));
        Map<Long, Invoice> invoices = invoiceRepository.findAll().stream()
                .filter(invoice -> invoice.getBookingId() != null)
                .collect(Collectors.toMap(Invoice::getBookingId, invoice -> invoice, (first, ignored) -> first));
        Map<Long, BookingDetail> bookingDetails = bookingDetailRepository.findAll().stream()
                .filter(detail -> detail.getBookingId() != null)
                .collect(Collectors.toMap(BookingDetail::getBookingId, detail -> detail, (first, ignored) -> first));
        Map<Long, Room> rooms = roomRepository.findAll().stream()
                .collect(Collectors.toMap(Room::getId, room -> room, (first, ignored) -> first));
        List<Map<String, Object>> tickets = serviceTicketRepository.findAll().stream()
                .sorted(Comparator.comparing(ServiceTicket::getId).reversed())
                .map(ticket -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    Service service = services.get(ticket.getServiceId());
                    BookingOrder booking = bookings.get(ticket.getBookingId());
                    Customer customer = booking != null ? customers.get(booking.getCustomerId()) : null;
                    Invoice invoice = invoices.get(ticket.getBookingId());
                    BookingDetail detail = bookingDetails.get(ticket.getBookingId());
                    Room room = detail != null ? rooms.get(detail.getRoomId()) : null;
                    int quantity = ticket.getQuantity() != null ? ticket.getQuantity() : 1;
                    double unitPrice = ticket.getPrice() != null
                            ? ticket.getPrice()
                            : service != null && service.getPrice() != null ? service.getPrice() : 0.0;
                    item.put("id", ticket.getId());
                    item.put("bookingId", ticket.getBookingId());
                    item.put("serviceId", ticket.getServiceId());
                    item.put("serviceName", service != null ? service.getName() : "Dịch vụ #" + ticket.getServiceId());
                    item.put("quantity", quantity);
                    item.put("unitPrice", unitPrice);
                    item.put("lineTotal", unitPrice * quantity);
                    item.put("status", ticket.getStatus() != null ? ticket.getStatus() : "PENDING");
                    item.put("requestGroupId", ticket.getRequestGroupId() != null
                            ? ticket.getRequestGroupId()
                            : "LEGACY-" + ticket.getId());
                    item.put("orderedAt", ticket.getOrderedAt());
                    item.put("invoiceId", invoice != null ? invoice.getId() : null);
                    item.put("guestName", customer != null ? customer.getFullName()
                            : invoice != null ? invoice.getGuestName() : null);
                    item.put("roomName", room != null ? room.getName() : null);
                    return item;
                }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu dịch vụ thành công", tickets));
    }

    @PatchMapping("/service-staff/tickets/{id}/status")
    public ResponseEntity<?> updateServiceTicketStatus(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> payload) {
        ResponseEntity<?> denied = requireServiceStaff(authorization);
        if (denied != null) return denied;
        String status = payload.getOrDefault("status", "").trim().toUpperCase(Locale.ROOT);
        if (!Set.of("PENDING", "IN_PROGRESS", "COMPLETED").contains(status)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Trạng thái dịch vụ không hợp lệ"));
        }
        Optional<ServiceTicket> ticket = serviceTicketRepository.findById(id);
        if (ticket.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ticket.get().setStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật trạng thái", serviceTicketRepository.save(ticket.get())));
    }

    private ResponseEntity<?> requireServiceStaff(String authorization) {
        try {
            com.votricuong.mayhotel.services.MobileTokenService.Claims claims =
                    mobileTokenService.verifyAuthorization(authorization);
            if (claims.roleId() != 6L && !"SERVICE_STAFF".equalsIgnoreCase(claims.role())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Chức năng chỉ dành cho nhân viên dịch vụ"));
            }
            return null;
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage()));
        }
    }

    private int effectiveGuestCount(BookingOrder order) {
        return order.getGuestCount() == null || order.getGuestCount() < 1 ? 1 : order.getGuestCount();
    }

    private int effectiveVerifiedGuestCount(BookingOrder order) {
        if (order.getVerifiedGuestCount() != null) return order.getVerifiedGuestCount();
        return Boolean.TRUE.equals(order.getIsFaceVerified()) ? effectiveGuestCount(order) : 0;
    }

    private int optionalPositiveInt(Map<String, Object> payload, String field, int defaultValue) {
        int value = optionalNonNegativeInt(payload, field, defaultValue);
        if (value < 1) throw new IllegalArgumentException(field + " phải lớn hơn 0");
        return value;
    }

    private int optionalNonNegativeInt(Map<String, Object> payload, String field, int defaultValue) {
        Object raw = payload.get(field);
        if (raw == null || raw.toString().isBlank()) return defaultValue;
        try {
            int value = Integer.parseInt(raw.toString());
            if (value < 0) throw new IllegalArgumentException(field + " không được âm");
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " phải là số nguyên");
        }
    }

    private Integer asInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String requiredString(Map<String, Object> payload, String field) {
        String value = optionalString(payload, field);
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " không được để trống");
        }
        return value;
    }

    private String optionalString(Map<String, Object> payload, String field) {
        Object value = payload.get(field);
        return value == null ? null : value.toString().trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Date calculateAutoCancelTime(Date expectedIn, boolean highRisk) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(expectedIn);
        calendar.set(Calendar.HOUR_OF_DAY, highRisk ? 14 : 18);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private String resolvePaymentMethod(Long paymentMethodId) {
        return switch (paymentMethodId.intValue()) {
            case 1 -> "CARD";
            case 2 -> "MOMO";
            case 3 -> "ZALOPAY";
            case 4 -> "GOOGLE_PAY";
            case 5 -> "PAYPAL";
            case 6 -> "SHOPEE_PAY";
            case 7 -> "APPLE_PAY";
            case 8 -> "PAY_AT_HOTEL";
            default -> throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ");
        };
    }

    private Date parseMobileDate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Ngày không được để trống");
        }

        String normalized = value.trim();
        if (normalized.length() == 10) {
            return Date.from(
                LocalDate.parse(normalized)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
            );
        }

        return Date.from(Instant.parse(normalized));
    }
}
