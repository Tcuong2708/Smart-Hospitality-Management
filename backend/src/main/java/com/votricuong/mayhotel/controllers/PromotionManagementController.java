package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.Promotion;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.services.PromotionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/manager/promotions")
public class PromotionManagementController extends BaseController {
    private final PromotionService promotionService;
    private final CustomerRepository customerRepository;

    public PromotionManagementController(PromotionService promotionService,
                                         CustomerRepository customerRepository) {
        this.promotionService = promotionService;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public String page(org.springframework.ui.Model model, HttpSession session) {
        if (!isManager(session)) {
            return "redirect:/account/login?error=forbidden";
        }
        setPageTitle(model, "Quản lý khuyến mãi");
        setExtraCSS(model, "view/manager/promotions/index :: extra_css");
        setExtraJS(model, "view/manager/promotions/index :: extra_js");
        return render(model, "view/manager/promotions/index");
    }

    @GetMapping("/api/types")
    @ResponseBody
    public ResponseEntity<?> types(HttpSession session) {
        if (!isManager(session)) return forbidden();
        return ResponseEntity.ok(List.of(
                promotionType("GROUP", "Khách đoàn / Công ty", "bi-people-fill", "blue"),
                promotionType("EARLY_BIRD", "Đặt phòng sớm", "bi-clock-history", "gold"),
                promotionType("LOW_SEASON", "Mùa thấp điểm", "bi-cloud-sun", "green"),
                promotionType("SPECIAL_DATE", "Ngày / Sự kiện đặc biệt", "bi-star-fill", "red"),
                promotionType("LONG_STAY", "Lưu trú dài ngày", "bi-calendar-range", "yellow"),
                promotionType("VOUCHER", "Voucher", "bi-ticket-perforated", "navy")
        ));
    }

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<?> list(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) String type,
                                  @RequestParam(required = false) String status,
                                  HttpSession session) {
        if (!isManager(session)) return forbidden();
        return ResponseEntity.ok(promotionService.search(q, type, status));
    }

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> create(@RequestBody Promotion promotion, HttpSession session) {
        if (!isManager(session)) return ResponseEntity.status(403).body(Map.of("message", "Không có quyền."));
        try {
            return ResponseEntity.ok(promotionService.save(promotion));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Promotion input, HttpSession session) {
        if (!isManager(session)) return ResponseEntity.status(403).body(Map.of("message", "Không có quyền."));
        try {
            input.setId(id);
            return ResponseEntity.ok(promotionService.save(input));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/api/{id}/disable")
    @ResponseBody
    public ResponseEntity<?> disable(@PathVariable Long id, HttpSession session) {
        if (!isManager(session)) return ResponseEntity.status(403).body(Map.of("message", "Không có quyền."));
        try {
            promotionService.disable(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/api/customers")
    @ResponseBody
    public ResponseEntity<?> customers(@RequestParam(required = false) String q, HttpSession session) {
        if (!isManager(session)) return forbidden();
        String keyword = q == null ? "" : q.trim().toLowerCase(java.util.Locale.ROOT);
        return ResponseEntity.ok(customerRepository.findAll().stream()
                .filter(customer -> keyword.isEmpty()
                        || contains(customer.getFullName(), keyword)
                        || contains(customer.getPhone(), keyword)
                        || contains(customer.getEmail(), keyword))
                .map(customer -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", customer.getId());
                    item.put("name", customer.getFullName() == null ? "—" : customer.getFullName());
                    item.put("phone", customer.getPhone() == null ? "" : customer.getPhone());
                    item.put("points", customer.getPoints() == null ? 0 : customer.getPoints());
                    return item;
                }).limit(200).toList());
    }

    @PostMapping("/api/{id}/send")
    @ResponseBody
    public ResponseEntity<?> send(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                  HttpSession session) {
        if (!isManager(session)) return forbidden();
        Object raw = body.get("customerIds");
        if (!(raw instanceof List<?> values) || values.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Chưa chọn khách hàng nào."));
        }
        try {
            List<Long> customerIds = values.stream().filter(Number.class::isInstance)
                    .map(Number.class::cast).map(Number::longValue).toList();
            int sent = promotionService.sendToCustomers(id, customerIds);
            return ResponseEntity.ok(Map.of("success", true, "sent", sent));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/api/quote")
    @ResponseBody
    public ResponseEntity<?> quote(@RequestBody QuoteRequest request) {
        PromotionService.Quote quote = promotionService.bestQuote(
                request.roomAmount(), request.roomCount(), request.guestCount(), request.nights(),
                request.bookingDate(), request.stayDate(), request.roomTypeId(), request.voucherCode());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("promotion", quote.promotion() == null ? null : quote.promotion().getId());
        result.put("discount", quote.discount());
        result.put("status", quote.status());
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(403)
                .body(Map.of("message", "Bạn không có quyền quản lý khuyến mãi. Chỉ ADMIN hoặc MANAGER được phép truy cập."));
    }

    private Map<String, String> promotionType(String code, String name, String icon, String color) {
        return Map.of("code", code, "name", name, "icon", icon, "color", color);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(java.util.Locale.ROOT).contains(keyword);
    }

    private boolean isManager(HttpSession session) {
        Object value = session.getAttribute("user");
        if (!(value instanceof User user) || user.getRole() == null) return false;
        return "ADMIN".equalsIgnoreCase(user.getRole()) || "MANAGER".equalsIgnoreCase(user.getRole())
                || Long.valueOf(1L).equals(user.getRoleId()) || Long.valueOf(2L).equals(user.getRoleId());
    }

    public record QuoteRequest(double roomAmount, int roomCount, int guestCount, int nights,
                               java.util.Date bookingDate, java.util.Date stayDate,
                               Long roomTypeId, String voucherCode) {}
}
