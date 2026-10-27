package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.BookingOrder;
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
}
