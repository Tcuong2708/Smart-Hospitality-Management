package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.RiskAlert;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.RiskAlertRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.services.NoShowPredictionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/receptionist")
public class AiNoShowController extends BaseController {

    @Autowired
    private BookingOrderRepository bookingOrderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RiskAlertRepository riskAlertRepository;

    @Autowired
    private NoShowPredictionService noShowPredictionService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @GetMapping("/ai-no-show")
    public String index(Model model) {
        setPageTitle(model, "Trợ lý AI - Rủi ro khách không đến");

        List<BookingOrder> activeBookings = bookingOrderRepository.findAll();
        List<Map<String, Object>> displayList = new ArrayList<>();

        for (BookingOrder order : activeBookings) {
            if (isPendingCheckIn(order.getStatus())) {
                Map<String, Object> map = new HashMap<>();
                map.put("order", order);
                map.put("statusLabel", statusLabel(order.getStatus()));
                RiskAlert alert = riskAlertRepository.findFirstByBookingId(order.getId()).orElse(null);
                map.put("riskAlert", alert);
                PaymentSummary payment = paymentSummary(order.getId());
                map.put("paymentLabel", payment.label());
                map.put("paymentAmount", payment.amount());
                map.put("paymentClass", payment.cssClass());
                
                String customerName = "Khách vãng lai";
                String customerPhone = "";
                if (order.getCustomerId() != null) {
                    Customer customer = customerRepository.findById(order.getCustomerId()).orElse(null);
                    if (customer != null) {
                        customerName = customer.getFullName() != null ? customer.getFullName() : customer.getPhone();
                        customerPhone = customer.getPhone() != null ? customer.getPhone() : "Chưa cập nhật";
                    }
                }
                map.put("customerName", customerName);
                map.put("customerPhone", customerPhone);
                displayList.add(map);
            }
        }

        // Sắp xếp theo tỷ lệ rủi ro giảm dần
        displayList.sort((a, b) -> {
            BookingOrder o1 = (BookingOrder) a.get("order");
            BookingOrder o2 = (BookingOrder) b.get("order");
            double r1 = o1.getNoShowRiskPercentage() == null ? -1.0 : o1.getNoShowRiskPercentage();
            double r2 = o2.getNoShowRiskPercentage() == null ? -1.0 : o2.getNoShowRiskPercentage();
            return Double.compare(r2, r1);
        });

        model.addAttribute("bookings", displayList);
        // Chỉ kiểm tra kết nối nhanh; AI offline không được làm trang quản trị lỗi 500.
        model.addAttribute("aiAvailable", noShowPredictionService.isAvailable());
        return render(model, "view/receptionist/ai_noshow/index");
    }

    @PostMapping("/ai-no-show/retrain")
    public String retrain(RedirectAttributes redirectAttributes) {
        int success = 0;
        int failed = 0;
        for (BookingOrder order : bookingOrderRepository.findAll()) {
            if (!isPendingCheckIn(order.getStatus()) || order.getId() == null) continue;
            BookingOrder analyzed = noShowPredictionService.analyzeAndSave(order.getId());
            if ("SUCCESS".equals(analyzed.getRiskAnalysisStatus())) success++;
            else failed++;
        }
        if (failed == 0) {
            redirectAttributes.addFlashAttribute("success",
                    "Đã phân tích lại " + success + " phiếu đang chờ nhận phòng.");
        } else {
            redirectAttributes.addFlashAttribute("warning",
                    "Đã phân tích " + success + " phiếu; " + failed
                            + " phiếu dùng mốc an toàn mặc định vì AI chưa phản hồi.");
        }
        return "redirect:/receptionist/ai-no-show";
    }

    @PostMapping("/ai-no-show/retry")
    public String retryPrediction(@RequestParam Long bookingId, RedirectAttributes redirectAttributes) {
        BookingOrder order = noShowPredictionService.analyzeAndSave(bookingId);
        if ("SUCCESS".equals(order.getRiskAnalysisStatus())) {
            redirectAttributes.addFlashAttribute("success", "Đã phân tích lại rủi ro cho phiếu #" + bookingId);
        } else {
            redirectAttributes.addFlashAttribute("error", "AI chưa sẵn sàng; phiếu vẫn giữ mốc xử lý mặc định 18:00");
        }
        return "redirect:/receptionist/ai-no-show";
    }

