package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.dto.ApiResponse;
import com.votricuong.mayhotel.repositories.BackupHistoryRepository;
import com.votricuong.mayhotel.repositories.BookingOrderRepository;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.InvoiceRepository;
import com.votricuong.mayhotel.repositories.LoyaltyPolicyRepository;
import com.votricuong.mayhotel.repositories.PromotionRepository;
import com.votricuong.mayhotel.repositories.ReviewRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.repositories.ServiceRepository;
import com.votricuong.mayhotel.repositories.ServiceTicketRepository;
import com.votricuong.mayhotel.repositories.StaffRepository;
import com.votricuong.mayhotel.repositories.UserRepository;
import com.votricuong.mayhotel.services.CustomerService;
import com.votricuong.mayhotel.services.MobileTokenService;
import com.votricuong.mayhotel.services.RoomService;
import com.votricuong.mayhotel.services.RoomTypeService;
import com.votricuong.mayhotel.services.SequenceGeneratorService;
import com.votricuong.mayhotel.services.StaffService;
import com.votricuong.mayhotel.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MobileManagementApiControllerTests {
    private MobileTokenService tokenService;
    private UserRepository userRepository;
    private MobileManagementApiController controller;

    @BeforeEach
    void setUp() {
        tokenService = mock(MobileTokenService.class);
        userRepository = mock(UserRepository.class);
        controller = new MobileManagementApiController(
                tokenService,
                mock(RoomRepository.class),
                mock(RoomTypeRepository.class),
                mock(ServiceRepository.class),
                mock(CustomerRepository.class),
                mock(ReviewRepository.class),
                mock(PromotionRepository.class),
                mock(LoyaltyPolicyRepository.class),
                mock(StaffRepository.class),
                userRepository,
                mock(InvoiceRepository.class),
                mock(BookingOrderRepository.class),
                mock(BackupHistoryRepository.class),
                mock(ServiceTicketRepository.class),
                mock(RoomService.class),
                mock(RoomTypeService.class),
                mock(CustomerService.class),
                mock(StaffService.class),
                mock(UserService.class),
                mock(SequenceGeneratorService.class));
    }

    @Test
    void managerCannotReadAdminUsers() {
        when(tokenService.verifyAuthorization("Bearer manager"))
                .thenReturn(new MobileTokenService.Claims(20L, 2L, "MANAGER", Long.MAX_VALUE));

        ResponseEntity<?> response = controller.users("Bearer manager");

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void adminUserResponseNeverContainsPassword() {
        when(tokenService.verifyAuthorization("Bearer admin"))
                .thenReturn(new MobileTokenService.Claims(1L, 1L, "ADMIN", Long.MAX_VALUE));
        when(userRepository.findAll()).thenReturn(List.of(User.builder()
                .id(9L)
                .username("manager01")
                .password("hashed-secret")
                .email("manager@mayhotel.vn")
                .roleId(2L)
                .status("Hoạt động")
                .build()));

        ResponseEntity<?> response = controller.users("Bearer admin");
        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = (List<Map<String, Object>>) body.getData();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(users).hasSize(1);
        assertThat(users.get(0)).containsEntry("roleCode", "MANAGER").doesNotContainKey("password");
    }
}
