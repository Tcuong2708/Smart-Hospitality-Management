package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final AuthService authService; // to use hashPassword
    private final com.votricuong.mayhotel.repositories.CustomerRepository customerRepository;
    private final com.votricuong.mayhotel.repositories.StaffRepository staffRepository;

    public UserService(UserRepository userRepository, SequenceGeneratorService sequenceGeneratorService, AuthService authService,
                       com.votricuong.mayhotel.repositories.CustomerRepository customerRepository,
                       com.votricuong.mayhotel.repositories.StaffRepository staffRepository) {
        this.userRepository = userRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.authService = authService;
        this.customerRepository = customerRepository;
        this.staffRepository = staffRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) throws Exception {
        if (user == null) throw new IllegalArgumentException("Dữ liệu tài khoản không được để trống");
        user.setUsername(com.votricuong.mayhotel.utils.InputValidator.username(user.getUsername()));
        user.setEmail(com.votricuong.mayhotel.utils.InputValidator.email(user.getEmail(), true));
        user.setPassword(com.votricuong.mayhotel.utils.InputValidator.adminUserPassword(user.getPassword()));
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            user.setFullName(com.votricuong.mayhotel.utils.InputValidator.requiredText(
                    user.getFullName(), "Họ và tên", 100));
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            user.setPhone(com.votricuong.mayhotel.utils.InputValidator.phone(user.getPhone()));
            if (userRepository.findFirstByPhone(user.getPhone()).isPresent()) {
                throw new IllegalArgumentException("Số điện thoại đã thuộc tài khoản khác");
            }
        }
        user.setAddress(com.votricuong.mayhotel.utils.InputValidator.optionalText(
                user.getAddress(), "Địa chỉ", 300));
        user.setNationality(com.votricuong.mayhotel.utils.InputValidator.optionalText(
                user.getNationality(), "Quốc tịch", 100));
        if (user.getRoleId() == null || user.getRoleId() < 1 || user.getRoleId() > 7) {
            throw new IllegalArgumentException("Vai trò tài khoản không hợp lệ");
        }
        if (userRepository.findFirstByEmail(user.getEmail()).isPresent() || userRepository.findFirstByUsername(user.getUsername()).isPresent()) {
            throw new Exception("Tài khoản hoặc email đã tồn tại.");
        }
        user.setId(sequenceGeneratorService.generateSequence("users_sequence"));
        user.setPassword(authService.hashPassword(user.getPassword()));
        if (user.getStatus() == null || user.getStatus().isEmpty()) {
            user.setStatus("Hoạt động");
        }
        return userRepository.save(user);
    }

    public User updateUser(Long id, User userDetails) throws Exception {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new Exception("Không tìm thấy người dùng.");
        }
        User user = userOpt.get();
        if (userDetails.getFullName() != null) {
            user.setFullName(com.votricuong.mayhotel.utils.InputValidator.requiredText(
                    userDetails.getFullName(), "Họ và tên", 100));
        }
        if (userDetails.getEmail() != null) {
            String email = com.votricuong.mayhotel.utils.InputValidator.email(userDetails.getEmail(), true);
            if (userRepository.findFirstByEmail(email).filter(item -> !item.getId().equals(id)).isPresent()) {
                throw new IllegalArgumentException("Email đã thuộc tài khoản khác");
            }
            user.setEmail(email);
        }
        if (userDetails.getPhone() != null && !userDetails.getPhone().isBlank()) {
            String phone = com.votricuong.mayhotel.utils.InputValidator.phone(userDetails.getPhone());
            if (userRepository.findFirstByPhone(phone).filter(item -> !item.getId().equals(id)).isPresent()) {
                throw new IllegalArgumentException("Số điện thoại đã thuộc tài khoản khác");
            }
            user.setPhone(phone);
        }
        if (userDetails.getAddress() != null) {
            user.setAddress(com.votricuong.mayhotel.utils.InputValidator.optionalText(
                    userDetails.getAddress(), "Địa chỉ", 300));
        }
        if (userDetails.getNationality() != null) {
            user.setNationality(com.votricuong.mayhotel.utils.InputValidator.optionalText(
                    userDetails.getNationality(), "Quốc tịch", 100));
        }
        if (userDetails.getStatus() != null) {
            String status = userDetails.getStatus().trim();
            if (!java.util.Set.of("Hoạt động", "Bị khóa", "Active", "Locked").contains(status)) {
                throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ");
            }
            user.setStatus(status);
        }
        if (userDetails.getRoleId() != null) {
            if (userDetails.getRoleId() < 1 || userDetails.getRoleId() > 7) {
                throw new IllegalArgumentException("Vai trò tài khoản không hợp lệ");
            }
            user.setRoleId(userDetails.getRoleId());
        }
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(authService.hashPassword(
                    com.votricuong.mayhotel.utils.InputValidator.adminUserPassword(userDetails.getPassword())));
        }
        return userRepository.save(user);
    }

    public User toggleStatus(Long id) throws Exception {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new Exception("Không tìm thấy người dùng.");
        }
        User user = userOpt.get();
        String currentStatus = user.getStatus();
        if ("Hoạt động".equalsIgnoreCase(currentStatus) || "Active".equalsIgnoreCase(currentStatus)) {
            user.setStatus("Bị khóa");
        } else {
            user.setStatus("Hoạt động");
        }
        return userRepository.save(user);
    }

    public void deleteUser(Long id) throws Exception {
        if (!userRepository.existsById(id)) {
            throw new Exception("Không tìm thấy người dùng.");
        }
        if (!customerRepository.findByUserId(id).isEmpty() || staffRepository.existsByUserId(id)) {
            throw new Exception("Không thể xóa tài khoản đang liên kết với hồ sơ khách hàng hoặc nhân viên; hãy khóa tài khoản");
        }
        userRepository.deleteById(id);
    }
}
