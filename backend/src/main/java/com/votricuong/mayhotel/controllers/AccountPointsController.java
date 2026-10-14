package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.LoyalCustomerRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class AccountPointsController {

    private final CustomerRepository customerRepository;
    private final LoyalCustomerRepository loyalCustomerRepository;
    private final InvoiceRepository invoiceRepository;

    public AccountPointsController(CustomerRepository customerRepository,
                                   LoyalCustomerRepository loyalCustomerRepository,
                                   InvoiceRepository invoiceRepository) {
        this.customerRepository = customerRepository;
        this.loyalCustomerRepository = loyalCustomerRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping("/account/points")
    public String showPoints(HttpSession session, RedirectAttributes redirectAttributes) {
        Object sessionUser = session.getAttribute("user");
        if (!(sessionUser instanceof User)) {
            redirectAttributes.addAttribute("error", "auth");
            return "redirect:/account/login";
        }

        return "forward:/frontend/account/points.html";
    }

    @GetMapping("/account/api/v1/points")
    @ResponseBody
    public Map<String, Object> getPoints(HttpSession session, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        Object sessionUser = session.getAttribute("user");
        if (!(sessionUser instanceof User user) || user.getId() == null) {
            return Map.of("success", false, "message", "Bạn chưa đăng nhập.");
        }

        Customer customer = customerRepository.findFirstByUserIdOrderByIdAsc(user.getId()).orElse(null);
        int currentPoints = customer == null || customer.getPoints() == null ? 0 : customer.getPoints();
        String tier = "Hạng Đồng";

        if (customer != null && customer.getTierId() != null) {
            tier = switch (customer.getTierId().intValue()) {
                case 4 -> "Hạng Bạch Kim";
                case 3 -> "Hạng Vàng";
                case 2 -> "Hạng Bạc";
                default -> "Hạng Đồng";
            };
        }
        if (customer != null) {
            String fallbackTier = tier;
            tier = loyalCustomerRepository.findByCustomerId(customer.getId())
                    .map(loyal -> loyal.getTier() == null ? fallbackTier : "Hạng " + loyal.getTier())
                    .orElse(fallbackTier);
        }

        List<Map<String, Object>> history = new ArrayList<>();
        for (Invoice invoice : invoiceRepository.findByUserId(user.getId())) {
            if (invoice.getEarnedPoints() != null && invoice.getEarnedPoints() > 0) {
                history.add(pointEntry(invoice, "EARN", invoice.getEarnedPoints(),
                        "Tích điểm sau khi thanh toán hóa đơn"));
            }
            if (invoice.getRedeemedPoints() != null && invoice.getRedeemedPoints() > 0) {
                history.add(pointEntry(invoice, "REDEEM", -invoice.getRedeemedPoints(),
                        "Sử dụng điểm giảm " + formatMoney(invoice.getPointsDiscount())));
            }
        }
        history.sort(Comparator.comparing(
                item -> (Date) item.get("sortDate"),
                Comparator.nullsLast(Comparator.reverseOrder())));
        history.forEach(item -> item.remove("sortDate"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("currentPoints", currentPoints);
        data.put("membershipTier", tier);
        data.put("history", history);
        return Map.of("success", true, "data", data);
    }

    private Map<String, Object> pointEntry(Invoice invoice, String type, int points, String note) {
        Date date = invoice.getCreatedAt();
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", invoice.getId() == null ? "HD" : "HD" + invoice.getId());
        entry.put("title", "Hóa đơn đặt phòng #" + (invoice.getBookingId() == null ? "" : invoice.getBookingId()));
        entry.put("date", date == null ? "" : new SimpleDateFormat("dd/MM/yyyy").format(date));
        entry.put("type", type);
        entry.put("points", points);
        entry.put("note", note);
        entry.put("sortDate", date);
        return entry;
    }

    private String formatMoney(Double amount) {
        if (amount == null) return "0 VNĐ";
        return String.format(Locale.US, "%,.0f VNĐ", amount);
    }
}
