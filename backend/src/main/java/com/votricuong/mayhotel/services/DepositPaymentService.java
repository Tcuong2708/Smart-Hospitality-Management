package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;

@Service
public class DepositPaymentService {

    private final InvoiceRepository invoiceRepository;
    private final BookingOrderRepository bookingOrderRepository;
    private final NoShowPredictionService noShowPredictionService;

    public DepositPaymentService(InvoiceRepository invoiceRepository,
                                 BookingOrderRepository bookingOrderRepository,
                                 NoShowPredictionService noShowPredictionService) {
        this.invoiceRepository = invoiceRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.noShowPredictionService = noShowPredictionService;
    }

    public synchronized boolean confirmDeposit(Invoice invoice, String transactionId, long paidAmount,
                                               String bankCode, String responseCode, String note) {
        Invoice current = invoiceRepository.findById(invoice.getId()).orElse(invoice);
        if (OnlineBookingPolicy.DEPOSIT_PAID.equalsIgnoreCase(current.getDepositStatus())) {
            return false;
        }
        long required = OnlineBookingPolicy.payableDeposit(current);
        if (required != paidAmount) {
            throw new IllegalArgumentException("Số tiền thanh toán không khớp tiền cọc");
        }

        boolean fullPayment = "VNPAY_FULL".equalsIgnoreCase(current.getPayMethod())
                || (current.getTotalAmount() != null && paidAmount >= Math.round(current.getTotalAmount()));
        current.setDepositStatus(OnlineBookingPolicy.DEPOSIT_PAID);
        current.setDepositPaidAmount(paidAmount);
        current.setDepositPaidAt(new Date());
        current.setPayMethod(fullPayment ? "VNPAY_FULL" : "VNPAY_DEPOSIT");
        current.setInvoiceStatus(fullPayment ? "Paid" : "Deposit Paid");
        current.setIsPaid(fullPayment);
        appendTransaction(current, transactionId, paidAmount, bankCode, "SUCCESS", responseCode, note);
        invoiceRepository.save(current);

        if (current.getBookingId() != null) {
            BookingOrder order = bookingOrderRepository.findById(current.getBookingId()).orElse(null);
            if (order != null) {
                order.setStatus(OnlineBookingPolicy.BOOKING_DEPOSIT_CONFIRMED);
                order.setPaymentExpiresAt(null);
                order.setRiskAnalysisStatus("PENDING");
                bookingOrderRepository.save(order);
                noShowPredictionService.analyzeAndSave(order.getId());
            }
        }
        return true;
    }

    public synchronized void recordFailure(Invoice invoice, String transactionId, long amount,
                                           String bankCode, String responseCode, String note) {
        Invoice current = invoiceRepository.findById(invoice.getId()).orElse(invoice);
        if (OnlineBookingPolicy.DEPOSIT_PAID.equalsIgnoreCase(current.getDepositStatus())) return;
        current.setDepositStatus(OnlineBookingPolicy.DEPOSIT_FAILED);
        appendTransaction(current, transactionId, amount, bankCode, "FAILED", responseCode, note);
        invoiceRepository.save(current);
    }

    private void appendTransaction(Invoice invoice, String transactionId, long amount, String bankCode,
                                   String status, String responseCode, String note) {
        if (invoice.getPaymentHistory() == null) invoice.setPaymentHistory(new ArrayList<>());
        boolean exists = invoice.getPaymentHistory().stream()
                .anyMatch(item -> transactionId != null && transactionId.equals(item.getTransactionId())
                        && status.equalsIgnoreCase(item.getStatus()));
        if (exists) return;
        invoice.getPaymentHistory().add(Invoice.PaymentTransaction.builder()
                .transactionId(transactionId)
                .paymentDate(new Date())
                .amount((double) amount)
                .paymentMethod("VNPAY")
                .bankCode(bankCode)
                .status(status)
                .responseCode(responseCode)
                .note(note)
                .build());
    }
}
