package com.votricuong.mayhotel.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InputValidatorTests {

    @Test
    void normalizesValidCustomerInput() {
        assertThat(InputValidator.requiredText("  Nguyễn   Văn A  ", "Họ tên", 100))
                .isEqualTo("Nguyễn Văn A");
        assertThat(InputValidator.phone("(+84) 912-345-678")).isEqualTo("+84912345678");
        assertThat(InputValidator.email(" TEST@Example.COM ", true)).isEqualTo("test@example.com");
    }

    @Test
    void rejectsInvalidBookingInput() {
        assertThatThrownBy(() -> InputValidator.stay(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("quá khứ");
        assertThatThrownBy(() -> InputValidator.stay(LocalDate.now().plusDays(2), LocalDate.now().plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sau ngày nhận");
        assertThatThrownBy(() -> InputValidator.phone("abc123"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("9-15");
    }

    @Test
    void rejectsInvalidCrudNumbersAndEnums() {
        assertThatThrownBy(() -> InputValidator.positiveMoney(-1.0, "Giá"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InputValidator.range(0, "Sức chứa", 1, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InputValidator.oneOf("crypto", "Thanh toán", Set.of("COD", "ONLINE")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
