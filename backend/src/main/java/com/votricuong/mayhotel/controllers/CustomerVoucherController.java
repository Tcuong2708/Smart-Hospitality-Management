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
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/customer/vouchers")
public class CustomerVoucherController extends BaseController {
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
        List<Promotion> privateVouchers = assignments.findByCustomerIdAndStatus(customer.getId(), "UNUSED")
                .stream().map(item -> promotions.findById(item.getPromotionId()).orElse(null))
                .filter(Objects::nonNull).filter(item -> valid(item, now)).toList();
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
        return "ACTIVE".equalsIgnoreCase(item.getStatus())
                && (item.getStartDate() == null || !item.getStartDate().after(now))
                && (item.getEndDate() == null || !item.getEndDate().before(now));
    }

    private Customer currentCustomer(HttpSession session) {
        Object value = session.getAttribute("user");
        if (!(value instanceof User user) || user.getId() == null) return null;
        return customers.findFirstByUserIdOrderByIdAsc(user.getId()).orElse(null);
    }
}
