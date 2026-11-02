package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.Promotion;
import com.votricuong.mayhotel.services.PromotionService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

@ControllerAdvice
public class PublicPromotionAdvice {

    private final PromotionService promotionService;

    public PublicPromotionAdvice(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @ModelAttribute
    public void addPublicPromotionBanner(Model model) {
        try {
            List<String> messages = promotionService.activePublicPromotions().stream()
                    .map(this::message)
                    .toList();
            model.addAttribute("publicPromotionMessages", messages);
        } catch (RuntimeException ignored) {
            // Banner không được phép làm gián đoạn trang khi MongoDB tạm thời mất kết nối.
            model.addAttribute("publicPromotionMessages", List.of());
        }
    }

    private String message(Promotion promotion) {
        String name = promotion.getName() == null || promotion.getName().isBlank()
                ? "Ưu đãi đang áp dụng" : promotion.getName().trim();
        String validity = validity(promotion);
        if ("AMOUNT".equalsIgnoreCase(promotion.getDiscountType())
                && promotion.getDiscountAmount() != null && promotion.getDiscountAmount() > 0) {
            return name + " — giảm " + new DecimalFormat("#,##0").format(promotion.getDiscountAmount())
                    + " đ" + validity;
        }
        if (promotion.getDiscountPercent() != null && promotion.getDiscountPercent() > 0) {
            return name + " — giảm " + new DecimalFormat("0.##").format(promotion.getDiscountPercent())
                    + "%" + validity;
        }
        return name + validity;
    }

    private String validity(Promotion promotion) {
        if (promotion.getEndDate() == null) return "";
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        return " — đến hết ngày " + formatter.format(promotion.getEndDate());
    }
}
