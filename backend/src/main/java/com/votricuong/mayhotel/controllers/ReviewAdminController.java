package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.Review;
import com.votricuong.mayhotel.repositories.ReviewRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping({"/manager/reviews", "/admin/reviews"})
public class ReviewAdminController extends BaseController {

    private final ReviewRepository reviewRepository;

    public ReviewAdminController(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    // Xem danh sách đánh giá (UC34)
    @GetMapping
    public String listReviews(Model model) {
        List<Review> reviews = reviewRepository.findAll();
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
