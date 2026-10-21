package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.Role;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.dto.ProfileResponse;
import com.votricuong.mayhotel.dto.ProfileUpdateRequest;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.LoyalCustomerRepository;
import com.votricuong.mayhotel.repositories.RoleRepository;
import com.votricuong.mayhotel.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileServiceTests {
    private UserRepository userRepository;
    private CustomerRepository customerRepository;
    private LoyalCustomerRepository loyalCustomerRepository;
    private RoleRepository roleRepository;
    private SequenceGeneratorService sequenceGeneratorService;
    private ProfileService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        customerRepository = mock(CustomerRepository.class);
        loyalCustomerRepository = mock(LoyalCustomerRepository.class);
        roleRepository = mock(RoleRepository.class);
        sequenceGeneratorService = mock(SequenceGeneratorService.class);
        service = new ProfileService(
                userRepository,
                customerRepository,
                loyalCustomerRepository,
                roleRepository,
                sequenceGeneratorService);
    }

    @Test
    void returnsTheSameProfileDataUsedByWebAndMobile() {
        User user = User.builder()
                .id(5L)
                .username("guest")
                .fullName("Nguyễn Văn A")
                .email("guest@mayhotel.com")
                .phone("0901234567")
                .address("TP.HCM")
                .nationality("Việt Nam")
                .gender("Nam")
                .roleId(5L)
                .build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(customerRepository.findFirstByUserIdOrderByIdAsc(5L)).thenReturn(Optional.empty());
        when(roleRepository.findById(5L)).thenReturn(Optional.of(
                Role.builder().id(5L).roleName("CUSTOMER").build()));

        ProfileResponse profile = service.findProfile(5L).orElseThrow();

        assertThat(profile.getHoTen()).isEqualTo("Nguyễn Văn A");
        assertThat(profile.getEmail()).isEqualTo("guest@mayhotel.com");
        assertThat(profile.getSoDienThoai()).isEqualTo("0901234567");
        assertThat(profile.getDiaChi()).isEqualTo("TP.HCM");
        assertThat(profile.getQuocTich()).isEqualTo("Việt Nam");
        assertThat(profile.getGender()).isEqualTo("Nam");
    }

    @Test
    void UpdatesUserAndCustomerFromTheSharedProfileRequest() {
        User user = User.builder().id(5L).username("guest").roleId(5L).build();
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullName("Nguyễn Văn B");
        request.setEmail("NEW@MAYHOTEL.COM");
        request.setPhone("0912345678");
        request.setAddress("Đà Nẵng");
        request.setNationality("Việt Nam");
        request.setGender("Nữ");

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.findFirstByEmail("new@mayhotel.com")).thenReturn(Optional.empty());
        when(userRepository.findFirstByPhone("0912345678")).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);
        when(customerRepository.findFirstByUserIdOrderByIdAsc(5L)).thenReturn(Optional.empty());
        when(sequenceGeneratorService.generateSequence("customers_sequence")).thenReturn(25L);
        when(roleRepository.findById(5L)).thenReturn(Optional.empty());
        when(loyalCustomerRepository.findByCustomerId(25L)).thenReturn(Optional.empty());

        ProfileResponse profile = service.updateProfile(5L, request);

        assertThat(user.getEmail()).isEqualTo("new@mayhotel.com");
        assertThat(profile.getHoTen()).isEqualTo("Nguyễn Văn B");
        assertThat(profile.getGender()).isEqualTo("Nữ");

        ArgumentCaptor<Customer> customer = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customer.capture());
        assertThat(customer.getValue().getId()).isEqualTo(25L);
        assertThat(customer.getValue().getUserId()).isEqualTo(5L);
        assertThat(customer.getValue().getEmail()).isEqualTo("new@mayhotel.com");
        assertThat(customer.getValue().getPhone()).isEqualTo("0912345678");
    }
}
