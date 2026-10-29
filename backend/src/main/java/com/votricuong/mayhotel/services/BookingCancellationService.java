package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class BookingCancellationService {

    public enum CancellationType { CUSTOMER, NO_SHOW }
    public record CancellationQuote(double penaltyRate, double paidAmount,
                                    double penaltyAmount, double refundAmount) {}

    private final BookingOrderRepository bookingOrderRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final InvoiceRepository invoiceRepository;
    private final RoomRepository roomRepository;

    public BookingCancellationService(BookingOrderRepository bookingOrderRepository,
                                      BookingDetailRepository bookingDetailRepository,
                                      InvoiceRepository invoiceRepository,
                                      RoomRepository roomRepository) {
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.invoiceRepository = invoiceRepository;
        this.roomRepository = roomRepository;
    }

    public BookingOrder cancel(Long bookingId, CancellationType type, String reason) {
        BookingOrder order = bookingOrderRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đặt phòng"));
        validateCancellable(order);

        boolean noShow = type == CancellationType.NO_SHOW;
        double customerPenaltyRate = noShow ? 100.0 : customerPenaltyRate(order);
        order.setStatus(noShow ? "Hủy do khách không đến" : "Đã hủy");
        order.setCancellationReason(reason == null || reason.isBlank()
                ? (noShow ? "Quá hạn nhận phòng (no-show)" : "Khách hàng yêu cầu hủy")
                : reason.trim());
        order.setCancelledAt(new Date());
        bookingOrderRepository.save(order);

        for (Invoice invoice : invoiceRepository.findByBookingId(bookingId)) {
            CancellationQuote quote = quote(invoice, customerPenaltyRate);
            double penaltyRate = quote.penaltyRate();
            double penalty = quote.penaltyAmount();
            double refund = quote.refundAmount();
            invoice.setPenaltyRate(penaltyRate);
            invoice.setPenaltyAmount(penalty);
            invoice.setRefundAmount(refund);
            invoice.setCancellationType(type.name());
            invoice.setInvoiceStatus(noShow ? "Forfeited" : "Cancelled");
            invoice.setNote(appendNote(invoice.getNote(), noShow
                    ? "[NO-SHOW] Phạt 100%, không hoàn lại."
                    : "[CUSTOMER CANCEL] Phí phạt " + penaltyRate + "%; số tiền hoàn dự kiến: " + refund));
            invoiceRepository.save(invoice);
        }

        releaseRooms(order);
        return order;
    }

    public void validateCancellable(BookingOrder order) {
        if (order == null) throw new IllegalArgumentException("Không tìm thấy phiếu đặt phòng");
        String status = order.getStatus() == null ? "" : order.getStatus().trim();
        if ("Đã nhận phòng".equalsIgnoreCase(status)
                || "Đang ở".equalsIgnoreCase(status)
                || "Đang lưu trú".equalsIgnoreCase(status)
                || "Checked-in".equalsIgnoreCase(status)
                || "Occupied".equalsIgnoreCase(status)
                || "In use".equalsIgnoreCase(status)
                || "Đã trả phòng".equalsIgnoreCase(status)
                || "Hoàn tất".equalsIgnoreCase(status)
                || "Đã hoàn thành".equalsIgnoreCase(status)
                || "Completed".equalsIgnoreCase(status)
                || "Đã hủy".equalsIgnoreCase(status)
                || "Cancelled".equalsIgnoreCase(status)
                || "Hủy do khách không đến".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Phiếu đặt phòng không còn đủ điều kiện hủy");
        }
    }

    public CancellationQuote quote(BookingOrder order) {
        validateCancellable(order);
        double rate = customerPenaltyRate(order);
        Invoice invoice = invoiceRepository.findByBookingId(order.getId()).stream().findFirst().orElse(null);
        return quote(invoice, rate);
    }

    private CancellationQuote quote(Invoice invoice, double penaltyRate) {
        double total = invoice == null || invoice.getTotalAmount() == null ? 0.0 : invoice.getTotalAmount();
        double paidAmount = invoice == null ? 0.0
                : invoice.getDepositPaidAmount() != null && invoice.getDepositPaidAmount() > 0
                ? invoice.getDepositPaidAmount()
                : (Boolean.TRUE.equals(invoice.getIsPaid()) ? total : 0.0);
        double penalty = paidAmount * penaltyRate / 100.0;
        return new CancellationQuote(penaltyRate, paidAmount, penalty, Math.max(paidAmount - penalty, 0.0));
    }

    private void releaseRooms(BookingOrder cancelledOrder) {
        List<BookingDetail> details = bookingDetailRepository.findByBookingId(cancelledOrder.getId());
        for (BookingDetail detail : details) {
            if (detail.getRoomId() == null || hasAnotherActiveBooking(detail.getRoomId(), cancelledOrder.getId())) continue;
            roomRepository.findById(detail.getRoomId()).ifPresent(room -> {
                if ("Reserved".equalsIgnoreCase(room.getStatus()) || "Đã đặt".equalsIgnoreCase(room.getStatus())) {
                    room.setStatus("Trống");
                    room.setNote("");
                    roomRepository.save(room);
                }
            });
        }
    }

    private double customerPenaltyRate(BookingOrder order) {
        if (order.getExpectedIn() == null) return 0.0;
        long remainingMillis = order.getExpectedIn().getTime() - System.currentTimeMillis();
        if (remainingMillis <= 0) return 100.0;
        return remainingMillis < 24L * 60 * 60 * 1000 ? 50.0 : 0.0;
    }

    private boolean hasAnotherActiveBooking(Long roomId, Long excludedBookingId) {
        return bookingDetailRepository.findAll().stream()
                .filter(detail -> roomId.equals(detail.getRoomId()))
                .map(BookingDetail::getBookingId)
                .filter(id -> id != null && !id.equals(excludedBookingId))
                .map(bookingOrderRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .anyMatch(order -> !isClosed(order.getStatus()));
    }

    private boolean isClosed(String status) {
        return "Đã hủy".equalsIgnoreCase(status)
                || "Cancelled".equalsIgnoreCase(status)
                || "Hủy do khách không đến".equalsIgnoreCase(status)
                || "Đã trả phòng".equalsIgnoreCase(status)
                || "Hoàn tất".equalsIgnoreCase(status)
                || "Đã hoàn thành".equalsIgnoreCase(status)
                || "Completed".equalsIgnoreCase(status)
                || "Checked-out".equalsIgnoreCase(status);
    }

    private String appendNote(String current, String extra) {
        return current == null || current.isBlank() ? extra : current + " | " + extra;
    }
}
