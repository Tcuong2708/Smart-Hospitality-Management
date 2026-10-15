package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReceptionApiControllerTests {

    private BookingOrderRepository bookingOrderRepository;
    private CustomerRepository customerRepository;
    private ReceptionApiController controller;

    @BeforeEach
    void setUp() {
        bookingOrderRepository = mock(BookingOrderRepository.class);
        customerRepository = mock(CustomerRepository.class);
        controller = new ReceptionApiController();
        ReflectionTestUtils.setField(controller, "roomRepository", mock(RoomRepository.class));
        ReflectionTestUtils.setField(controller, "bookingOrderRepository", bookingOrderRepository);
        ReflectionTestUtils.setField(controller, "bookingDetailRepository", mock(BookingDetailRepository.class));
        ReflectionTestUtils.setField(controller, "customerRepository", customerRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void manualVerificationPersistsCustomerAndVerificationAudit() {
        BookingOrder booking = BookingOrder.builder().id(22L).customerId(7L).build();
        Customer customer = Customer.builder().id(7L).build();
        when(bookingOrderRepository.findById(22L)).thenReturn(Optional.of(booking));
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));

        ResponseEntity<?> response = controller.saveManualVerification(22L, Map.of(
                "reason", "FOREIGN_GUEST",
                "fullName", "JOHN DOE",
                "documentNumber", "P1234567",
                "nationality", "USA",
                "dateOfBirth", "1990-05-20"));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("verificationMethod", "MANUAL_EXCEPTION")
                .containsEntry("isFaceVerified", true);
        assertThat(customer.getFullName()).isEqualTo("JOHN DOE");
        assertThat(customer.getCccd()).isEqualTo("P1234567");
        assertThat(customer.getNationality()).isEqualTo("USA");
        assertThat(customer.getDob()).isNotNull();
        assertThat(booking.getIsFaceVerified()).isTrue();
        assertThat(booking.getIdentityVerificationMethod()).isEqualTo("MANUAL_EXCEPTION");
        assertThat(booking.getIdentityVerificationReason()).isEqualTo("FOREIGN_GUEST");
        assertThat(booking.getIdentityVerifiedAt()).isNotNull();
        verify(customerRepository).save(customer);
        verify(bookingOrderRepository).save(booking);
    }

    @Test
    void manualVerificationRejectsAnUnknownExceptionReason() {
        BookingOrder booking = BookingOrder.builder().id(22L).customerId(7L).build();
        when(bookingOrderRepository.findById(22L)).thenReturn(Optional.of(booking));

        ResponseEntity<?> response = controller.saveManualVerification(22L, Map.of(
                "reason", "NORMAL_CHECK_IN",
                "fullName", "JOHN DOE",
                "documentNumber", "P1234567",
                "nationality", "USA",
                "dateOfBirth", "1990-05-20"));

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        verify(customerRepository, never()).save(any(Customer.class));
        verify(bookingOrderRepository, never()).save(any(BookingOrder.class));
    }
}
