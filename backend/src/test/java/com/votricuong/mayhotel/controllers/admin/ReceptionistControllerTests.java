package com.votricuong.mayhotel.controllers.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.services.LoyaltyService;
import com.votricuong.mayhotel.services.BookingAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReceptionistControllerTests {

    private InvoiceRepository invoiceRepository;
    private RoomRepository roomRepository;
    private RoomTypeRepository roomTypeRepository;
    private BookingDetailRepository bookingDetailRepository;
    private BookingOrderRepository bookingOrderRepository;
    private BookingAvailabilityService bookingAvailabilityService;
    private ReceptionistController controller;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        roomRepository = mock(RoomRepository.class);
        roomTypeRepository = mock(RoomTypeRepository.class);
        bookingDetailRepository = mock(BookingDetailRepository.class);
        bookingOrderRepository = mock(BookingOrderRepository.class);
        bookingAvailabilityService = mock(BookingAvailabilityService.class);
        when(bookingAvailabilityService.findOccupiedRoomIdsOn(any())).thenReturn(Set.of());
        controller = new ReceptionistController(
                invoiceRepository,
                roomRepository,
                roomTypeRepository,
                bookingDetailRepository,
                mock(LoyaltyService.class),
                bookingOrderRepository,
                bookingAvailabilityService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkInViewKeepsTheRoomAssignedInBookingDetailsSelectable() throws Exception {
        Invoice invoice = Invoice.builder()
                .id(18L)
                .bookingId(22L)
                .invoiceStatus("Reserved")
                .build();
        BookingDetail detail = BookingDetail.builder()
                .id(22L)
                .bookingId(22L)
                .roomId(59L)
                .build();
        Room reservedRoom = Room.builder()
                .id(59L)
                .name("P102")
                .roomTypeId(1L)
                .status("Reserved")
                .viewDirection("Hướng Thành Phố")
                .build();
        Room vacantRoom = Room.builder()
                .id(60L)
                .name("P103")
                .roomTypeId(1L)
                .status("Trống")
                .build();
        Room occupiedRoom = Room.builder()
                .id(64L)
                .name("P201")
                .roomTypeId(2L)
                .status("Đang ở")
                .build();

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));
        when(bookingDetailRepository.findByBookingId(22L)).thenReturn(List.of(detail));
        when(roomRepository.findById(59L)).thenReturn(Optional.of(reservedRoom));
        when(roomRepository.findAll()).thenReturn(List.of(reservedRoom, vacantRoom, occupiedRoom));
        when(roomTypeRepository.findAll()).thenReturn(List.of(
                RoomType.builder().id(1L).name("Phòng Tiêu Chuẩn").build(),
                RoomType.builder().id(2L).name("Phòng Đôi").build()));
        when(bookingOrderRepository.findById(22L)).thenReturn(Optional.of(
                BookingOrder.builder().id(22L).isFaceVerified(true).build()));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.checkInView(null, model);

        assertThat(view).isEqualTo("layout/layout");
        List<ReceptionistController.InvoiceDTO> invoices =
                (List<ReceptionistController.InvoiceDTO>) model.get("invoices");
        assertThat(invoices).singleElement().satisfies(item -> {
            assertThat(item.getBookingId()).isEqualTo(22L);
            assertThat(item.getMaPhong()).isEqualTo(59L);
            assertThat(item.getTenPhong()).isEqualTo("P102");
            assertThat(item.getMaLoaiPhong()).isEqualTo(1L);
            assertThat(item.isVerified()).isTrue();
        });
        assertThat(new ObjectMapper().writeValueAsString(invoices.get(0)))
                .contains("\"bookingId\":22", "\"isVerified\":true");

        List<ReceptionistController.CheckInRoomDTO> selectableRooms =
                (List<ReceptionistController.CheckInRoomDTO>) model.get("emptyRooms");
        assertThat(selectableRooms).extracting(ReceptionistController.CheckInRoomDTO::getId)
                .containsExactly(59L, 60L);
        assertThat(selectableRooms).first().satisfies(room -> {
            assertThat(room.getLabel()).isEqualTo("102");
            assertThat(room.getRoomTypeId()).isEqualTo(1L);
            assertThat(room.getType()).isEqualTo("Standard");
            assertThat(room.getView()).isEqualTo("Hướng Thành Phố");
        });
        assertThat(reservedRoom.getView()).isEqualTo("Hướng Thành Phố");
    }

    @Test
    void executeCheckInRejectsARoomFromAnotherRoomType() {
        Invoice invoice = Invoice.builder()
                .id(18L)
                .bookingId(22L)
                .invoiceStatus("Reserved")
                .build();
        BookingDetail detail = BookingDetail.builder()
                .id(22L)
                .bookingId(22L)
                .roomId(59L)
                .build();
        Room bookedStandardRoom = Room.builder()
                .id(59L)
                .roomTypeId(1L)
                .status("Reserved")
                .build();
        Room selectedFamilyRoom = Room.builder()
                .id(117L)
                .roomTypeId(4L)
                .status("Trống")
                .build();

        when(invoiceRepository.findById(18L)).thenReturn(Optional.of(invoice));
        when(bookingDetailRepository.findByBookingId(22L)).thenReturn(List.of(detail));
        when(roomRepository.findById(117L)).thenReturn(Optional.of(selectedFamilyRoom));
        when(roomRepository.findById(59L)).thenReturn(Optional.of(bookedStandardRoom));
        when(bookingOrderRepository.findById(22L)).thenReturn(Optional.of(
                BookingOrder.builder().id(22L).isFaceVerified(true).build()));

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String redirect = controller.executeCheckIn(18L, 117L, false, redirectAttributes);

        assertThat(redirect).isEqualTo("redirect:/receptionist/check-in");
        assertThat(redirectAttributes.getFlashAttributes().get("error"))
                .isEqualTo("Chỉ được check-in phòng đúng loại khách đã đặt.");
        verify(roomRepository, never()).save(any(Room.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void executeCheckInRejectsAnUnverifiedBooking() {
        Invoice invoice = Invoice.builder()
                .id(18L)
                .bookingId(22L)
                .invoiceStatus("Reserved")
                .build();
        Room selectedRoom = Room.builder()
                .id(59L)
                .roomTypeId(1L)
                .status("Reserved")
                .build();

        when(invoiceRepository.findById(18L)).thenReturn(Optional.of(invoice));
        when(roomRepository.findById(59L)).thenReturn(Optional.of(selectedRoom));
        when(bookingOrderRepository.findById(22L)).thenReturn(Optional.of(
                BookingOrder.builder().id(22L).isFaceVerified(false).build()));

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String redirect = controller.executeCheckIn(18L, 59L, false, redirectAttributes);

        assertThat(redirect).isEqualTo("redirect:/receptionist/check-in");
        assertThat(redirectAttributes.getFlashAttributes().get("error"))
                .isEqualTo("Khách chưa xác thực danh tính qua Mobile hoặc nhập tay ngoại lệ.");
        verify(roomRepository, never()).save(any(Room.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkInViewIncludesAnOccupiedRoomAsDisabledInformation() throws Exception {
        Invoice invoice = Invoice.builder()
                .id(18L)
                .bookingId(22L)
                .invoiceStatus("Reserved")
                .build();
        BookingDetail detail = BookingDetail.builder()
                .bookingId(22L)
                .roomId(59L)
                .build();
        Room occupiedRoom = Room.builder()
                .id(59L)
                .name("P102")
                .roomTypeId(1L)
                .status("Occupied")
                .build();

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));
        when(bookingDetailRepository.findByBookingId(22L)).thenReturn(List.of(detail));
        when(roomRepository.findById(59L)).thenReturn(Optional.of(occupiedRoom));
        when(roomRepository.findAll()).thenReturn(List.of(occupiedRoom));
        when(roomTypeRepository.findAll()).thenReturn(List.of(
                RoomType.builder().id(1L).name("Phòng Tiêu Chuẩn").build()));
        when(bookingOrderRepository.findById(22L)).thenReturn(Optional.of(
                BookingOrder.builder().id(22L).isFaceVerified(true).build()));
        when(bookingAvailabilityService.findOccupiedRoomIdsOn(any())).thenReturn(Set.of(59L));

        ExtendedModelMap model = new ExtendedModelMap();
        controller.checkInView(null, model);

        List<ReceptionistController.CheckInRoomDTO> rooms =
                (List<ReceptionistController.CheckInRoomDTO>) model.get("emptyRooms");
        assertThat(rooms).singleElement().satisfies(room -> {
            assertThat(room.getId()).isEqualTo(59L);
            assertThat(room.isOccupied()).isTrue();
        });
    }

    @Test
    void executeCheckInRejectsARoomWithAnActiveStay() {
        Invoice invoice = Invoice.builder()
                .id(18L)
                .bookingId(22L)
                .invoiceStatus("Reserved")
                .build();
        Room room = Room.builder()
                .id(59L)
                .roomTypeId(1L)
                .status("Vacant")
                .build();
        when(invoiceRepository.findById(18L)).thenReturn(Optional.of(invoice));
        when(roomRepository.findById(59L)).thenReturn(Optional.of(room));
        when(bookingOrderRepository.findById(22L)).thenReturn(Optional.of(
                BookingOrder.builder().id(22L).isFaceVerified(true).build()));
        when(bookingAvailabilityService.isRoomOccupiedOn(any(), any())).thenReturn(true);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        controller.executeCheckIn(18L, 59L, false, redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("error"))
                .isEqualTo("Phòng này đang có khách lưu trú, không thể gán cho phiếu check-in.");
        verify(roomRepository, never()).save(any(Room.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }
}
