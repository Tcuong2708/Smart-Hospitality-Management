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

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
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

    public MobileApiController(RoomRepository roomRepository, ServiceRepository serviceRepository, InvoiceRepository invoiceRepository, UserRepository userRepository, EmailService emailService, OtpCodeRepository otpCodeRepository, com.votricuong.mayhotel.repositories.RoomTypeRepository roomTypeRepository, BookingOrderRepository bookingOrderRepository, BookingDetailRepository bookingDetailRepository, ServiceTicketRepository serviceTicketRepository, CustomerRepository customerRepository, SequenceGeneratorService sequenceGeneratorService) {
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
                    
                    int roleId = 3; // customer
                    if (user.getRoleId() != null && user.getRoleId() == 1L) roleId = 1;
                    else if (user.getRoleId() != null && user.getRoleId() == 2L) roleId = 2;
                    data.put("role", roleId);
                    
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
                map.put("maTrangThai", "Vacant".equalsIgnoreCase(r.getStatus()) ? 1 : 2);
                
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
            if (!ngayNhanStr.endsWith("Z")) ngayNhanStr += "Z";
            if (!ngayTraStr.endsWith("Z")) ngayTraStr += "Z";
            
            Date ngayNhan = Date.from(Instant.parse(ngayNhanStr));
            Date ngayTra = Date.from(Instant.parse(ngayTraStr));
            
            if (ngayTra.before(ngayNhan) || ngayTra.equals(ngayNhan)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Ngày trả phòng phải sau ngày nhận phòng"));
            }

            List<Room> allRooms = roomRepository.findAll();
            List<RoomType> allRoomTypes = roomTypeRepository.findAll();
            Map<Long, RoomType> typeMap = allRoomTypes.stream().collect(Collectors.toMap(RoomType::getId, t -> t));
            
            // Lấy tất cả các phiếu đặt phòng có trùng lịch để loại bỏ phòng bận
            List<Long> bookedRoomIds = invoiceRepository.findAll().stream()
                .filter(inv -> "Đã xác nhận".equals(inv.getInvoiceStatus()) || "Chờ thanh toán".equals(inv.getInvoiceStatus()) || "Reserved".equals(inv.getInvoiceStatus()))
                .map(Invoice::getBookingId) // mock, vì DB hiện chưa nối trực tiếp roomID trong invoice. 
                // DO NOT USE INVOICE TO FILTER IN MOCK, JUST USE ROOM STATUS FOR NOW.
                .collect(Collectors.toList());

            // Gom nhóm các phòng trống theo loại phòng (ẩn đi tên phòng cụ thể)
            Map<Long, List<Room>> availableRoomsByType = allRooms.stream()
                .filter(r -> "Vacant".equalsIgnoreCase(r.getStatus()) || 
                             "Còn phòng".equalsIgnoreCase(r.getStatus()) || 
                             "Trống".equalsIgnoreCase(r.getStatus()) || 
                             "Phòng trống".equalsIgnoreCase(r.getStatus()))
                .collect(Collectors.groupingBy(Room::getRoomTypeId));

            List<Map<String, Object>> roomsData = availableRoomsByType.entrySet().stream()
                .map(entry -> {
                    Long roomTypeId = entry.getKey();
                    List<Room> roomsOfType = entry.getValue();
                    RoomType rt = typeMap.get(roomTypeId);
                    
                    if (rt == null || roomsOfType.isEmpty()) return null;
                    
                    // Lọc theo số lượng người lớn
                    Integer maxOcc = rt.getMaxOccupancy() != null ? rt.getMaxOccupancy() : 2;
                    if (maxOcc < adults) return null;
                    
                    Room firstRoom = roomsOfType.get(0);
                    
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rt.getId()); // Trả về ID của loại phòng
                    map.put("name", rt.getName());
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> addBooking(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            Long roomTypeId = Long.valueOf(payload.get("roomTypeId").toString());
            String ngayNhanStr = (String) payload.get("ngayNhan");
            String ngayTraStr = (String) payload.get("ngayTra");
            
            if (!ngayNhanStr.endsWith("Z")) ngayNhanStr += "Z";
            if (!ngayTraStr.endsWith("Z")) ngayTraStr += "Z";
            
            Date ngayNhan = Date.from(Instant.parse(ngayNhanStr));
            Date ngayTra = Date.from(Instant.parse(ngayTraStr));
            
            List<Room> availableRooms = roomRepository.findAll().stream()
                .filter(r -> roomTypeId.equals(r.getRoomTypeId()) && "Vacant".equalsIgnoreCase(r.getStatus()))
                .collect(Collectors.toList());
                
            if (availableRooms.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Hết phòng trống cho loại phòng này."));
            }
            
            Room selectedRoom = availableRooms.get(0);
            
            // Xử lý Customer
            List<Customer> customers = customerRepository.findByUserId(userId);
            Customer customer;
            if (customers.isEmpty()) {
                customer = new Customer();
                customer.setId(sequenceGeneratorService.generateSequence("customers_sequence"));
                customer.setUserId(userId);
                
                userRepository.findById(userId).ifPresent(u -> {
                    customer.setFullName(u.getUsername() != null ? u.getUsername() : "Khách hàng Mobile");
                    customer.setPhone(u.getPhone());
                    customer.setEmail(u.getEmail());
                });
                customerRepository.save(customer);
            } else {
                customer = customers.get(0);
            }

            long diffInMillies = Math.abs(ngayTra.getTime() - ngayNhan.getTime());
            long diff = java.util.concurrent.TimeUnit.DAYS.convert(diffInMillies, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (diff == 0) diff = 1;
            double total = selectedRoom.getPrice() * diff;
            
            BookingOrder order = new BookingOrder();
            order.setId(sequenceGeneratorService.generateSequence("booking_orders_sequence"));
            order.setCustomerId(customer.getId());
            order.setBookingDate(new Date());
            order.setExpectedIn(ngayNhan);
            order.setExpectedOut(ngayTra);
            order.setStatus("Đã xác nhận"); // Đã xác nhận luôn cho luồng Mobile nhanh
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
            invoice.setInvoiceStatus("Chờ thanh toán");
            invoice.setCreatedAt(new Date());
            invoice.setUserId(userId);
            invoiceRepository.save(invoice);
            
            selectedRoom.setStatus("Reserved");
            roomRepository.save(selectedRoom);
            
            Map<String, Object> resp = new HashMap<>();
            resp.put("bookingId", order.getId());
            resp.put("invoiceId", invoice.getId());
            resp.put("roomId", selectedRoom.getId());
            resp.put("total", total);
            
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
                .sorted((a, b) -> b.getBookingDate().compareTo(a.getBookingDate()))
                .collect(Collectors.toList());
                
            List<Map<String, Object>> data = new ArrayList<>();
            for (BookingOrder o : orders) {
                double tongTien = 0.0;
                Optional<Invoice> invOpt = invoiceRepository.findAll().stream()
                        .filter(inv -> o.getId().equals(inv.getBookingId()))
                        .findFirst();
                if (invOpt.isPresent()) tongTien = invOpt.get().getTotalAmount();
                
                Map<String, Object> map = new HashMap<>();
                map.put("bookingId", o.getId());
                map.put("ngayNhan", o.getExpectedIn().toInstant().toString());
                map.put("ngayTra", o.getExpectedOut().toInstant().toString());
                map.put("tongTien", tongTien);
                map.put("trangThai", o.getStatus() != null ? o.getStatus() : "Chờ xác nhận");
                
                // Tìm chi tiết để lấy thông tin phòng
                List<BookingDetail> details = bookingDetailRepository.findByBookingId(o.getId());
                if (!details.isEmpty() && details.get(0).getRoomId() != null) {
                    roomRepository.findById(details.get(0).getRoomId()).ifPresent(r -> {
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
                .filter(o -> "Đã nhận phòng".equalsIgnoreCase(o.getStatus()) || "Occupied".equalsIgnoreCase(o.getStatus()) || "In Use".equalsIgnoreCase(o.getStatus()))
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkinAIAndNFC(@RequestBody Map<String, Object> payload) {
        try {
            Long bookingId = Long.valueOf(payload.get("bookingId").toString());
            String cccdData = (String) payload.getOrDefault("cccdData", "AI_VERIFIED"); 
            
            Optional<BookingOrder> orderOpt = bookingOrderRepository.findById(bookingId);
            if (orderOpt.isEmpty()) return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy đơn đặt phòng"));
            
            BookingOrder order = orderOpt.get();
            order.setStatus("Đã nhận phòng");
            bookingOrderRepository.save(order);
            
            List<BookingDetail> details = bookingDetailRepository.findByBookingId(order.getId());
            Long roomId = null;
            if (!details.isEmpty() && details.get(0).getRoomId() != null) {
                roomId = details.get(0).getRoomId();
                roomRepository.findById(roomId).ifPresent(r -> {
                    r.setStatus("Occupied");
                    roomRepository.save(r);
                });
            }
            
            String nfcCode = "MAYHOTEL-ROOM-" + (roomId != null ? roomId : "UNKNOWN") + "-" + UUID.randomUUID().toString().substring(0, 8);
            
            Map<String, Object> resp = new HashMap<>();
            resp.put("bookingId", bookingId);
            resp.put("status", "Đã nhận phòng");
            resp.put("canOrderService", true);
            resp.put("nfcAccessCode", nfcCode);
            resp.put("cccdParsed", cccdData);
            
            return ResponseEntity.ok(ApiResponse.success("Nhận phòng thành công. Quý khách đã có thể đặt dịch vụ.", resp));
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
            if (!details.isEmpty() && details.get(0).getRoomId() != null) {
                Long roomId = details.get(0).getRoomId();
                roomRepository.findById(roomId).ifPresent(r -> {
                    r.setStatus("Cleaning"); // Chờ dọn dẹp
                    roomRepository.save(r);
                });
            }
            
            // Xử lý Invoice thành Completed
            invoiceRepository.findAll().stream()
                .filter(inv -> bookingId.equals(inv.getBookingId()))
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
            Integer quantity = (Integer) payload.getOrDefault("quantity", 1);
            
            Optional<BookingOrder> orderOpt = bookingOrderRepository.findById(bookingId);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy thông tin đơn đặt phòng."));
            }

            BookingOrder order = orderOpt.get();
            String st = order.getStatus();

            // Kiểm tra chỉ cho phép gọi dịch vụ khi đã Check-In và chưa Check-Out
            boolean isCheckedIn = "Đã nhận phòng".equalsIgnoreCase(st) || "Occupied".equalsIgnoreCase(st) || "In Use".equalsIgnoreCase(st);
            if (!isCheckedIn) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Quý khách chưa Check-in hoặc đã Trả phòng. Dịch vụ tạm thời bị khóa!"));
            }
            
            Optional<Service> srvOpt = serviceRepository.findById(serviceId);
            if (srvOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Dịch vụ không tồn tại."));
            }
            
            Service srv = srvOpt.get();
            
            ServiceTicket stItem = new ServiceTicket();
            stItem.setId(sequenceGeneratorService.generateSequence("service_tickets_sequence"));
            stItem.setBookingId(bookingId);
            stItem.setServiceId(serviceId);
            stItem.setQuantity(quantity);
            stItem.setPrice(srv.getPrice());
            serviceTicketRepository.save(stItem);

            // Cập nhật tổng tiền vào hóa đơn hiện tại (nếu có)
            invoiceRepository.findAll().stream()
                .filter(inv -> bookingId.equals(inv.getBookingId()))
                .findFirst()
                .ifPresent(inv -> {
                    double currentTotal = inv.getTotalAmount() != null ? inv.getTotalAmount() : 0.0;
                    inv.setTotalAmount(currentTotal + (srv.getPrice() * quantity));
                    invoiceRepository.save(inv);
                });
            
            // Generate Receipt Data cho Mobile render
            Map<String, Object> receipt = new HashMap<>();
            receipt.put("title", "HÓA ĐƠN DỊCH VỤ");
            receipt.put("hotelName", "MAY HOTEL");
            receipt.put("address", "Tây Thạnh, TP.HCM");
            receipt.put("hotline", "0123456789");
            receipt.put("serviceName", srv.getName());
            receipt.put("quantity", quantity);
            receipt.put("unitPrice", srv.getPrice());
            receipt.put("totalPrice", srv.getPrice() * quantity);
            receipt.put("message", "Cảm ơn quý khách đã sử dụng dịch vụ!");
            
            return ResponseEntity.ok(ApiResponse.success("Đặt dịch vụ thành công", receipt));
        } catch(Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
