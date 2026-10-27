package com.votricuong.mayhotel.scheduler;

import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.services.BookingCancellationService;
import com.votricuong.mayhotel.services.OnlineBookingPolicy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class PendingDepositScheduler {

    private final BookingOrderRepository bookingOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final BookingCancellationService cancellationService;

    public PendingDepositScheduler(BookingOrderRepository bookingOrderRepository,
                                   InvoiceRepository invoiceRepository,
                                   BookingCancellationService cancellationService) {
        this.bookingOrderRepository = bookingOrderRepository;
        this.invoiceRepository = invoiceRepository;
        this.cancellationService = cancellationService;
    }

    @Scheduled(cron = "15 * * * * *")
    public void expireUnpaidOnlineBookings() {
        Date now = new Date();
        for (BookingOrder order : bookingOrderRepository.findAll()) {
            if (order.getId() == null || order.getPaymentExpiresAt() == null
                    || order.getPaymentExpiresAt().after(now)
                    || !OnlineBookingPolicy.BOOKING_AWAITING_DEPOSIT.equalsIgnoreCase(order.getStatus())) {
                continue;
            }
            boolean paid = invoiceRepository.findByBookingId(order.getId()).stream()
                    .anyMatch(invoice -> OnlineBookingPolicy.DEPOSIT_PAID.equalsIgnoreCase(invoice.getDepositStatus()));
            if (paid) continue;
            try {
                cancellationService.cancel(order.getId(), BookingCancellationService.CancellationType.CUSTOMER,
                        "Hết thời hạn thanh toán cọc 15 phút");
                for (Invoice invoice : invoiceRepository.findByBookingId(order.getId())) {
                    invoice.setDepositStatus("EXPIRED");
                    invoiceRepository.save(invoice);
                }
            } catch (RuntimeException ignored) {
                // Một luồng thanh toán khác có thể vừa hoàn tất; lần chạy sau sẽ đối chiếu lại trạng thái.
            }
        }
    }
}
