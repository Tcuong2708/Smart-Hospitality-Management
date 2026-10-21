package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookingAvailabilityServiceTests {

    private RoomRepository roomRepository;
    private BookingOrderRepository bookingOrderRepository;
    private BookingDetailRepository bookingDetailRepository;
    private BookingAvailabilityService service;

    @BeforeEach
    void setUp() {
        roomRepository = mock(RoomRepository.class);
        bookingOrderRepository = mock(BookingOrderRepository.class);
        bookingDetailRepository = mock(BookingDetailRepository.class);
        service = new BookingAvailabilityService(
                roomRepository,
                bookingOrderRepository,
                bookingDetailRepository);
    }

    @Test
    void findsRoomsByDateOverlapInsteadOfCurrentReservedStatus() {
        Room overlappingRoom = room(1L, 10L, "Reserved");
        Room cancelledRoom = room(2L, 10L, "Reserved");
        Room adjacentRoom = room(3L, 10L, "Occupied");
        Room maintenanceRoom = room(4L, 10L, "Maintenance");

        BookingOrder overlapping = order(101L, "Pending", "2026-10-11T00:00:00Z", "2026-10-13T00:00:00Z");
        BookingOrder cancelled = order(102L, "Đã hủy", "2026-10-11T00:00:00Z", "2026-10-13T00:00:00Z");
        BookingOrder adjacent = order(103L, "Đã xác nhận cọc", "2026-10-08T00:00:00Z", "2026-10-10T00:00:00Z");

        when(roomRepository.findAll()).thenReturn(List.of(
                overlappingRoom, cancelledRoom, adjacentRoom, maintenanceRoom));
        when(bookingOrderRepository.findAll()).thenReturn(List.of(overlapping, cancelled, adjacent));
        when(bookingDetailRepository.findByBookingId(101L)).thenReturn(List.of(detail(1L)));
        when(bookingDetailRepository.findByBookingId(103L)).thenReturn(List.of(detail(3L)));

        List<Room> available = service.findAvailableRooms(
                10L,
                date("2026-10-10T00:00:00Z"),
                date("2026-10-12T00:00:00Z"));

        assertThat(available).extracting(Room::getId).containsExactly(2L, 3L);
    }

    @Test
    void keepsTheAssignedRoomWhenCheckingAvailabilityForTheSameBooking() {
        Room assignedRoom = room(1L, 10L, "Vacant");
        Room blockedByAnotherBooking = room(2L, 10L, "Vacant");
        BookingOrder current = order(101L, "Reserved", "2026-10-11T00:00:00Z", "2026-10-13T00:00:00Z");
        BookingOrder other = order(102L, "Reserved", "2026-10-11T00:00:00Z", "2026-10-13T00:00:00Z");

        when(roomRepository.findAll()).thenReturn(List.of(assignedRoom, blockedByAnotherBooking));
        when(bookingOrderRepository.findAll()).thenReturn(List.of(current, other));
        when(bookingDetailRepository.findByBookingId(101L)).thenReturn(List.of(detail(1L)));
        when(bookingDetailRepository.findByBookingId(102L)).thenReturn(List.of(detail(2L)));

        List<Room> available = service.findAvailableRoomsExcludingBooking(
                10L,
                date("2026-10-11T00:00:00Z"),
                date("2026-10-13T00:00:00Z"),
                101L);

        assertThat(available).extracting(Room::getId).containsExactly(1L);
    }

    @Test
    void rejectsAnInvalidStayRange() {
        Date sameDay = date("2026-10-10T00:00:00Z");

        assertThatThrownBy(() -> service.validateStay(sameDay, sameDay))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ngày trả phòng phải sau ngày nhận phòng");
    }

    @Test
    void roomTimelineUsesActualStayForHistoryAndExcludesCheckoutDate() {
        BookingOrder completedStay = order(
                201L,
                "Đã trả phòng",
                "2026-10-13T00:00:00Z",
                "2026-10-15T00:00:00Z");
        BookingDetail detail = BookingDetail.builder()
                .roomId(1L)
                .actualIn(date("2026-10-13T07:00:00Z"))
                .actualOut(date("2026-10-15T03:00:00Z"))
                .build();
        when(bookingOrderRepository.findAll()).thenReturn(List.of(completedStay));
        when(bookingDetailRepository.findByBookingId(201L)).thenReturn(List.of(detail));

        Map<Long, BookingAvailabilityService.RoomTimelineStatus> previousDay =
                service.findRoomUsageOn(LocalDate.of(2026, 10, 14));
        Map<Long, BookingAvailabilityService.RoomTimelineStatus> checkoutDay =
                service.findRoomUsageOn(LocalDate.of(2026, 10, 15));

        assertThat(previousDay).containsEntry(
                1L, BookingAvailabilityService.RoomTimelineStatus.OCCUPIED);
        assertThat(checkoutDay).doesNotContainKey(1L);
    }

    @Test
    void roomTimelineMarksAnUpcomingBlockingBookingAsReserved() {
        BookingOrder reserved = order(
                202L,
                "Reserved",
                "2026-10-20T00:00:00Z",
                "2026-10-22T00:00:00Z");
        when(bookingOrderRepository.findAll()).thenReturn(List.of(reserved));
        when(bookingDetailRepository.findByBookingId(202L)).thenReturn(List.of(detail(2L)));

        Map<Long, BookingAvailabilityService.RoomTimelineStatus> usage =
                service.findRoomUsageOn(LocalDate.of(2026, 10, 21));

        assertThat(usage).containsEntry(
                2L, BookingAvailabilityService.RoomTimelineStatus.RESERVED);
    }

    private Room room(Long id, Long roomTypeId, String status) {
        return Room.builder().id(id).roomTypeId(roomTypeId).status(status).build();
    }

    private BookingOrder order(Long id, String status, String checkIn, String checkOut) {
        return BookingOrder.builder()
                .id(id)
                .status(status)
                .expectedIn(date(checkIn))
                .expectedOut(date(checkOut))
                .build();
    }

    private BookingDetail detail(Long roomId) {
        return BookingDetail.builder().roomId(roomId).build();
    }

    private Date date(String value) {
        return Date.from(Instant.parse(value));
    }
}
