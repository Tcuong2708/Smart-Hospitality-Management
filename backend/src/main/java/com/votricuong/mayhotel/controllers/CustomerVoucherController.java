package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.Promotion;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.repositories.CustomerPromotionRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.PromotionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;

@Controller
@RequestMapping("/customer/vouchers")
public class CustomerVoucherController extends BaseController {
    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final CustomerPromotionRepository assignments;
    private final PromotionRepository promotions;
    private final CustomerRepository customers;

    public CustomerVoucherController(CustomerPromotionRepository assignments,
                                     PromotionRepository promotions, CustomerRepository customers) {
        this.assignments = assignments;
        this.promotions = promotions;
        this.customers = customers;
    }

    @GetMapping
    public String page(Model model, HttpSession session) {
        Customer customer = currentCustomer(session);
        if (customer == null) return "redirect:/account/login";
        Date now = new Date();
        LinkedHashMap<Long, Promotion> privateVoucherMap = new LinkedHashMap<>();
        assignments.findByCustomerIdAndStatus(customer.getId(), "UNUSED").forEach(assignment -> {
            Promotion promotion = assignment.getPromotionId() == null
                    ? null
                    : promotions.findById(assignment.getPromotionId()).orElse(null);
            // Dữ liệu cũ hoặc dữ liệu được nhập từ MongoDB có thể chỉ lưu mã khuyến mãi.
            // Đối chiếu dự phòng theo mã để trang Ưu đãi luôn đồng bộ với thông báo đã gửi.
            if (promotion == null && assignment.getPromotionCode() != null
                    && !assignment.getPromotionCode().isBlank()) {
                promotion = promotions.findByCodeIgnoreCase(assignment.getPromotionCode().trim()).orElse(null);
            }
            if (promotion != null && valid(promotion, now)) {
                privateVoucherMap.putIfAbsent(promotion.getId(), promotion);
            }
        });
        List<Promotion> privateVouchers = List.copyOf(privateVoucherMap.values());
        List<Promotion> publicVouchers = promotions.findAll().stream()
                .filter(item -> !"PRIVATE".equalsIgnoreCase(item.getVisibility()))
                .filter(item -> item.getCode() != null && !item.getCode().isBlank())
                .filter(item -> valid(item, now)).toList();
        setPageTitle(model, "Ưu đãi của tôi");
        model.addAttribute("privateVouchers", privateVouchers);
        model.addAttribute("publicVouchers", publicVouchers);
        return "view/customer/vouchers/index";
    }

    private boolean valid(Promotion item, Date now) {
        if ("DISABLED".equalsIgnoreCase(item.getStatus()) || "DRAFT".equalsIgnoreCase(item.getStatus())) {
            return false;
        }
        LocalDate today = toLocalDate(now);
        return (item.getStartDate() == null || !today.isBefore(toLocalDate(item.getStartDate())))
                && (item.getEndDate() == null || !today.isAfter(toLocalDate(item.getEndDate())));
    }

    private LocalDate toLocalDate(Date value) {
        return value.toInstant().atZone(HOTEL_ZONE).toLocalDate();
    }

    private Customer currentCustomer(HttpSession session) {
        Object value = session.getAttribute("user");
        if (!(value instanceof User user) || user.getId() == null) return null;
        return customers.findFirstByUserIdOrderByIdAsc(user.getId()).orElse(null);
    }
}
