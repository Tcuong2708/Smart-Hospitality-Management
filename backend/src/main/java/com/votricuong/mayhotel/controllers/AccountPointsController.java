package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.PointTransaction;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.PointTransactionRepository;
import com.votricuong.mayhotel.repositories.LoyaltyPolicyRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
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
    private final PointTransactionRepository pointTransactionRepository;
    private final LoyaltyPolicyRepository loyaltyPolicyRepository;
    private final InvoiceRepository invoiceRepository;

    public AccountPointsController(CustomerRepository customerRepository,
                                   PointTransactionRepository pointTransactionRepository,
                                   LoyaltyPolicyRepository loyaltyPolicyRepository,
                                   InvoiceRepository invoiceRepository) {
        this.customerRepository = customerRepository;
        this.pointTransactionRepository = pointTransactionRepository;
        this.loyaltyPolicyRepository = loyaltyPolicyRepository;
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
        String tier = tierName(customer == null ? null : customer.getTierId());
        var policy = loyaltyPolicyRepository.findById("singleton")
                .orElseGet(com.votricuong.mayhotel.documents.LoyaltyPolicy::defaultPolicy);
        if (policy.usesLegacyThresholds()) {
            policy.upgradeLegacyThresholds();
            loyaltyPolicyRepository.save(policy);
        }
        int nextThreshold = nextThreshold(currentPoints, policy);
        String nextTier = nextTier(currentPoints, policy);

        List<Map<String, Object>> history = new ArrayList<>();
        if (customer != null) {
            for (PointTransaction transaction : pointTransactionRepository
                    .findByCustomerIdOrderByCreatedAtDesc(customer.getId())) {
                history.add(pointEntry(transaction));
            }
        }
        if (history.isEmpty()) {
            for (Invoice invoice : invoiceRepository.findByUserId(user.getId())) {
                if (invoice.getEarnedPoints() != null && invoice.getEarnedPoints() > 0) {
                    history.add(legacyPointEntry(invoice, "EARN", invoice.getEarnedPoints(),
                            "Tích điểm sau khi thanh toán hóa đơn"));
                }
                if (invoice.getRedeemedPoints() != null && invoice.getRedeemedPoints() > 0) {
                    history.add(legacyPointEntry(invoice, "REDEEM", -invoice.getRedeemedPoints(),
                            "Sử dụng điểm giảm giá hóa đơn"));
                }
            }
            history.sort(Comparator.comparing(
                    item -> (Date) item.get("sortDate"),
                    Comparator.nullsLast(Comparator.reverseOrder())));
            history.forEach(item -> item.remove("sortDate"));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("currentPoints", currentPoints);
        data.put("membershipTier", tier);
        data.put("nextTier", nextTier);
        data.put("nextTierThreshold", nextThreshold);
        data.put("pointsToNextTier", Math.max(0, nextThreshold - currentPoints));
        data.put("history", history);
        return Map.of("success", true, "data", data);
    }

    private Map<String, Object> pointEntry(PointTransaction transaction) {
        Date date = transaction.getCreatedAt();
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", transaction.getReferenceId());
        entry.put("title", transaction.getDescription());
        entry.put("date", date == null ? "" : new SimpleDateFormat("dd/MM/yyyy").format(date));
        entry.put("type", transaction.getType());
        entry.put("points", transaction.getPoints());
        entry.put("note", transaction.getDescription());
        return entry;
    }

    private Map<String, Object> legacyPointEntry(Invoice invoice, String type, int points, String note) {
        Map<String, Object> entry = new LinkedHashMap<>();
        Date date = invoice.getCreatedAt();
        entry.put("id", invoice.getId() == null ? "HD" : "HD" + invoice.getId());
        entry.put("title", note + (invoice.getId() == null ? "" : " #" + invoice.getId()));
        entry.put("date", date == null ? "" : new SimpleDateFormat("dd/MM/yyyy").format(date));
        entry.put("type", type);
        entry.put("points", points);
        entry.put("note", note);
        entry.put("sortDate", date);
        return entry;
    }

    private String tierName(Long tierId) {
        if (Long.valueOf(4L).equals(tierId)) return "Hạng Bạch Kim";
        if (Long.valueOf(3L).equals(tierId)) return "Hạng Vàng";
        if (Long.valueOf(2L).equals(tierId)) return "Hạng Bạc";
        return "Hạng Đồng";
    }

    private int nextThreshold(int points, com.votricuong.mayhotel.documents.LoyaltyPolicy policy) {
        if (policy == null) return 0;
        int silver = policy.getSilverThreshold() == null ? 0 : policy.getSilverThreshold();
        int gold = policy.getGoldThreshold() == null ? silver : policy.getGoldThreshold();
        int platinum = policy.getPlatinumThreshold() == null ? gold : policy.getPlatinumThreshold();
        if (points < silver) return silver;
        if (points < gold) return gold;
        return platinum;
    }

    private String nextTier(int points, com.votricuong.mayhotel.documents.LoyaltyPolicy policy) {
        if (policy == null) return "Bạch Kim";
        int silver = policy.getSilverThreshold() == null ? 0 : policy.getSilverThreshold();
        int gold = policy.getGoldThreshold() == null ? silver : policy.getGoldThreshold();
        int platinum = policy.getPlatinumThreshold() == null ? gold : policy.getPlatinumThreshold();
        if (points < silver) return "Bạc";
        if (points < gold) return "Vàng";
        if (points < platinum) return "Bạch Kim";
        return "Bạch Kim";
    }
}
