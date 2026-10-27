package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.services.VNPayService;
import com.votricuong.mayhotel.services.LoyaltyService;
import com.votricuong.mayhotel.services.MobileTokenService;
import com.votricuong.mayhotel.services.DepositPaymentService;
import com.votricuong.mayhotel.services.OnlineBookingPolicy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/payment")
public class PaymentApiController {

    private final VNPayService vnPayService;
    private final InvoiceRepository invoiceRepository;
    private final BookingOrderRepository bookingOrderRepository;
    private final LoyaltyService loyaltyService;
    private final MobileTokenService mobileTokenService;
    private final DepositPaymentService depositPaymentService;

    public PaymentApiController(VNPayService vnPayService,
            InvoiceRepository invoiceRepository,
            BookingOrderRepository bookingOrderRepository,
            LoyaltyService loyaltyService,
            MobileTokenService mobileTokenService,
            DepositPaymentService depositPaymentService) {
        this.vnPayService = vnPayService;
        this.invoiceRepository = invoiceRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.loyaltyService = loyaltyService;
        this.mobileTokenService = mobileTokenService;
        this.depositPaymentService = depositPaymentService;
    }

    /**
     * IPN Webhook (Instant Payment Notification) từ cổng VNPAY
     * Được VNPAY Server gọi ngầm để đồng bộ trạng thái giao dịch
     */
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnpayIPN(@RequestParam Map<String, String> allParams) {
        if (!vnPayService.isEnabled()) {
            return ResponseEntity.status(503).body(Map.of("RspCode", "99", "Message", "VNPay disabled"));
        }
        log.info("Nhận VNPAY IPN Callback cho giao dịch {}", allParams.get("vnp_TxnRef"));
        Map<String, String> response = new HashMap<>();

        // 1. Kiểm tra chữ ký bảo mật
        if (!vnPayService.validateSignature(allParams)) {
            response.put("RspCode", "97");
            response.put("Message", "Invalid signature");
            return ResponseEntity.ok(response);
        }

        String vnp_TxnRef = allParams.get("vnp_TxnRef");
        String vnp_ResponseCode = allParams.get("vnp_ResponseCode");
        String vnp_TransactionNo = allParams.get("vnp_TransactionNo");
        String vnp_BankCode = allParams.get("vnp_BankCode");
        long vnp_Amount = 0L;
        try {
            vnp_Amount = Long.parseLong(allParams.get("vnp_Amount")) / 100;
        } catch (Exception ignored) {
        }

        // 2. Tìm hóa đơn từ vnp_TxnRef (định dạng: {invoiceId}_{timestamp})
        Long invoiceId = null;
        try {
            if (vnp_TxnRef.contains("_")) {
                invoiceId = Long.parseLong(vnp_TxnRef.split("_")[0]);
            } else {
                invoiceId = Long.parseLong(vnp_TxnRef);
            }
        } catch (Exception e) {
            log.error("Lỗi parse invoiceId từ vnp_TxnRef: {}", vnp_TxnRef);
        }

        Invoice invoice = null;
        if (invoiceId != null) {
            invoice = invoiceRepository.findById(invoiceId).orElse(null);
        }

        if (invoice == null) {
            response.put("RspCode", "01");
            response.put("Message", "Order not found");
            return ResponseEntity.ok(response);
        }

        // 3. Kiểm tra số tiền
        long invoiceAmount = OnlineBookingPolicy.payableDeposit(invoice);
        if (invoiceAmount != vnp_Amount) {
            response.put("RspCode", "04");
            response.put("Message", "Invalid amount");
            return ResponseEntity.ok(response);
        }

        // 4. Kiểm tra xem hóa đơn đã được thanh toán trước đó chưa
        if (OnlineBookingPolicy.DEPOSIT_PAID.equalsIgnoreCase(invoice.getDepositStatus())) {
            response.put("RspCode", "02");
            response.put("Message", "Order already confirmed");
            return ResponseEntity.ok(response);
        }

        // 5. Cập nhật kết quả thanh toán
        if ("00".equals(vnp_ResponseCode)) {
            depositPaymentService.confirmDeposit(invoice, vnp_TxnRef, vnp_Amount, vnp_BankCode,
                    vnp_ResponseCode, "VNPAY IPN xác nhận cọc thành công - Mã GD: " + vnp_TransactionNo);
        } else {
            depositPaymentService.recordFailure(invoice, vnp_TxnRef, vnp_Amount, vnp_BankCode,
                    vnp_ResponseCode, "VNPAY IPN báo giao dịch cọc thất bại");
        }

        response.put("RspCode", "00");
        response.put("Message", "Confirm Success");
        return ResponseEntity.ok(response);
    }

