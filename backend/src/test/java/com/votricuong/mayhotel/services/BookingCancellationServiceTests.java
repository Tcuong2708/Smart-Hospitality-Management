package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.BookingDetail;
import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingCancellationServiceTests {

    @Test
    void noShowForfeitsEntireInvoiceAndReleasesReservedRoom() {
        BookingOrderRepository bookingOrders = mock(BookingOrderRepository.class);
        BookingDetailRepository bookingDetails = mock(BookingDetailRepository.class);
        InvoiceRepository invoices = mock(InvoiceRepository.class);
        RoomRepository rooms = mock(RoomRepository.class);
        BookingCancellationService service = new BookingCancellationService(
                bookingOrders, bookingDetails, invoices, rooms);

        BookingOrder order = BookingOrder.builder().id(10L).status("Reserved").build();
        BookingDetail detail = BookingDetail.builder().id(20L).bookingId(10L).roomId(101L).build();
        Invoice invoice = Invoice.builder().id(30L).bookingId(10L).totalAmount(1_000_000.0)
                .isPaid(true).build();
        Room room = Room.builder().id(101L).status("Reserved").build();

        when(bookingOrders.findById(10L)).thenReturn(Optional.of(order));
        when(bookingDetails.findByBookingId(10L)).thenReturn(List.of(detail));
        when(bookingDetails.findAll()).thenReturn(List.of(detail));
        when(invoices.findByBookingId(10L)).thenReturn(List.of(invoice));
        when(rooms.findById(101L)).thenReturn(Optional.of(room));

        service.cancel(10L, BookingCancellationService.CancellationType.NO_SHOW, null);

        assertThat(order.getStatus()).isEqualTo("Hủy do khách không đến");
        assertThat(invoice.getPenaltyRate()).isEqualTo(100.0);
        assertThat(invoice.getPenaltyAmount()).isEqualTo(1_000_000.0);
        assertThat(invoice.getRefundAmount()).isZero();
        assertThat(invoice.getInvoiceStatus()).isEqualTo("Forfeited");
        assertThat(room.getStatus()).isEqualTo("Trống");
        verify(bookingOrders).save(order);
        verify(invoices).save(invoice);
        verify(rooms).save(room);
    }

    @Test
    void customerCancellationRefundsOnlyPaidDepositNotEntireInvoice() {
        BookingOrderRepository bookingOrders = mock(BookingOrderRepository.class);
        BookingDetailRepository bookingDetails = mock(BookingDetailRepository.class);
        InvoiceRepository invoices = mock(InvoiceRepository.class);
        RoomRepository rooms = mock(RoomRepository.class);
        BookingCancellationService service = new BookingCancellationService(
                bookingOrders, bookingDetails, invoices, rooms);

        BookingOrder order = BookingOrder.builder()
                .id(11L)
                .status(OnlineBookingPolicy.BOOKING_DEPOSIT_CONFIRMED)
                .expectedIn(new Date(System.currentTimeMillis() + 48L * 60 * 60 * 1000))
                .build();
        Invoice invoice = Invoice.builder()
                .id(31L)
                .bookingId(11L)
                .totalAmount(1_000_000.0)
                .depositPaidAmount(100_000L)
                .depositStatus(OnlineBookingPolicy.DEPOSIT_PAID)
                .isPaid(false)
                .build();

        when(bookingOrders.findById(11L)).thenReturn(Optional.of(order));
        when(bookingDetails.findByBookingId(11L)).thenReturn(List.of());
        when(invoices.findByBookingId(11L)).thenReturn(List.of(invoice));

        service.cancel(11L, BookingCancellationService.CancellationType.CUSTOMER, "Đổi kế hoạch");

        assertThat(invoice.getPenaltyRate()).isZero();
        assertThat(invoice.getPenaltyAmount()).isZero();
        assertThat(invoice.getRefundAmount()).isEqualTo(100_000.0);
    }
}
