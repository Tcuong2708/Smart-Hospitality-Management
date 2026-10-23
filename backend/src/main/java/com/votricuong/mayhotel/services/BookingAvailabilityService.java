package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.enums.RoomStatus;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class BookingAvailabilityService {

    public enum RoomTimelineStatus {
        OCCUPIED,
        RESERVED
    }

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

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

    private static final Set<String> CHECKED_IN_BOOKING_STATUSES = Set.of(
            "đã nhận phòng",
            "occupied",
            "in use",
            "đang ở",
            "đang lưu trú"
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
        return findAvailableRoomsExcludingBooking(roomTypeId, checkIn, checkOut, null);
    }

    public List<Room> findAvailableRoomsExcludingBooking(
            Long roomTypeId, Date checkIn, Date checkOut, Long excludedBookingId) {
        validateStay(checkIn, checkOut);
        Set<Long> conflictingRoomIds = findConflictingRoomIds(checkIn, checkOut, excludedBookingId);

        return roomRepository.findAll().stream()
                .filter(room -> roomTypeId == null || roomTypeId.equals(room.getRoomTypeId()))
                .filter(room -> isOperational(room, checkIn))
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

    public Map<Long, RoomTimelineStatus> findRoomUsageOn(LocalDate selectedDate) {
        if (selectedDate == null) {
            throw new IllegalArgumentException("Ngày xem sơ đồ phòng không được để trống");
        }

        Map<Long, RoomTimelineStatus> usageByRoom = new HashMap<>();
        LocalDate today = LocalDate.now(HOTEL_ZONE);

        for (BookingOrder order : bookingOrderRepository.findAll()) {
            if (order == null || order.getId() == null) {
                continue;
            }
            for (BookingDetail detail : bookingDetailRepository.findByBookingId(order.getId())) {
                if (detail == null || detail.getRoomId() == null) {
                    continue;
                }

                RoomTimelineStatus usage = usageFor(order, detail, selectedDate, today);
                if (usage == null) {
                    continue;
                }
                usageByRoom.merge(detail.getRoomId(), usage,
                        (current, candidate) -> current == RoomTimelineStatus.OCCUPIED
                                ? current
                                : candidate);
            }
        }
        return usageByRoom;
    }

    public Set<Long> findOccupiedRoomIdsOn(LocalDate selectedDate) {
        Set<Long> occupiedRoomIds = new HashSet<>();
        findRoomUsageOn(selectedDate).forEach((roomId, status) -> {
            if (status == RoomTimelineStatus.OCCUPIED) {
                occupiedRoomIds.add(roomId);
            }
        });
        return occupiedRoomIds;
    }

    public boolean isRoomOccupiedOn(Long roomId, LocalDate selectedDate) {
        return roomId != null && findOccupiedRoomIdsOn(selectedDate).contains(roomId);
    }

    private RoomTimelineStatus usageFor(BookingOrder order,
                                        BookingDetail detail,
                                        LocalDate selectedDate,
                                        LocalDate today) {
        boolean hasActualStay = detail.getActualIn() != null;
        boolean checkedIn = CHECKED_IN_BOOKING_STATUSES.contains(normalize(order.getStatus()));

        if (hasActualStay || checkedIn) {
            LocalDate stayStart = toLocalDate(hasActualStay ? detail.getActualIn() : order.getExpectedIn());
            LocalDate stayEnd = toLocalDate(detail.getActualOut() != null
                    ? detail.getActualOut()
                    : order.getExpectedOut());

            boolean withinRecordedStay = containsDate(stayStart, stayEnd, selectedDate);
            boolean activeOverstayToday = detail.getActualOut() == null
                    && checkedIn
                    && stayStart != null
                    && !today.isBefore(stayStart)
                    && selectedDate.equals(today);
            if (withinRecordedStay || activeOverstayToday) {
                return RoomTimelineStatus.OCCUPIED;
            }
        }

        LocalDate expectedStart = toLocalDate(order.getExpectedIn());
        LocalDate expectedEnd = toLocalDate(order.getExpectedOut());
        if (blocksInventory(order) && containsDate(expectedStart, expectedEnd, selectedDate)) {
            return RoomTimelineStatus.RESERVED;
        }
        return null;
    }

    private boolean containsDate(LocalDate start, LocalDate end, LocalDate selectedDate) {
        if (start == null || selectedDate.isBefore(start)) {
            return false;
        }
        return end == null || selectedDate.isBefore(end);
    }

    private LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toInstant().atZone(HOTEL_ZONE).toLocalDate();
    }

    private Set<Long> findConflictingRoomIds(Date checkIn, Date checkOut, Long excludedBookingId) {
        Set<Long> conflictingRoomIds = new HashSet<>();

        for (BookingOrder order : bookingOrderRepository.findAll()) {
            if (Objects.equals(order.getId(), excludedBookingId)) {
                continue;
            }
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

    private boolean isOperational(Room room, Date requestedCheckIn) {
        if (room == null) return false;
        RoomStatus current = RoomStatus.fromStored(room.getStatus());
        if (current == RoomStatus.MAINTENANCE) return false;
        if (current != RoomStatus.CLEANING) return true;
        LocalDate requestedDate = toLocalDate(requestedCheckIn);
        return requestedDate != null && requestedDate.isAfter(LocalDate.now(HOTEL_ZONE));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
