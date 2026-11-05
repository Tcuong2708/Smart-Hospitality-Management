package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.BookingOrder;
import com.votricuong.mayhotel.documents.Invoice;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OnlineBookingPolicyTests {

    @Test
    void allAgeGroupsCountTowardCapacityButOnlyAdultsRequireIdentity() {
        BookingOrder order = BookingOrder.builder()
                .guestCount(5)
                .adultCount(2)
                .childCount(2)
                .infantCount(1)
                .build();

        assertThat(OnlineBookingPolicy.totalGuests(2, 2, 1)).isEqualTo(5);
        assertThat(OnlineBookingPolicy.requiredIdentityVerifications(order)).isEqualTo(2);
    }

    @Test
    void depositIsTenPercentRoundedToWholeVnd() {
        assertThat(OnlineBookingPolicy.depositAmount(1_234_567D)).isEqualTo(123_457L);
        assertThat(OnlineBookingPolicy.depositAmount(0D)).isZero();
    }

    @Test
    void atLeastOneAdultIsRequired() {
        assertThatThrownBy(() -> OnlineBookingPolicy.totalGuests(0, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void vatIsCalculatedAfterDiscountsAndDepositUsesTaxInclusiveTotal() {
        OnlineBookingPolicy.PriceBreakdown price = OnlineBookingPolicy.priceBreakdown(
                500_000D, 0D, 0D, 0D);
        assertThat(price.subtotal()).isEqualTo(500_000L);
        assertThat(price.taxAmount()).isEqualTo(50_000L);
        assertThat(price.totalAmount()).isEqualTo(550_000L);
        assertThat(price.depositAmount()).isEqualTo(55_000L);

        OnlineBookingPolicy.PriceBreakdown discounted = OnlineBookingPolicy.priceBreakdown(
                500_000D, 100_000D, 50_000D, 50_000D);
        assertThat(discounted.taxableAmount()).isEqualTo(500_000L);
        assertThat(discounted.taxAmount()).isEqualTo(50_000L);
        assertThat(discounted.totalAmount()).isEqualTo(550_000L);
    }

    @Test
    void payAtHotelBookingIsConfirmedForCheckInWithoutOnlinePayment() {
        Invoice invoice = Invoice.builder()
                .payMethod("PAY_AT_HOTEL")
                .invoiceStatus("Pay at Hotel")
                .depositStatus("NOT_REQUIRED")
                .depositPaidAmount(0L)
                .build();

        assertThat(OnlineBookingPolicy.isConfirmedForCheckIn(invoice)).isTrue();
    }

    @Test
    void pendingVnPayBookingWaitsForSuccessfulCallback() {
        Invoice pending = Invoice.builder()
                .payMethod("VNPAY_DEPOSIT")
                .invoiceStatus("Awaiting Payment")
                .depositStatus("PENDING")
                .depositPaidAmount(0L)
                .build();
        Invoice paid = Invoice.builder()
                .payMethod("VNPAY_DEPOSIT")
                .invoiceStatus("Deposit Paid")
                .depositStatus("PAID")
                .depositPaidAmount(50_000L)
                .build();

        assertThat(OnlineBookingPolicy.isConfirmedForCheckIn(pending)).isFalse();
        assertThat(OnlineBookingPolicy.isConfirmedForCheckIn(paid)).isTrue();
    }
}
