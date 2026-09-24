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
import com.votricuong.mayhotel.services.EmailService;
import com.votricuong.mayhotel.documents.OtpCode;
import com.votricuong.mayhotel.repositories.OtpCodeRepository;

import java.time.LocalDate;
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

    public MobileApiController(RoomRepository roomRepository, ServiceRepository serviceRepository, InvoiceRepository invoiceRepository, UserRepository userRepository, EmailService emailService, OtpCodeRepository otpCodeRepository, com.votricuong.mayhotel.repositories.RoomTypeRepository roomTypeRepository) {
        this.roomRepository = roomRepository;
        this.serviceRepository = serviceRepository;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.otpCodeRepository = otpCodeRepository;
        this.roomTypeRepository = roomTypeRepository;
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
            Optional<User> userOpt = userRepository.findByEmail(username);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByUsername(username);
            }
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByPhone(username);
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
    
    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<Service>>> getServices() {
        return ResponseEntity.ok(ApiResponse.success("Thành công", serviceRepository.findAll()));
    }

    // ==========================================
    // 3. BOOKING & INVOICES
    // ==========================================
    
    @PostMapping("/bookings/checkout")
    public ResponseEntity<ApiResponse<Invoice>> createBooking(@RequestBody Map<String, Object> payload) {
        try {
            // Lấy thông tin từ Flutter payload
            Long maLoai = Long.valueOf(payload.get("maLoai").toString());
            int soLuong = Integer.parseInt(payload.get("soLuong").toString());
            String guestName = (String) payload.get("guestName");
            String phone = (String) payload.get("phone");
            String ghiChu = (String) payload.get("ghiChu");
            String phuongThucThanhToan = (String) payload.get("phuongThucThanhToan");
            LocalDate ngayNhan = LocalDate.parse(payload.get("ngayNhan").toString());
            LocalDate ngayTra = LocalDate.parse(payload.get("ngayTra").toString());
            
            // Tìm phòng trống
            List<Room> danhSachPhongTrong = roomRepository.findAll().stream()
                    .filter(p -> p.getRoomType() != null && p.getRoomType().getId().equals(maLoai) && "Vacant".equalsIgnoreCase(p.getStatus()))
                    .collect(Collectors.toList());

            if (danhSachPhongTrong.size() < soLuong) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Không đủ phòng trống"));
            }

            Room phongDuocChon = danhSachPhongTrong.get(0); // For demo, pick first
            
            Invoice invoice = new Invoice();
            invoice.setId(System.currentTimeMillis() % 100000); 
            // Mock booking ID for mobile creation
            invoice.setBookingId(System.currentTimeMillis() % 10000);
            invoice.setCreatedAt(new Date());
            
            invoice.setInvoiceStatus("Reserved");
            invoice.setPayMethod(phuongThucThanhToan);

            long days = java.time.temporal.ChronoUnit.DAYS.between(ngayNhan, ngayTra);
            if (days <= 0) days = 1;

            invoice.setTotalAmount(phongDuocChon.getPrice() * days);
            invoice.setNote(ghiChu != null ? ghiChu + " [Đặt từ Mobile]" : "[Đặt từ Mobile]");

            Invoice savedInvoice = invoiceRepository.save(invoice);

            phongDuocChon.setStatus("Reserved");
            roomRepository.save(phongDuocChon);

            return ResponseEntity.ok(ApiResponse.success("Đặt phòng thành công", savedInvoice));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi xử lý đặt phòng: " + e.getMessage()));
        }
    }

    @GetMapping("/bookings/history")
    public ResponseEntity<ApiResponse<List<Invoice>>> getHistory(@RequestParam("phone") String phone) {
        if (phone == null || phone.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Thiếu số điện thoại"));
        }
        // Mock returning an empty list as Invoice doesn't have phone directly mapped yet
        List<Invoice> history = new ArrayList<>();
        return ResponseEntity.ok(ApiResponse.success("Thành công", history));
    }
}
