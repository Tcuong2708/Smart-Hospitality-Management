package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class BookingAvailabilityService {

    private static final Set<String> NON_BLOCKING_BOOKING_STATUSES = Set.of(
            "đã hủy",
            "cancelled",
            "canceled",
            "hủy do khách không đến",
            "đã trả phòng",
            "completed"
    );

    private static final Set<String> OUT_OF_SERVICE_ROOM_STATUSES = Set.of(
            "maintenance",
            "bảo trì",
            "đang bảo trì",
            "out of service",
            "cleaning",
            "chờ dọn dẹp"
    );

    private final RoomRepository roomRepository;
    private final BookingOrderRepository bookingOrderRepository;
    private final BookingDetailRepository bookingDetailRepository;

    public BookingAvailabilityService(RoomRepository roomRepository,
                                      BookingOrderRepository bookingOrderRepository,
                                      BookingDetailRepository bookingDetailRepository) {
        this.roomRepository = roomRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingDetailRepository = bookingDetailRepository;
    }

    public void validateStay(Date checkIn, Date checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Ngày nhận và ngày trả phòng không được để trống");
        }
        if (!checkOut.after(checkIn)) {
            throw new IllegalArgumentException("Ngày trả phòng phải sau ngày nhận phòng");
        }
    }

    public List<Room> findAvailableRooms(Long roomTypeId, Date checkIn, Date checkOut) {
        validateStay(checkIn, checkOut);
        Set<Long> conflictingRoomIds = findConflictingRoomIds(checkIn, checkOut);

        return roomRepository.findAll().stream()
                .filter(room -> roomTypeId == null || roomTypeId.equals(room.getRoomTypeId()))
                .filter(this::isOperational)
                .filter(room -> room.getId() != null && !conflictingRoomIds.contains(room.getId()))
                .toList();
    }

    public boolean isRoomAvailable(Long roomId, Date checkIn, Date checkOut) {
        if (roomId == null) {
            return false;
        }
        return findAvailableRooms(null, checkIn, checkOut).stream()
                .anyMatch(room -> roomId.equals(room.getId()));
    }

    private Set<Long> findConflictingRoomIds(Date checkIn, Date checkOut) {
        Set<Long> conflictingRoomIds = new HashSet<>();

        for (BookingOrder order : bookingOrderRepository.findAll()) {
            if (!blocksInventory(order) || !overlaps(order, checkIn, checkOut)) {
                continue;
            }
            for (BookingDetail detail : bookingDetailRepository.findByBookingId(order.getId())) {
                if (detail.getRoomId() != null) {
                    conflictingRoomIds.add(detail.getRoomId());
                }
            }
        }
        return conflictingRoomIds;
    }

    private boolean blocksInventory(BookingOrder order) {
        if (order == null || order.getId() == null) {
            return false;
        }
        String status = normalize(order.getStatus());
        return !NON_BLOCKING_BOOKING_STATUSES.contains(status);
    }

    private boolean overlaps(BookingOrder order, Date requestedCheckIn, Date requestedCheckOut) {
        Date existingCheckIn = order.getExpectedIn();
        Date existingCheckOut = order.getExpectedOut();
        if (existingCheckIn == null || existingCheckOut == null) {
            return false;
        }
        return existingCheckIn.before(requestedCheckOut) && existingCheckOut.after(requestedCheckIn);
    }

    private boolean isOperational(Room room) {
        return room != null && !OUT_OF_SERVICE_ROOM_STATUSES.contains(normalize(room.getStatus()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
