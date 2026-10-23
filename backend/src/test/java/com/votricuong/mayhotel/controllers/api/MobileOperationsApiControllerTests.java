package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.dto.ApiResponse;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.LoyalCustomerRepository;
import com.votricuong.mayhotel.repositories.LoyaltyPolicyRepository;
import com.votricuong.mayhotel.repositories.ReviewRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.services.BookingAvailabilityService;
import com.votricuong.mayhotel.services.BookingCancellationService;
import com.votricuong.mayhotel.services.LoyaltyService;
import com.votricuong.mayhotel.services.MobileTokenService;
import com.votricuong.mayhotel.services.NoShowPredictionService;
import com.votricuong.mayhotel.services.RoomStatusService;
import com.votricuong.mayhotel.services.SequenceGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MobileOperationsApiControllerTests {
    private MobileTokenService tokenService;
    private BookingOrderRepository bookingOrders;
    private BookingDetailRepository bookingDetails;
    private CustomerRepository customers;
    private InvoiceRepository invoices;
    private RoomRepository rooms;
    private BookingAvailabilityService availability;
    private RoomStatusService roomStatusService;
    private MobileOperationsApiController controller;

    @BeforeEach
    void setUp() {
        tokenService = mock(MobileTokenService.class);
        bookingOrders = mock(BookingOrderRepository.class);
        bookingDetails = mock(BookingDetailRepository.class);
        customers = mock(CustomerRepository.class);
        invoices = mock(InvoiceRepository.class);
        rooms = mock(RoomRepository.class);
        availability = mock(BookingAvailabilityService.class);
        roomStatusService = mock(RoomStatusService.class);
        controller = new MobileOperationsApiController(
                tokenService,
                bookingOrders,
                bookingDetails,
                customers,
                invoices,
                mock(ReviewRepository.class),
                rooms,
                mock(RoomTypeRepository.class),
                mock(LoyalCustomerRepository.class),
                mock(LoyaltyPolicyRepository.class),
                mock(BookingCancellationService.class),
                availability,
                roomStatusService,
                mock(SequenceGeneratorService.class),
                mock(LoyaltyService.class),
                mock(NoShowPredictionService.class));
        when(tokenService.verifyAuthorization("Bearer receptionist"))
                .thenReturn(new MobileTokenService.Claims(30L, 3L, "RECEPTIONIST", Long.MAX_VALUE));
    }

    @Test
    void assignRoomUsesAtomicRoomStatusWorkflow() {
        BookingOrder order = BookingOrder.builder()
                .id(10L)
                .customerId(20L)
                .status("Reserved")
                .guestCount(1)
                .verifiedGuestCount(1)
                .isFaceVerified(true)
                .build();
        BookingDetail detail = BookingDetail.builder().id(11L).bookingId(10L).roomId(100L).build();
        Room bookedRoom = Room.builder().id(100L).roomTypeId(5L).status("RESERVED").build();
        Room targetRoom = Room.builder().id(101L).roomTypeId(5L).status("VACANT").build();
        Invoice invoice = Invoice.builder().id(12L).bookingId(10L).payMethod("PAY_AT_HOTEL").build();

        when(bookingOrders.findById(10L)).thenReturn(Optional.of(order));
        when(bookingDetails.findByBookingId(10L)).thenReturn(List.of(detail));
        when(rooms.findById(100L)).thenReturn(Optional.of(bookedRoom));
        when(rooms.findById(101L)).thenReturn(Optional.of(targetRoom));
        when(invoices.findByBookingId(10L)).thenReturn(List.of(invoice));
        when(availability.findAvailableRoomsExcludingBooking(5L, null, null, 10L))
                .thenReturn(List.of(targetRoom));
        when(roomStatusService.markOccupiedForStay(eq(101L), eq(30L), anyString()))
                .thenReturn(targetRoom);

        ResponseEntity<?> response = controller.assignRoom(
                10L, "Bearer receptionist", Map.of("roomId", 101));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(order.getStatus()).isEqualTo("Đã nhận phòng");
        assertThat(detail.getRoomId()).isEqualTo(101L);
        verify(roomStatusService).markOccupiedForStay(eq(101L), eq(30L), anyString());
        verify(rooms, never()).save(targetRoom);
    }

    @Test
    void retryingCheckoutDoesNotAddTheSameSurchargeTwice() {
        BookingOrder order = BookingOrder.builder().id(10L).status("Đã nhận phòng").build();
        Invoice invoice = Invoice.builder()
                .id(12L)
                .bookingId(10L)
                .totalAmount(1_100_000D)
                .surcharge(100_000D)
                .isPaid(true)
                .invoiceStatus("Paid")
                .build();
        when(bookingOrders.findById(10L)).thenReturn(Optional.of(order));
        when(invoices.findByBookingId(10L)).thenReturn(List.of(invoice));
        when(bookingDetails.findByBookingId(10L)).thenReturn(List.of());

        ResponseEntity<?> response = controller.checkout(
                10L, "Bearer receptionist", Map.of("surcharge", 100_000D));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(invoice.getTotalAmount()).isEqualTo(1_100_000D);
        assertThat(invoice.getSurcharge()).isEqualTo(100_000D);
        assertThat(invoice.getIsPaid()).isTrue();
        assertThat(order.getStatus()).isEqualTo("Đã trả phòng");
        assertThat(response.getBody()).isInstanceOf(ApiResponse.class);
    }
}