    @PostMapping("/ai-no-show/late-arrival")
    public String approveLateArrival(@RequestParam Long bookingId,
                                     @RequestParam String reason,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        BookingOrder order = bookingOrderRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt phòng"));
        User staff = (User) session.getAttribute("user");
        order.setLateArrivalApproved(true);
        order.setLateArrivalReason(reason == null ? "" : reason.trim());
        order.setLateArrivalAt(new java.util.Date());
        order.setLateArrivalApprovedBy(staff == null ? null : staff.getId());
        order.setAutoCancelTime(endOfCheckInDay(order.getExpectedIn()));
        bookingOrderRepository.save(order);
        redirectAttributes.addFlashAttribute("success", "Đã hủy lịch tự động lúc 14:00/18:00 cho phiếu #" + bookingId
                + ". Đơn được giữ đến 23:59 ngày nhận phòng; nếu khách vẫn không đến, hệ thống sẽ tự động hủy.");
        return "redirect:/receptionist/ai-no-show";
    }

    private java.util.Date endOfCheckInDay(java.util.Date expectedIn) {
        if (expectedIn == null) return null;
        java.util.Calendar calendar = java.util.Calendar.getInstance(
                java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        calendar.setTime(expectedIn);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
        calendar.set(java.util.Calendar.MINUTE, 59);
        calendar.set(java.util.Calendar.SECOND, 59);
        calendar.set(java.util.Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    private boolean isPendingCheckIn(String status) {
        return "Pending".equals(status)
                || "Đã xác nhận".equals(status)
                || "Chờ nhận phòng".equals(status)
                || "Đã xác nhận cọc".equals(status)
                || "Reserved".equals(status);
    }

    private String statusLabel(String status) {
        if (status == null || status.isBlank()) return "Chưa cập nhật";
        return switch (status) {
            case "Pending" -> "Chờ xác nhận";
            case "Reserved" -> "Đã giữ phòng";
            case "Checked-in", "CheckedIn" -> "Đã nhận phòng";
            case "Checked-out", "CheckedOut" -> "Đã trả phòng";
            case "Cancelled" -> "Đã hủy";
            default -> status;
        };
    }

    private PaymentSummary paymentSummary(Long bookingId) {
        if (bookingId == null) return new PaymentSummary("Chưa thanh toán", 0L, "adm-badge-neutral");
        List<Invoice> invoices = invoiceRepository.findByBookingId(bookingId);
        if (invoices == null || invoices.isEmpty()) {
            return new PaymentSummary("Chưa thanh toán", 0L, "adm-badge-neutral");
        }

        boolean fullyPaid = invoices.stream().anyMatch(invoice -> Boolean.TRUE.equals(invoice.getIsPaid())
                || "Paid".equalsIgnoreCase(invoice.getInvoiceStatus())
                || "PAID".equalsIgnoreCase(invoice.getPaymentStage()));
        long paidAmount = invoices.stream().mapToLong(invoice -> {
            if (fullyPaid && invoice.getTotalAmount() != null) return Math.round(invoice.getTotalAmount());
            return invoice.getDepositPaidAmount() == null ? 0L : invoice.getDepositPaidAmount();
        }).sum();
        if (fullyPaid) return new PaymentSummary("Đã thanh toán toàn bộ", paidAmount, "adm-badge-success");

        boolean deposited = invoices.stream().anyMatch(invoice ->
                (invoice.getDepositPaidAmount() != null && invoice.getDepositPaidAmount() > 0)
                        || "PAID".equalsIgnoreCase(invoice.getDepositStatus())
                        || "DEPOSITED".equalsIgnoreCase(invoice.getPaymentStage())
                        || "Deposit Paid".equalsIgnoreCase(invoice.getInvoiceStatus()));
        if (deposited) return new PaymentSummary("Đã thanh toán tiền cọc", paidAmount, "adm-badge-warning");
        return new PaymentSummary("Chưa thanh toán", 0L, "adm-badge-neutral");
    }

    private record PaymentSummary(String label, long amount, String cssClass) {}
}
