package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.LoyalCustomer;
import com.votricuong.mayhotel.documents.LoyaltyPolicy;
import com.votricuong.mayhotel.repositories.LoyalCustomerRepository;
import com.votricuong.mayhotel.repositories.LoyaltyPolicyRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/manager/loyalty")
public class LoyaltyController extends BaseController {

    private final LoyaltyPolicyRepository loyaltyPolicyRepository;
    private final LoyalCustomerRepository loyalCustomerRepository;

    public LoyaltyController(LoyaltyPolicyRepository loyaltyPolicyRepository, LoyalCustomerRepository loyalCustomerRepository) {
        this.loyaltyPolicyRepository = loyaltyPolicyRepository;
        this.loyalCustomerRepository = loyalCustomerRepository;
    }

    @GetMapping
    public String index(Model model) {
        // Lấy cấu hình (mặc định id="singleton")
        LoyaltyPolicy policy = loyaltyPolicyRepository.findById("singleton").orElse(
                LoyaltyPolicy.builder()
                        .id("singleton")
                        .moneyPerPoint(100000.0)
                        .silverThreshold(100)
                        .goldThreshold(500)
                        .platinumThreshold(2000)
                        .build()
        );

        List<LoyalCustomer> customers = loyalCustomerRepository.findAll();

        setPageTitle(model, "Quản lý Chính sách Tích điểm");
        model.addAttribute("policy", policy);
        model.addAttribute("customers", customers);

        return render(model, "view/Manager/loyalty/index");
    }

    @PostMapping("/update")
    public String updatePolicy(@RequestParam("moneyPerPoint") Double moneyPerPoint,
                               @RequestParam("silverThreshold") Integer silverThreshold,
                               @RequestParam("goldThreshold") Integer goldThreshold,
                               @RequestParam("platinumThreshold") Integer platinumThreshold,
                               RedirectAttributes ra) {
        try {
            LoyaltyPolicy policy = loyaltyPolicyRepository.findById("singleton").orElse(new LoyaltyPolicy());
            policy.setId("singleton");
            policy.setMoneyPerPoint(moneyPerPoint);
            policy.setSilverThreshold(silverThreshold);
            policy.setGoldThreshold(goldThreshold);
            policy.setPlatinumThreshold(platinumThreshold);

            loyaltyPolicyRepository.save(policy);

            ra.addFlashAttribute("success", "Cập nhật chính sách thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/manager/loyalty";
    }
}
