package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.services.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Khách gửi đánh giá phòng sau khi đã check-in (UC07).
 * Mọi kiểm tra (đăng nhập, đúng chủ đơn, đã check-in, chưa đánh giá, 1-5 sao)
 * nằm trong ReviewService.
 */
@Controller
@RequestMapping("/review")
public class ReviewController extends BaseController {

    private final BookingOrderRepository bookingOrderRepository;
    private final ReviewService reviewService;

    public ReviewController(BookingOrderRepository bookingOrderRepository, ReviewService reviewService) {
        this.bookingOrderRepository = bookingOrderRepository;
        this.reviewService = reviewService;
    }

    @GetMapping("/create")
    public String showReviewForm(@RequestParam("bookingId") Long bookingId,
            Model model, HttpSession session, RedirectAttributes ra) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/account/login?error=auth";
        }

        Optional<BookingOrder> orderOpt = bookingOrderRepository.findById(bookingId);
        if (orderOpt.isEmpty() || !reviewService.ownsBooking(user, orderOpt.get())) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn đặt phòng của bạn!");
            return "redirect:/booking/history";
        }

        String state = reviewService.reviewState(orderOpt.get());
        if (ReviewService.STATE_REVIEWED.equals(state)) {
            ra.addFlashAttribute("error", "Bạn đã đánh giá kỳ nghỉ này rồi. Cảm ơn bạn!");
            return "redirect:/booking/history/detail?id=" + bookingId;
        }
        if (!ReviewService.STATE_CAN_REVIEW.equals(state)) {
            ra.addFlashAttribute("error", "Chỉ có thể đánh giá phòng sau khi khách đã check-in!");
            return "redirect:/booking/history/detail?id=" + bookingId;
        }

        setPageTitle(model, "Đánh giá kỳ nghỉ #" + bookingId);
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("roomLabel", reviewService.roomLabel(bookingId));
        return "view/review/review-form";
    }

    @PostMapping("/create")
    public String submitReview(@RequestParam("bookingId") Long bookingId,
            @RequestParam("rating") Integer rating,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "returnRoomId", required = false) Long returnRoomId,
            HttpSession session, RedirectAttributes ra) {
        User user = (User) session.getAttribute("user");
        String error = reviewService.submit(user, bookingId, rating, content);
        if (error != null) {
            if (user == null) {
                return "redirect:/account/login?error=auth";
            }
            ra.addFlashAttribute("error", error);
        } else {
            ra.addFlashAttribute("success", "Cảm ơn bạn đã đánh giá dịch vụ của May Hotel!");
        }
        // Gửi từ trang chi tiết phòng -> quay lại đúng phòng đó
        if (returnRoomId != null) {
            return "redirect:/rooms/detail/" + returnRoomId;
        }
        return "redirect:/booking/history/detail?id=" + bookingId;
    }
}
