package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.LoyaltyPolicy;
import com.votricuong.mayhotel.documents.Promotion;
import com.votricuong.mayhotel.documents.Review;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.documents.Staff;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.dto.ApiResponse;
import com.votricuong.mayhotel.repositories.BackupHistoryRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.LoyaltyPolicyRepository;
import com.votricuong.mayhotel.repositories.PromotionRepository;
import com.votricuong.mayhotel.repositories.ReviewRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.repositories.ServiceRepository;
import com.votricuong.mayhotel.repositories.ServiceTicketRepository;
import com.votricuong.mayhotel.repositories.StaffRepository;
import com.votricuong.mayhotel.repositories.UserRepository;
import com.votricuong.mayhotel.services.CustomerService;
import com.votricuong.mayhotel.services.MobileTokenService;
import com.votricuong.mayhotel.services.RoomService;
import com.votricuong.mayhotel.services.RoomTypeService;
import com.votricuong.mayhotel.services.SequenceGeneratorService;
import com.votricuong.mayhotel.services.StaffService;
import com.votricuong.mayhotel.services.UserService;
import com.votricuong.mayhotel.utils.InputValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mobile")
public class MobileManagementApiController {
    private final MobileTokenService tokenService;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ServiceRepository serviceRepository;
    private final CustomerRepository customerRepository;
    private final ReviewRepository reviewRepository;
    private final PromotionRepository promotionRepository;
    private final LoyaltyPolicyRepository loyaltyPolicyRepository;
    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final BookingOrderRepository bookingOrderRepository;
    private final BackupHistoryRepository backupHistoryRepository;
    private final ServiceTicketRepository serviceTicketRepository;
    private final RoomService roomService;
    private final RoomTypeService roomTypeService;
    private final CustomerService customerService;
    private final StaffService staffService;
    private final UserService userService;
    private final SequenceGeneratorService sequenceGenerator;

    public MobileManagementApiController(
            MobileTokenService tokenService,
            RoomRepository roomRepository,
            RoomTypeRepository roomTypeRepository,
            ServiceRepository serviceRepository,
            CustomerRepository customerRepository,
            ReviewRepository reviewRepository,
            PromotionRepository promotionRepository,
            LoyaltyPolicyRepository loyaltyPolicyRepository,
            StaffRepository staffRepository,
            UserRepository userRepository,
            InvoiceRepository invoiceRepository,
            BookingOrderRepository bookingOrderRepository,
            BackupHistoryRepository backupHistoryRepository,
            ServiceTicketRepository serviceTicketRepository,
            RoomService roomService,
            RoomTypeService roomTypeService,
            CustomerService customerService,
            StaffService staffService,
            UserService userService,
            SequenceGeneratorService sequenceGenerator) {
        this.tokenService = tokenService;
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.serviceRepository = serviceRepository;
        this.customerRepository = customerRepository;
        this.reviewRepository = reviewRepository;
        this.promotionRepository = promotionRepository;
        this.loyaltyPolicyRepository = loyaltyPolicyRepository;
        this.staffRepository = staffRepository;
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.backupHistoryRepository = backupHistoryRepository;
        this.serviceTicketRepository = serviceTicketRepository;
        this.roomService = roomService;
        this.roomTypeService = roomTypeService;
        this.customerService = customerService;
        this.staffService = staffService;
        this.userService = userService;
        this.sequenceGenerator = sequenceGenerator;
    }

