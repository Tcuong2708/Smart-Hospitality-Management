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
    private final com.votricuong.mayhotel.repositories.CustomerRepository customerRepository;

    public LoyaltyController(LoyaltyPolicyRepository loyaltyPolicyRepository, com.votricuong.mayhotel.repositories.CustomerRepository customerRepository) {
        this.loyaltyPolicyRepository = loyaltyPolicyRepository;
        this.customerRepository = customerRepository;
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

        List<com.votricuong.mayhotel.documents.Customer> customers = customerRepository.findAll();

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
}
