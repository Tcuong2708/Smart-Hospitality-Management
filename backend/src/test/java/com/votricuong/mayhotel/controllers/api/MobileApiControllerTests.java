package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Invoice;
import com.votricuong.mayhotel.documents.Service;
import com.votricuong.mayhotel.documents.ServiceTicket;
import com.votricuong.mayhotel.dto.ApiResponse;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.ServiceRepository;
import com.votricuong.mayhotel.repositories.ServiceTicketRepository;
import com.votricuong.mayhotel.services.SequenceGeneratorService;
import com.votricuong.mayhotel.services.MobileTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MobileApiControllerTests {

    private BookingOrderRepository bookingOrderRepository;
    private InvoiceRepository invoiceRepository;
    private ServiceRepository serviceRepository;
    private ServiceTicketRepository serviceTicketRepository;
    private SequenceGeneratorService sequenceGeneratorService;
    private MobileTokenService mobileTokenService;
    private MobileApiController controller;

    @BeforeEach
    void setUp() {
        bookingOrderRepository = mock(BookingOrderRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        serviceRepository = mock(ServiceRepository.class);
        serviceTicketRepository = mock(ServiceTicketRepository.class);
        sequenceGeneratorService = mock(SequenceGeneratorService.class);
        mobileTokenService = mock(MobileTokenService.class);
        BookingDetailRepository bookingDetailRepository = mock(BookingDetailRepository.class);
        controller = new MobileApiController(
                null, serviceRepository, invoiceRepository, null, null, null, null,
                bookingOrderRepository, bookingDetailRepository,
                serviceTicketRepository, null, sequenceGeneratorService, null, null, null, mobileTokenService, null);
        when(bookingDetailRepository.findByBookingId(88L)).thenReturn(Collections.emptyList());
    }

    @Test
    void rejectsNonServiceStaffFromServiceTicketQueue() {
        when(mobileTokenService.verifyAuthorization("Bearer customer"))
                .thenReturn(new MobileTokenService.Claims(5L, 5L, "CUSTOMER", Long.MAX_VALUE));

        ResponseEntity<?> response = controller.getServiceTicketsForStaff("Bearer customer");

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void onlyCompletesVerificationAfterEveryGuestIsVerified() {
        BookingOrder booking = BookingOrder.builder()
                .id(88L)
                .guestCount(2)
                .verifiedGuestCount(0)
                .isFaceVerified(false)
                .build();
        when(bookingOrderRepository.findById(88L)).thenReturn(Optional.of(booking));

        ResponseEntity<ApiResponse<Map<String, Object>>> first = controller.checkinAIAndNFC(Map.of(
                "bookingId", 88,
                "guestIndex", 0,
                "cccdData", "CCCD-A"));

        assertThat(first.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(first.getBody().getData())
                .containsEntry("verifiedGuestCount", 1)
                .containsEntry("requiredGuestCount", 2)
                .containsEntry("allGuestsVerified", false);
        assertThat(booking.getIsFaceVerified()).isFalse();

        ResponseEntity<ApiResponse<Map<String, Object>>> second = controller.checkinAIAndNFC(Map.of(
                "bookingId", 88,
                "guestIndex", 1,
                "cccdData", "CCCD-B"));

        assertThat(second.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(second.getBody().getData())
                .containsEntry("verifiedGuestCount", 2)
                .containsEntry("requiredGuestCount", 2)
                .containsEntry("allGuestsVerified", true)
                .containsKey("nfcAccessCode");
        assertThat(booking.getIsFaceVerified()).isTrue();
    }

    @Test
    void rejectsUsingTheSameIdentityForAnotherGuest() {
        BookingOrder booking = BookingOrder.builder().id(88L).guestCount(2).build();
        when(bookingOrderRepository.findById(88L)).thenReturn(Optional.of(booking));

        controller.checkinAIAndNFC(Map.of(
                "bookingId", 88, "guestIndex", 0, "cccdData", "SAME-CCCD"));
        ResponseEntity<ApiResponse<Map<String, Object>>> duplicate = controller.checkinAIAndNFC(Map.of(
                "bookingId", 88, "guestIndex", 1, "cccdData", "SAME-CCCD"));

        assertThat(duplicate.getStatusCode().is4xxClientError()).isTrue();
        assertThat(duplicate.getBody().getMessage()).contains("đã được dùng");
        assertThat(booking.getVerifiedGuestCount()).isEqualTo(1);
        assertThat(booking.getIsFaceVerified()).isFalse();
    }

    @Test
    void storesMultipleServicesAndUpdatesOneInvoiceOnce() {
        BookingOrder booking = BookingOrder.builder()
                .id(88L)
                .status("Đang ở")
                .build();
        Invoice invoice = Invoice.builder()
                .id(58L)
                .bookingId(88L)
                .totalAmount(1_000_000.0)
                .build();
        Service motorbike = Service.builder().id(1L).name("Thuê xe máy").price(150_000.0).build();
        Service water = Service.builder().id(2L).name("Nước suối").price(10_000.0).build();

        when(bookingOrderRepository.findById(88L)).thenReturn(Optional.of(booking));
        when(invoiceRepository.findByBookingId(88L)).thenReturn(List.of(invoice));
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(motorbike));
        when(serviceRepository.findById(2L)).thenReturn(Optional.of(water));
        when(sequenceGeneratorService.generateSequence("service_tickets_sequence"))
                .thenReturn(101L, 102L);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = controller.orderServiceBatch(Map.of(
                "bookingId", 88,
                "items", List.of(
                        Map.of("serviceId", 1, "quantity", 1),
                        Map.of("serviceId", 2, "quantity", 3))));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData())
                .containsEntry("invoiceId", 58L)
                .containsEntry("serviceTotal", 180_000.0)
                .containsEntry("invoiceTotal", 1_180_000.0)
                .containsKeys("requestGroupId", "orderedAt");
        assertThat((List<?>) response.getBody().getData().get("items")).hasSize(2);
        assertThat(invoice.getTotalAmount()).isEqualTo(1_180_000.0);

        verify(serviceTicketRepository).saveAll(org.mockito.ArgumentMatchers.argThat(saved -> {
            List<ServiceTicket> tickets = new java.util.ArrayList<>();
            saved.forEach(tickets::add);
            return tickets.size() == 2
                    && tickets.get(0).getServiceId().equals(1L)
                    && tickets.get(1).getServiceId().equals(2L)
                    && tickets.get(0).getRequestGroupId() != null
                    && tickets.get(0).getRequestGroupId().equals(tickets.get(1).getRequestGroupId())
                    && tickets.get(0).getOrderedAt() != null
                    && tickets.get(0).getOrderedAt().equals(tickets.get(1).getOrderedAt());
        }));
        verify(invoiceRepository).save(invoice);
    }
}
