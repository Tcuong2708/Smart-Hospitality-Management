package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.LoyalCustomer;
import com.votricuong.mayhotel.documents.Role;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.dto.ProfileResponse;
import com.votricuong.mayhotel.dto.ProfileUpdateRequest;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.repositories.LoyalCustomerRepository;
import com.votricuong.mayhotel.repositories.RoleRepository;
import com.votricuong.mayhotel.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class ProfileService {
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final LoyalCustomerRepository loyalCustomerRepository;
    private final RoleRepository roleRepository;

    public ProfileService(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            LoyalCustomerRepository loyalCustomerRepository,
            RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.loyalCustomerRepository = loyalCustomerRepository;
        this.roleRepository = roleRepository;
    }

    public Optional<ProfileResponse> findProfile(Long userId) {
        return userRepository.findById(userId).map(user -> toResponse(user, findCustomer(userId)));
    }

    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Không tìm thấy tài khoản."));
        validate(request);

        if (request.getFullName() != null) user.setFullName(normalize(request.getFullName()));
        if (request.getPhone() != null) {
            String phone = normalize(request.getPhone());
            userRepository.findFirstByPhone(phone)
                    .filter(other -> !userId.equals(other.getId()))
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("Số điện thoại đã được sử dụng.");
                    });
            user.setPhone(phone);
        }
        if (request.getNationality() != null) user.setNationality(normalize(request.getNationality()));
        if (request.getAddress() != null) user.setAddress(normalize(request.getAddress()));

        User savedUser = userRepository.save(user);
        Customer customer = findCustomer(userId).orElseGet(() -> Customer.builder().userId(userId).build());
        if (request.getFullName() != null) customer.setFullName(normalize(request.getFullName()));
        if (request.getPhone() != null) customer.setPhone(normalize(request.getPhone()));
        if (request.getNationality() != null) customer.setNationality(normalize(request.getNationality()));
        if (request.getAddress() != null) customer.setAddress(normalize(request.getAddress()));
        customerRepository.save(customer);

        return toResponse(savedUser, Optional.of(customer));
    }

    private Optional<Customer> findCustomer(Long userId) {
        return customerRepository.findFirstByUserIdOrderByIdAsc(userId);
    }

    private ProfileResponse toResponse(User user, Optional<Customer> customerOpt) {
        Customer customer = customerOpt.orElse(null);
        Optional<LoyalCustomer> loyalty = customer == null
                ? Optional.empty()
                : loyalCustomerRepository.findByCustomerId(customer.getId());
        Optional<Role> role = user.getRoleId() == null
                ? Optional.empty()
                : roleRepository.findById(user.getRoleId());

        String fullName = firstNonBlank(user.getFullName(), customer == null ? null : customer.getFullName(), user.getUsername(), user.getEmail());
        String phone = firstNonBlank(user.getPhone(), customer == null ? null : customer.getPhone());
        String nationality = firstNonBlank(user.getNationality(), user.getQuocTich(), customer == null ? null : customer.getNationality(), customer == null ? null : customer.getQuocTich());
        Integer points = loyalty.map(LoyalCustomer::getTotalPoints)
                .orElse(customer == null ? null : customer.getPoints());
        String tier = loyalty.map(LoyalCustomer::getTier).orElseGet(
                () -> customer == null ? null : tierName(customer.getTierId()));

        return ProfileResponse.builder()
                .idTaiKhoan(user.getId())
                .tenDangNhap(user.getUsername())
                .hoTen(fullName)
                .soDienThoai(phone)
                .quocTich(nationality)
                .diaChi(firstNonBlank(user.getAddress(), customer == null ? null : customer.getAddress()))
                .roleID(user.getRoleId())
                .roleName(role.map(Role::getRoleName).orElse(user.getRole()))
                .displayTier(tier)
                .points(points)
                .build();
    }

    private void validate(ProfileUpdateRequest request) {
        if (request.getFullName() != null && !StringUtils.hasText(request.getFullName())) {
            throw new IllegalArgumentException("Họ và tên không được để trống.");
        }
        if (request.getPhone() != null && !request.getPhone().trim().matches("^[0-9+() .-]{7,20}$")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        }
        if (request.getFullName() != null && request.getFullName().trim().length() > 120) {
            throw new IllegalArgumentException("Họ và tên vượt quá độ dài cho phép.");
        }
        if (request.getNationality() != null && request.getNationality().trim().length() > 80) {
            throw new IllegalArgumentException("Quốc tịch vượt quá độ dài cho phép.");
        }
        if (request.getAddress() != null && request.getAddress().trim().length() > 255) {
            throw new IllegalArgumentException("Địa chỉ vượt quá độ dài cho phép.");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) return value;
        }

        return null;
    }

    private String tierName(Long tierId) {
        if (Long.valueOf(4L).equals(tierId)) return "Bạch Kim";
        if (Long.valueOf(3L).equals(tierId)) return "Vàng";
        if (Long.valueOf(2L).equals(tierId)) return "Bạc";
        return "Đồng";
    }

    public static class ProfileNotFoundException extends RuntimeException {
        public ProfileNotFoundException(String message) {
            super(message);
        }
    }
}
