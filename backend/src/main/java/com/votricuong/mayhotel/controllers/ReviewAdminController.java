package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.Review;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.ReviewRepository;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping({"/manager/reviews", "/admin/reviews"})
public class ReviewAdminController extends BaseController {

    private final ReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public ReviewAdminController(ReviewRepository reviewRepository, CustomerRepository customerRepository) {
        this.reviewRepository = reviewRepository;
        this.customerRepository = customerRepository;
    }

    @Data
    @Builder
    public static class ReviewDTO {
        private Long id;
        private String hoTen;
        private String ngayDanhGia;
        private Integer soSao;
        private String noiDung;
        private String status;
        private String reply;
    }

    // Xem danh sách đánh giá (UC34)
    @GetMapping
    public String listReviews(Model model) {
        List<Review> rawList = reviewRepository.findAll();
        List<ReviewDTO> reviews = rawList.stream().map(r -> {
            String name = "Khách hàng ẩn danh";
            if (r.getCustomerId() != null) {
                Customer c = customerRepository.findById(r.getCustomerId()).orElse(null);
                if (c != null && c.getFullName() != null && !c.getFullName().isEmpty()) {
                    name = c.getFullName();
                }
            }
            return ReviewDTO.builder()
                    .id(r.getId())
                    .hoTen(name)
                    .ngayDanhGia(r.getReviewDate() != null ? dateFormat.format(r.getReviewDate()) : "")
                    .soSao(r.getRating() != null ? r.getRating() : 5)
                    .noiDung(r.getContent() != null ? r.getContent() : "")
                    .status(r.getStatus() != null ? r.getStatus() : "Hiện")
                    .reply(r.getReply())
                    .build();
        }).collect(Collectors.toList());

        setPageTitle(model, "Quản lý Đánh giá");
        model.addAttribute("reviews", reviews);
        return render(model, "view/Manager/reviews/index");
    }

    // Ẩn/Hiện đánh giá (UC34)
    @PostMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable("id") Long id, RedirectAttributes ra) {
        Optional<Review> reviewOpt = reviewRepository.findById(id);
        if (reviewOpt.isPresent()) {
            Review review = reviewOpt.get();
            if ("Hiện".equalsIgnoreCase(review.getStatus())) {
                review.setStatus("Ẩn");
                ra.addFlashAttribute("success", "Đã ẩn đánh giá!");
            } else {
                review.setStatus("Hiện");
                ra.addFlashAttribute("success", "Đã hiển thị lại đánh giá!");
            }
            reviewRepository.save(review);
        } else {
            ra.addFlashAttribute("error", "Không tìm thấy đánh giá!");
        }
        return "redirect:/admin/reviews";
    }

    // Quản lý phản hồi đánh giá (UC34)
    @PostMapping("/reply/{id}")
    public String replyReview(@PathVariable("id") Long id, @RequestParam("replyContent") String replyContent, RedirectAttributes ra) {
        Optional<Review> reviewOpt = reviewRepository.findById(id);
        if (reviewOpt.isPresent()) {
            Review review = reviewOpt.get();
            review.setReply(replyContent);
            reviewRepository.save(review);
            ra.addFlashAttribute("success", "Đã gửi phản hồi thành công!");
        } else {
            ra.addFlashAttribute("error", "Không tìm thấy đánh giá!");
        }
        return "redirect:/admin/reviews";
    }
}
