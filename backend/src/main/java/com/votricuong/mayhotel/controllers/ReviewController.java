package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Review;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.ReviewRepository;
import com.votricuong.mayhotel.services.SequenceGeneratorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Date;
import java.util.Optional;

@Controller
@RequestMapping("/review")
public class ReviewController extends BaseController {

    private final BookingOrderRepository bookingOrderRepository;
    private final ReviewRepository reviewRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    public ReviewController(BookingOrderRepository bookingOrderRepository,
                            ReviewRepository reviewRepository,
                            SequenceGeneratorService sequenceGeneratorService) {
        this.bookingOrderRepository = bookingOrderRepository;
        this.reviewRepository = reviewRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    // Hiển thị form đánh giá (UC07)
    @GetMapping("/create")
    public String showReviewForm(@RequestParam("bookingId") Long bookingId, Model model, RedirectAttributes ra) {
        Optional<BookingOrder> orderOpt = bookingOrderRepository.findById(bookingId);
        
        if (orderOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Không tìm thấy thông tin đơn đặt phòng!");
            return "redirect:/booking/history";
        }

        BookingOrder order = orderOpt.get();
        
        // Theo UC07: Chỉ được đánh giá khi đã sử dụng dịch vụ xong (Đã trả phòng)
        if (!"Đã trả phòng".equalsIgnoreCase(order.getStatus())) {
            ra.addFlashAttribute("error", "Chỉ có thể đánh giá sau khi đã hoàn tất lưu trú (Đã trả phòng)!");
            return "redirect:/booking/history/detail?id=" + bookingId;
        }

        // Kiểm tra xem đã đánh giá chưa
        Optional<Review> existingReview = reviewRepository.findByBookingId(bookingId);
        if (existingReview.isPresent()) {
            ra.addFlashAttribute("error", "Bạn đã đánh giá cho kỳ nghỉ này rồi. Cảm ơn bạn!");
            return "redirect:/booking/history/detail?id=" + bookingId;
        }

        setPageTitle(model, "Đánh giá kỳ nghỉ #" + bookingId);
        model.addAttribute("bookingId", bookingId);
        return render(model, "view/Home/review/review");
    }

    // Xử lý lưu đánh giá (UC07)
    @PostMapping("/create")
    public String submitReview(@RequestParam("bookingId") Long bookingId,
                               @RequestParam("rating") Integer rating,
                               @RequestParam("content") String content,
                               RedirectAttributes ra) {
                               
        Optional<BookingOrder> orderOpt = bookingOrderRepository.findById(bookingId);
        if (orderOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Lỗi dữ liệu!");
            return "redirect:/booking/history";
        }

        BookingOrder order = orderOpt.get();

        Review review = new Review();
        review.setId(sequenceGeneratorService.generateSequence("reviews_sequence"));
        review.setBookingId(bookingId);
        review.setCustomerId(order.getCustomerId());
        review.setRating(rating);
        review.setContent(content);
        review.setReviewDate(new Date());
        review.setStatus("Hiện"); // Mặc định hiển thị, Quản lý có thể ẩn sau
        
        reviewRepository.save(review);

        ra.addFlashAttribute("success", "Cảm ơn bạn đã đánh giá dịch vụ của May Hotel!");
        return "redirect:/booking/history/detail?id=" + bookingId;
    }
}
