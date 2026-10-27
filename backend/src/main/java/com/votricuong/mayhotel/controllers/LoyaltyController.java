package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.LoyaltyPolicy;
import com.votricuong.mayhotel.repositories.LoyaltyPolicyRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import com.votricuong.mayhotel.documents.User;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/manager/loyalty")
public class LoyaltyController extends BaseController {

    private final LoyaltyPolicyRepository loyaltyPolicyRepository;
    private final com.votricuong.mayhotel.repositories.CustomerRepository customerRepository;

    public LoyaltyController(LoyaltyPolicyRepository loyaltyPolicyRepository, com.votricuong.mayhotel.repositories.CustomerRepository customerRepository) {
        this.loyaltyPolicyRepository = loyaltyPolicyRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public String index(@RequestParam(value = "q", required = false) String query,
                        Model model, HttpSession session) {
        if (!isManager(session)) {
            return "redirect:/account/login?error=forbidden";
        }
        // Lấy cấu hình (mặc định id="singleton")
        LoyaltyPolicy policy = loyaltyPolicyRepository.findById("singleton")
                .orElseGet(LoyaltyPolicy::defaultPolicy);
        if (policy.usesLegacyThresholds()) {
            policy.upgradeLegacyThresholds();
            loyaltyPolicyRepository.save(policy);
        }

        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<com.votricuong.mayhotel.documents.Customer> customers = customerRepository.findAll().stream()
                .filter(customer -> normalizedQuery.isEmpty()
                        || (customer.getFullName() != null
                            && customer.getFullName().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                        || (customer.getPhone() != null && customer.getPhone().contains(normalizedQuery)))
                .collect(Collectors.toList());

        setPageTitle(model, "Quản lý Chính sách Tích điểm");
        model.addAttribute("policy", policy);
        model.addAttribute("customers", customers);
        model.addAttribute("customerQuery", query == null ? "" : query);
        setExtraCSS(model, "view/Manager/loyalty/index :: extra_css");

        return render(model, "view/Manager/loyalty/index");
    }

    @PostMapping("/update")
    public String updatePolicy(@RequestParam("moneyPerPoint") Double moneyPerPoint,
                               @RequestParam("silverThreshold") Integer silverThreshold,
                               @RequestParam("goldThreshold") Integer goldThreshold,
                               @RequestParam("platinumThreshold") Integer platinumThreshold,
                               HttpSession session,
                               RedirectAttributes ra) {
        if (!isManager(session)) {
            return "redirect:/account/login?error=forbidden";
        }
        try {
            if (moneyPerPoint == null || moneyPerPoint <= 0
                    || silverThreshold == null || goldThreshold == null || platinumThreshold == null
                    || silverThreshold < 0 || silverThreshold >= goldThreshold
                    || goldThreshold >= platinumThreshold) {
                throw new IllegalArgumentException("Tỷ lệ và các mốc hạng phải hợp lệ, tăng dần.");
            }
            LoyaltyPolicy policy = loyaltyPolicyRepository.findById("singleton").orElse(new LoyaltyPolicy());
            policy.setId("singleton");
            policy.setMoneyPerPoint(moneyPerPoint);
            policy.setSilverThreshold(silverThreshold);
            policy.setGoldThreshold(goldThreshold);
            policy.setPlatinumThreshold(platinumThreshold);

            loyaltyPolicyRepository.save(policy);

            // Kiểm tra và cập nhật lại hạng thành viên (KiemTraDieuKienThangHang() + CapNhatThongSo())
            List<com.votricuong.mayhotel.documents.Customer> customers = customerRepository.findAll();
            for (com.votricuong.mayhotel.documents.Customer cus : customers) {
                int pts = cus.getPoints() != null ? cus.getPoints() : 0;
                Long newTierId = 1L; // 1L = Đồng
                if (pts >= platinumThreshold) {
                    newTierId = 4L; // 4L = Bạch Kim
                } else if (pts >= goldThreshold) {
                    newTierId = 3L; // 3L = Vàng
                } else if (pts >= silverThreshold) {
                    newTierId = 2L; // 2L = Bạc
                }
                
                if (cus.getTierId() == null || !cus.getTierId().equals(newTierId)) {
                    cus.setTierId(newTierId);
                    customerRepository.save(cus);
                }
            }

            ra.addFlashAttribute("success", "Cập nhật chính sách thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/manager/loyalty";
    }

    private boolean isManager(HttpSession session) {
        Object value = session.getAttribute("user");
        if (!(value instanceof User user)) return false;
        return Long.valueOf(1L).equals(user.getRoleId()) || Long.valueOf(2L).equals(user.getRoleId());
    }
}
