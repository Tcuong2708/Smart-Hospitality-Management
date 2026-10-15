package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.dto.ApiResponse;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MobileApiControllerTests {

    private BookingOrderRepository bookingOrderRepository;
    private MobileApiController controller;

    @BeforeEach
    void setUp() {
        bookingOrderRepository = mock(BookingOrderRepository.class);
        BookingDetailRepository bookingDetailRepository = mock(BookingDetailRepository.class);
        controller = new MobileApiController(
                null, null, null, null, null, null, null,
                bookingOrderRepository, bookingDetailRepository,
                null, null, null, null, null);
        when(bookingDetailRepository.findByBookingId(88L)).thenReturn(Collections.emptyList());
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
}