    @GetMapping("/manager/dashboard")
    public ResponseEntity<?> managerDashboard(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return withRole(authorization, 2L, "MANAGER", () -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("rooms", roomRepository.count());
            data.put("roomTypes", roomTypeRepository.count());
            data.put("services", serviceRepository.count());
            data.put("customers", customerRepository.count());
            data.put("staff", staffRepository.count());
            data.put("bookings", bookingOrderRepository.count());
            data.put("reviews", reviewRepository.count());
            double revenue = invoiceRepository.findAll().stream()
                    .filter(invoice -> invoice.getTotalAmount() != null)
                    .mapToDouble(invoice -> invoice.getTotalAmount()).sum();
            data.put("revenue", revenue);
            return ok("Tải tổng quan quản lý thành công", data);
        });
    }

    @GetMapping("/manager/resources/{resource}")
    public ResponseEntity<?> managerResources(
            @PathVariable String resource,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return withRole(authorization, 2L, "MANAGER", () -> switch (resource) {
            case "rooms" -> ok("Danh sách phòng", roomRepository.findAll());
            case "room-types" -> ok("Danh sách loại phòng", roomTypeRepository.findAll());
            case "services" -> ok("Danh sách dịch vụ", serviceRepository.findAll());
            case "customers" -> ok("Danh sách khách hàng", customerRepository.findAll());
            case "reviews" -> ok("Danh sách đánh giá", reviewRepository.findAll());
            case "promotions" -> ok("Danh sách khuyến mãi", promotionRepository.findAll());
            case "staff" -> ok("Danh sách nhân viên", staffRepository.findAll());
            default -> ResponseEntity.badRequest().body(ApiResponse.error("Phân hệ quản lý không hợp lệ"));
        });
    }

    @GetMapping("/manager/loyalty-policy")
    public ResponseEntity<?> loyaltyPolicy(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return withRole(authorization, 2L, "MANAGER", () -> ok("Chính sách tích điểm",
                loyaltyPolicyRepository.findById("singleton").orElseGet(this::defaultLoyaltyPolicy)));
    }

    @PutMapping("/manager/loyalty-policy")
    public ResponseEntity<?> updateLoyaltyPolicy(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody LoyaltyPolicy policy) {
        return withRole(authorization, 2L, "MANAGER", () -> {
            validateLoyaltyPolicy(policy);
            policy.setId("singleton");
            return ok("Cập nhật chính sách tích điểm thành công", loyaltyPolicyRepository.save(policy));
        });
    }

    @PostMapping("/manager/rooms")
    public ResponseEntity<?> createRoom(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @RequestBody Room room) {
        return withRole(authorization, 2L, "MANAGER", () -> ok("Thêm phòng thành công", roomService.createRoom(room)));
    }

    @PutMapping("/manager/rooms/{id}")
    public ResponseEntity<?> updateRoom(@PathVariable Long id,
                                        @RequestHeader(value = "Authorization", required = false) String authorization,
                                        @RequestBody Room room) {
        return withRole(authorization, 2L, "MANAGER", () -> ok("Cập nhật phòng thành công", roomService.updateRoom(id, room)));
    }

    @DeleteMapping("/manager/rooms/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id,
                                        @RequestHeader(value = "Authorization", required = false) String authorization) {
        return withRole(authorization, 2L, "MANAGER", () -> {
            roomService.deleteRoom(id);
            return ok("Xóa phòng thành công", null);
        });
    }

    @PostMapping("/manager/room-types")
    public ResponseEntity<?> createRoomType(@RequestHeader(value = "Authorization", required = false) String authorization,
                                            @RequestBody RoomType roomType) {
        return withRole(authorization, 2L, "MANAGER", () -> ok("Thêm loại phòng thành công", roomTypeService.createRoomType(roomType)));
    }

    @PutMapping("/manager/room-types/{id}")
    public ResponseEntity<?> updateRoomType(@PathVariable Long id,
                                            @RequestHeader(value = "Authorization", required = false) String authorization,
                                            @RequestBody RoomType roomType) {
        return withRole(authorization, 2L, "MANAGER", () -> ok("Cập nhật loại phòng thành công", roomTypeService.updateRoomType(id, roomType)));
    }

    @DeleteMapping("/manager/room-types/{id}")
    public ResponseEntity<?> deleteRoomType(@PathVariable Long id,
                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return withRole(authorization, 2L, "MANAGER", () -> {
            roomTypeService.deleteRoomType(id);
            return ok("Xóa loại phòng thành công", null);
        });
    }

    @PostMapping("/manager/services")
    public ResponseEntity<?> createService(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody com.votricuong.mayhotel.documents.Service service) {
        return withRole(authorization, 2L, "MANAGER", () -> {
            validateService(service, null);
            service.setId(sequenceGenerator.generateSequence("services_sequence"));
            return ok("Thêm dịch vụ thành công", serviceRepository.save(service));
        });
    }

    @PutMapping("/manager/services/{id}")
    public ResponseEntity<?> updateService(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody com.votricuong.mayhotel.documents.Service payload) {
        return withRole(authorization, 2L, "MANAGER", () -> {
            com.votricuong.mayhotel.documents.Service current = serviceRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ"));
            validateService(payload, id);
            current.setName(payload.getName());
            current.setPrice(payload.getPrice());
            current.setUnit(payload.getUnit());
            return ok("Cập nhật dịch vụ thành công", serviceRepository.save(current));
        });
    }

    @DeleteMapping("/manager/services/{id}")
    public ResponseEntity<?> deleteService(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return withRole(authorization, 2L, "MANAGER", () -> {
            if (!serviceRepository.existsById(id)) throw new IllegalArgumentException("Không tìm thấy dịch vụ");
            if (serviceTicketRepository.existsByServiceId(id)) {
                throw new IllegalArgumentException("Không thể xóa dịch vụ đã phát sinh giao dịch");
            }
            serviceRepository.deleteById(id);
            return ok("Xóa dịch vụ thành công", null);
        });
    }

    @PostMapping("/manager/customers")
    public ResponseEntity<?> createCustomer(@RequestHeader(value = "Authorization", required = false) String authorization,
                                            @RequestBody Customer customer) {
        return withRole(authorization, 2L, "MANAGER", () -> ok("Thêm khách hàng thành công", customerService.createCustomer(customer)));
    }

    @PutMapping("/manager/customers/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable Long id,
                                            @RequestHeader(value = "Authorization", required = false) String authorization,
                                            @RequestBody Customer customer) {
        return withRole(authorization, 2L, "MANAGER", () -> ok("Cập nhật khách hàng thành công", customerService.updateCustomer(id, customer)));
    }

    @DeleteMapping("/manager/customers/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id,
                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return withRole(authorization, 2L, "MANAGER", () -> {
            customerService.deleteCustomer(id);
            return ok("Xóa khách hàng thành công", null);
        });
    }

    @PatchMapping("/manager/reviews/{id}")
    public ResponseEntity<?> updateReview(@PathVariable Long id,
                                          @RequestHeader(value = "Authorization", required = false) String authorization,
                                          @RequestBody Map<String, Object> payload) {
        return withRole(authorization, 2L, "MANAGER", () -> {
            Review review = reviewRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá"));
            if (payload.containsKey("status")) {
                String status = InputValidator.requiredText(String.valueOf(payload.get("status")), "Trạng thái", 20);
                if (!List.of("Hiện", "Ẩn").contains(status)) throw new IllegalArgumentException("Trạng thái đánh giá không hợp lệ");
                review.setStatus(status);
            }
            if (payload.containsKey("reply")) review.setReply(InputValidator.optionalText(String.valueOf(payload.get("reply")), "Phản hồi", 1000));
            return ok("Cập nhật đánh giá thành công", reviewRepository.save(review));
        });
    }

    @PostMapping("/manager/promotions")
    public ResponseEntity<?> createPromotion(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestBody Promotion promotion) {
        return withRole(authorization, 2L, "MANAGER", () -> {
            validatePromotion(promotion);
            promotion.setId(sequenceGenerator.generateSequence("promotions_sequence"));
            return ok("Thêm khuyến mãi thành công", promotionRepository.save(promotion));
        });
    }

    @PutMapping("/manager/promotions/{id}")
    public ResponseEntity<?> updatePromotion(@PathVariable Long id,
                                             @RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestBody Promotion promotion) {
        return withRole(authorization, 2L, "MANAGER", () -> {
            if (!promotionRepository.existsById(id)) throw new IllegalArgumentException("Không tìm thấy khuyến mãi");
            validatePromotion(promotion);
            promotion.setId(id);
            return ok("Cập nhật khuyến mãi thành công", promotionRepository.save(promotion));
        });
    }

    @DeleteMapping("/manager/promotions/{id}")
    public ResponseEntity<?> deletePromotion(@PathVariable Long id,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        return withRole(authorization, 2L, "MANAGER", () -> {
            if (!promotionRepository.existsById(id)) throw new IllegalArgumentException("Không tìm thấy khuyến mãi");
            promotionRepository.deleteById(id);
            return ok("Xóa khuyến mãi thành công", null);
        });
    }

    @PostMapping("/manager/staff")
    public ResponseEntity<?> createStaff(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @RequestBody Map<String, Object> payload) {
        return withRole(authorization, 2L, "MANAGER", () -> {
            Staff staff = new Staff();
            staff.setFullName(string(payload.get("fullName")));
            staff.setPhone(string(payload.get("phone")));
            staff.setPosition(string(payload.get("position")));
            staff.setGender(string(payload.get("gender")));
            Long roleId = longValue(payload.get("roleId"));
            return ok("Thêm nhân viên thành công", staffService.createStaff(
                    staff, string(payload.get("username")), string(payload.get("password")), roleId));
        });
    }

    @PutMapping("/manager/staff/{id}")
    public ResponseEntity<?> updateStaff(@PathVariable Long id,
                                         @RequestHeader(value = "Authorization", required = false) String authorization,
                                         @RequestBody Staff staff) {
        return withRole(authorization, 2L, "MANAGER", () -> ok("Cập nhật nhân viên thành công", staffService.updateStaff(id, staff)));
    }

    @DeleteMapping("/manager/staff/{id}")
    public ResponseEntity<?> deleteStaff(@PathVariable Long id,
                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        return withRole(authorization, 2L, "MANAGER", () -> {
            staffService.deleteStaff(id);
            return ok("Xóa nhân viên thành công", null);
        });
    }

    @GetMapping("/admin/dashboard")
    public ResponseEntity<?> adminDashboard(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return withRole(authorization, 1L, "ADMIN", () -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("users", userRepository.count());
            data.put("activeUsers", userRepository.findAll().stream().filter(this::isActive).count());
            data.put("lockedUsers", userRepository.findAll().stream().filter(user -> !isActive(user)).count());
            data.put("backupRecords", backupHistoryRepository.count());
            return ok("Tải tổng quan quản trị thành công", data);
        });
    }

    @GetMapping("/admin/users")
    public ResponseEntity<?> users(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return withRole(authorization, 1L, "ADMIN", () -> ok("Danh sách tài khoản",
                userRepository.findAll().stream().map(this::safeUser).toList()));
    }

    @PostMapping("/admin/users")
    public ResponseEntity<?> createUser(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @RequestBody User user) {
        return withRole(authorization, 1L, "ADMIN", () -> ok("Thêm tài khoản thành công", safeUser(userService.createUser(user))));
    }

    @PutMapping("/admin/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @RequestHeader(value = "Authorization", required = false) String authorization,
                                        @RequestBody User user) {
        return withRole(authorization, 1L, "ADMIN", () -> ok("Cập nhật tài khoản thành công", safeUser(userService.updateUser(id, user))));
    }

    @PatchMapping("/admin/users/{id}/status")
    public ResponseEntity<?> toggleUser(@PathVariable Long id,
                                        @RequestHeader(value = "Authorization", required = false) String authorization) {
        return withRole(authorization, 1L, "ADMIN", () -> ok("Đổi trạng thái tài khoản thành công", safeUser(userService.toggleStatus(id))));
    }

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id,
                                        @RequestHeader(value = "Authorization", required = false) String authorization) {
        return withRole(authorization, 1L, "ADMIN", () -> {
            MobileTokenService.Claims claims = tokenService.verifyAuthorization(authorization);
            if (claims.userId() == id) throw new IllegalArgumentException("Không thể xóa tài khoản đang đăng nhập");
            userService.deleteUser(id);
            return ok("Xóa tài khoản thành công", null);
        });
    }

    @GetMapping("/admin/backup-status")
    public ResponseEntity<?> backupStatus(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return withRole(authorization, 1L, "ADMIN", () -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("configured", false);
            data.put("canBackup", false);
            data.put("canRestore", false);
            data.put("message", "Backend hiện chưa cấu hình tiến trình mongodump/mongorestore an toàn.");
            data.put("history", backupHistoryRepository.findAll().stream()
                    .sorted(Comparator.comparing(item -> item.getBackupDate() == null ? new Date(0) : item.getBackupDate(), Comparator.reverseOrder()))
                    .toList());
            return ok("Trạng thái sao lưu", data);
        });
    }

    private ResponseEntity<?> withRole(String authorization, long roleId, String roleCode, ThrowingAction action) {
        try {
            MobileTokenService.Claims claims = tokenService.verifyAuthorization(authorization);
            if (claims.roleId() != roleId && !roleCode.equalsIgnoreCase(claims.role())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Bạn không có quyền sử dụng chức năng này"));
            }
            return action.run();
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
        }
    }

    private ResponseEntity<ApiResponse<Object>> ok(String message, Object data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    private Map<String, Object> safeUser(User user) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", user.getId());
        dto.put("username", user.getUsername());
        dto.put("fullName", user.getFullName());
        dto.put("email", user.getEmail());
        dto.put("phone", user.getPhone());
        dto.put("address", user.getAddress());
        dto.put("nationality", user.getNationality());
        dto.put("gender", user.getGender());
        dto.put("status", user.getStatus());
        dto.put("roleId", user.getRoleId());
        dto.put("roleCode", user.getRole());
        return dto;
    }

    private boolean isActive(User user) {
        return "Hoạt động".equalsIgnoreCase(user.getStatus()) || "Active".equalsIgnoreCase(user.getStatus());
    }

    private void validateService(com.votricuong.mayhotel.documents.Service service, Long excludedId) {
        service.setName(InputValidator.requiredText(service.getName(), "Tên dịch vụ", 100));
        service.setUnit(InputValidator.requiredText(service.getUnit(), "Đơn vị tính", 30));
        service.setPrice(InputValidator.positiveMoney(service.getPrice(), "Giá dịch vụ"));
        boolean duplicate = serviceRepository.findAll().stream().anyMatch(item ->
                item.getId() != null && !item.getId().equals(excludedId)
                        && item.getName() != null && item.getName().equalsIgnoreCase(service.getName()));
        if (duplicate) throw new IllegalArgumentException("Tên dịch vụ đã tồn tại");
    }

    private void validatePromotion(Promotion promotion) {
        promotion.setName(InputValidator.requiredText(promotion.getName(), "Tên khuyến mãi", 100));
        if (promotion.getDiscountPercent() == null || !Double.isFinite(promotion.getDiscountPercent())
                || promotion.getDiscountPercent() <= 0 || promotion.getDiscountPercent() > 100) {
            throw new IllegalArgumentException("Phần trăm giảm phải lớn hơn 0 và không vượt quá 100");
        }
        if (promotion.getStartDate() == null || promotion.getEndDate() == null || !promotion.getEndDate().after(promotion.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
        }
    }

    private void validateLoyaltyPolicy(LoyaltyPolicy policy) {
        policy.setMoneyPerPoint(InputValidator.positiveMoney(policy.getMoneyPerPoint(), "Số tiền quy đổi một điểm"));
        int silver = InputValidator.range(policy.getSilverThreshold(), "Mốc Bạc", 0, 100_000_000);
        int gold = InputValidator.range(policy.getGoldThreshold(), "Mốc Vàng", 0, 100_000_000);
        int platinum = InputValidator.range(policy.getPlatinumThreshold(), "Mốc Bạch Kim", 0, 100_000_000);
        if (!(silver < gold && gold < platinum)) throw new IllegalArgumentException("Các mốc hạng phải tăng dần: Bạc < Vàng < Bạch Kim");
    }

    private LoyaltyPolicy defaultLoyaltyPolicy() {
        return LoyaltyPolicy.builder().id("singleton").moneyPerPoint(100000D)
                .silverThreshold(100).goldThreshold(500).platinumThreshold(1000).build();
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }

    private Long longValue(Object value) {
        if (value == null) return null;
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Mã vai trò không hợp lệ");
        }
    }

    @FunctionalInterface
    private interface ThrowingAction {
        ResponseEntity<?> run() throws Exception;
    }
}
