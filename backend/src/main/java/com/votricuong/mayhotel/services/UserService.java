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

    public UserService(UserRepository userRepository, SequenceGeneratorService sequenceGeneratorService, AuthService authService) {
        this.userRepository = userRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.authService = authService;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) throws Exception {
        if (userRepository.findByEmail(user.getEmail()).isPresent() || userRepository.findByUsername(user.getUsername()).isPresent()) {
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
        if (userDetails.getRoleId() != null) {
            user.setRoleId(userDetails.getRoleId());
        }
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(authService.hashPassword(userDetails.getPassword()));
        }
        return userRepository.save(user);
    }

    public User toggleStatus(Long id) throws Exception {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new Exception("Không tìm thấy người dùng.");
        }
        User user = userOpt.get();
        if ("Hoạt động".equals(user.getStatus())) {
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
        userRepository.deleteById(id);
    }
}
