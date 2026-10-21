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
            double total = invoice.getTotalAmount() == null ? 0.0 : invoice.getTotalAmount();
            double penaltyRate = customerPenaltyRate;
            double penaltyBase = noShow || Boolean.TRUE.equals(invoice.getIsPaid()) ? total : 0.0;
            double penalty = penaltyBase * penaltyRate / 100.0;
            double refund = Boolean.TRUE.equals(invoice.getIsPaid()) ? Math.max(total - penalty, 0.0) : 0.0;
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
        String status = order.getStatus();
        if ("Đã nhận phòng".equalsIgnoreCase(status)
                || "Checked-in".equalsIgnoreCase(status)
                || "Đã trả phòng".equalsIgnoreCase(status)
                || "Completed".equalsIgnoreCase(status)
                || "Đã hủy".equalsIgnoreCase(status)
                || "Hủy do khách không đến".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Phiếu đặt phòng không còn đủ điều kiện hủy");
        }
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
                .anyMatch(order -> !"Đã hủy".equalsIgnoreCase(order.getStatus())
                        && !"Hủy do khách không đến".equalsIgnoreCase(order.getStatus())
                        && !"Đã trả phòng".equalsIgnoreCase(order.getStatus())
                        && !"Completed".equalsIgnoreCase(order.getStatus()));
    }

    private String appendNote(String current, String extra) {
        return current == null || current.isBlank() ? extra : current + " | " + extra;
    }
}
