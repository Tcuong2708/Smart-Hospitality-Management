package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomChangeHistory;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.enums.RoomStatus;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.RoomChangeHistoryRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.utils.InputValidator;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class RoomChangeService {

    private final InvoiceRepository invoiceRepository;
    private final BookingOrderRepository bookingOrderRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomChangeHistoryRepository historyRepository;
    private final BookingAvailabilityService availabilityService;
    private final RoomStatusService roomStatusService;
    private final SequenceGeneratorService sequenceGeneratorService;

    public RoomChangeService(InvoiceRepository invoiceRepository,
                             BookingOrderRepository bookingOrderRepository,
                             BookingDetailRepository bookingDetailRepository,
                             RoomRepository roomRepository,
                             RoomTypeRepository roomTypeRepository,
                             RoomChangeHistoryRepository historyRepository,
                             BookingAvailabilityService availabilityService,
                             RoomStatusService roomStatusService,
                             SequenceGeneratorService sequenceGeneratorService) {
        this.invoiceRepository = invoiceRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.historyRepository = historyRepository;
        this.availabilityService = availabilityService;
        this.roomStatusService = roomStatusService;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    public synchronized Result change(Long oldRoomId, Long newRoomId, String reason, Long performedBy) {
        if (oldRoomId == null || newRoomId == null || oldRoomId.equals(newRoomId)) {
            throw new IllegalArgumentException("Phòng mới phải khác phòng hiện tại.");
        }
        String normalizedReason = InputValidator.requiredText(reason, "Lý do đổi phòng", 500);
        Invoice invoice = invoiceRepository.findAll().stream()
                .filter(item -> "In Use".equalsIgnoreCase(item.getInvoiceStatus())
                        && oldRoomId.equals(item.getRoomId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy khách đang lưu trú ở phòng cũ."));
        if (invoice.getBookingId() == null) {
            throw new IllegalStateException("Hóa đơn lưu trú chưa liên kết với phiếu đặt phòng.");
        }
        BookingOrder booking = bookingOrderRepository.findById(invoice.getBookingId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy phiếu đặt phòng đang lưu trú."));
        if (!isCheckedIn(booking.getStatus())) {
            throw new IllegalStateException("Chỉ được đổi phòng khi khách đã check-in và đang lưu trú.");
        }

        Room oldRoom = roomRepository.findById(oldRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Phòng cũ không tồn tại."));
        Room newRoom = roomRepository.findById(newRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Phòng mới không tồn tại."));
        if (RoomStatus.fromStored(newRoom.getStatus()) != RoomStatus.VACANT) {
            throw new IllegalStateException("Phòng mới hiện không ở trạng thái trống.");
        }

        Date now = new Date();
        Date stayEnd = booking.getExpectedOut() != null ? booking.getExpectedOut() : invoice.getCheckOutDate();
        if (stayEnd == null || !stayEnd.after(now)) {
            throw new IllegalStateException("Thời gian lưu trú đã kết thúc, không thể đổi phòng.");
        }
        if (!availabilityService.isRoomAvailable(newRoomId, now, stayEnd)) {
            throw new IllegalStateException("Phòng mới không trống trong toàn bộ thời gian lưu trú còn lại.");
        }
        validateCapacity(newRoom, invoice);

        List<BookingDetail> details = bookingDetailRepository.findByBookingId(invoice.getBookingId());
        List<BookingDetail> affectedDetails = details.stream()
                .filter(detail -> oldRoomId.equals(detail.getRoomId()))
                .toList();
        if (affectedDetails.isEmpty()) {
            throw new IllegalStateException("Chi tiết lưu trú không còn khớp với phòng hiện tại.");
        }

        long remainingNights = Math.max(1L,
                (long) Math.ceil((stayEnd.getTime() - now.getTime()) / 86_400_000.0));
        double oldPrice = value(oldRoom.getPrice());
        double newPrice = value(newRoom.getPrice());
        double difference = (newPrice - oldPrice) * remainingNights;

        roomStatusService.markOccupiedForStay(newRoomId, performedBy,
                "Đổi phòng từ " + oldRoom.getName());
        roomStatusService.markCleaningAfterCheckout(oldRoomId, performedBy);

        for (BookingDetail detail : affectedDetails) {
            detail.setRoomId(newRoomId);
            detail.setUnitPrice(newPrice);
            bookingDetailRepository.save(detail);
        }

        invoice.setRoomId(newRoomId);
        invoice.setTotalAmount(Math.max(0D, value(invoice.getTotalAmount()) + difference));
        if (invoice.getGrossAmount() != null) {
            invoice.setGrossAmount(Math.max(0D, invoice.getGrossAmount() + difference));
        }
        String note = "[ĐỔI PHÒNG] " + oldRoom.getName() + " -> " + newRoom.getName()
                + "; lý do: " + normalizedReason + "; chênh lệch: " + difference;
        invoice.setNote(invoice.getNote() == null || invoice.getNote().isBlank()
                ? note : invoice.getNote() + " | " + note);
        invoiceRepository.save(invoice);

        historyRepository.save(RoomChangeHistory.builder()
                .id(sequenceGeneratorService.generateSequence("room_change_histories_sequence"))
                .invoiceId(invoice.getId())
                .bookingId(invoice.getBookingId())
                .oldRoomId(oldRoomId)
                .newRoomId(newRoomId)
                .oldRoomPrice(oldPrice)
                .newRoomPrice(newPrice)
                .remainingNights(remainingNights)
                .priceDifference(difference)
                .reason(normalizedReason)
                .performedBy(performedBy)
                .changedAt(now)
                .build());
        return new Result(oldRoom.getName(), newRoom.getName(), difference, remainingNights);
    }

    private void validateCapacity(Room room, Invoice invoice) {
        RoomType type = room.getRoomTypeId() == null ? null
                : roomTypeRepository.findById(room.getRoomTypeId()).orElse(null);
        int guests = invoice.getGuestCount() == null ? 1 : invoice.getGuestCount();
        if (type != null && type.getMaxOccupancy() != null && guests > type.getMaxOccupancy()) {
            throw new IllegalStateException("Phòng mới không đủ sức chứa cho " + guests + " khách.");
        }
    }

    private boolean isCheckedIn(String status) {
        return "Đã nhận phòng".equalsIgnoreCase(status)
                || "Checked-in".equalsIgnoreCase(status)
                || "CHECKED_IN".equalsIgnoreCase(status);
    }

    private double value(Double number) {
        return number == null ? 0D : number;
    }

    public record Result(String oldRoomName, String newRoomName, double priceDifference,
                         long remainingNights) {
    }
}
