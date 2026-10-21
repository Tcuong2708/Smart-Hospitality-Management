package com.votricuong.mayhotel.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.votricuong.mayhotel.documents.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MobileTokenServiceTests {

    @Test
    void issuesAndVerifiesAHousekeeperToken() {
        MobileTokenService service = new MobileTokenService(new ObjectMapper(), "test-secret-at-least-32-characters-long");
        User housekeeper = User.builder().id(104L).roleId(4L).build();

        String token = service.issue(housekeeper);
        MobileTokenService.Claims claims = service.verifyAuthorization("Bearer " + token);

        assertThat(claims.userId()).isEqualTo(104L);
        assertThat(claims.roleId()).isEqualTo(4L);
        assertThat(claims.role()).isEqualTo("HOUSEKEEPER");
    }

    @Test
    void rejectsATamperedToken() {
        MobileTokenService service = new MobileTokenService(new ObjectMapper(), "test-secret-at-least-32-characters-long");
        String token = service.issue(User.builder().id(104L).roleId(4L).build());

        assertThatThrownBy(() -> service.verifyAuthorization("Bearer " + token + "x"))
                .isInstanceOf(SecurityException.class);
    }
}