    /**
     * API tạo liên kết thanh toán VNPAY dành cho Mobile App hoặc Frontend gọi REST
     */
    @PostMapping("/create-vnpay-url")
    public ResponseEntity<?> createVNPayUrl(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Long> payload, HttpServletRequest request) {
        if (!vnPayService.isEnabled()) {
            return ResponseEntity.status(503).body(Map.of("error", "Thanh toán VNPay chưa được bật."));
        }
        Long invoiceId = payload.get("invoiceId");
        if (invoiceId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Thiếu invoiceId"));
        }

        Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoice == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy hóa đơn #" + invoiceId));
        }
        if (OnlineBookingPolicy.DEPOSIT_PAID.equalsIgnoreCase(invoice.getDepositStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tiền cọc của hóa đơn đã được thanh toán."));
        }

        try {
            MobileTokenService.Claims claims = mobileTokenService.verifyAuthorization(authorization);
            if (invoice.getUserId() == null || invoice.getUserId().longValue() != claims.userId()) {
                return ResponseEntity.status(403).body(Map.of("error", "Bạn không có quyền thanh toán hóa đơn này."));
            }
        } catch (SecurityException exception) {
            return ResponseEntity.status(401).body(Map.of("error", exception.getMessage()));
        }

        String paymentUrl = vnPayService.createPaymentUrl(invoice, request);
        return ResponseEntity.ok(Map.of(
                "invoiceId", invoiceId,
                "paymentUrl", paymentUrl,
                "amount", OnlineBookingPolicy.payableDeposit(invoice),
                "remainingAmount", invoice.getRemainingAmount() == null ? 0L : invoice.getRemainingAmount()));
    }

    @GetMapping("/invoices/{invoiceId}/deposit-status")
    public ResponseEntity<?> depositStatus(
            @PathVariable Long invoiceId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            MobileTokenService.Claims claims = mobileTokenService.verifyAuthorization(authorization);
            Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
            if (invoice == null) return ResponseEntity.notFound().build();
            if (invoice.getUserId() == null || invoice.getUserId().longValue() != claims.userId()) {
                return ResponseEntity.status(403).body(Map.of("error", "Bạn không có quyền xem giao dịch này."));
            }
            BookingOrder order = invoice.getBookingId() == null ? null
                    : bookingOrderRepository.findById(invoice.getBookingId()).orElse(null);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("invoiceId", invoice.getId());
            result.put("bookingId", OnlineBookingPolicy.DEPOSIT_PAID.equalsIgnoreCase(invoice.getDepositStatus())
                    ? invoice.getBookingId() : null);
            result.put("depositStatus", invoice.getDepositStatus());
            result.put("depositAmount", OnlineBookingPolicy.payableDeposit(invoice));
            result.put("remainingAmount", invoice.getRemainingAmount() == null ? 0L : invoice.getRemainingAmount());
            result.put("bookingStatus", order == null ? null : order.getStatus());
            result.put("confirmed", OnlineBookingPolicy.DEPOSIT_PAID.equalsIgnoreCase(invoice.getDepositStatus()));
            return ResponseEntity.ok(result);
        } catch (SecurityException ex) {
            return ResponseEntity.status(401).body(Map.of("error", ex.getMessage()));
        }
    }
}
