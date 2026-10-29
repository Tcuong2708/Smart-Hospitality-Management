package com.votricuong.mayhotel.config;

import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.repositories.UserRepository;
import com.votricuong.mayhotel.services.SequenceGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * Creates the mobile service-staff account once, without storing a new plain
 * text password in source control. It copies the existing password hash from
 * the configured trusted staff account, so the deployment keeps the common
 * staff password already managed by the owner.
 */
@Component
public class ServiceStaffAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ServiceStaffAccountInitializer.class);

    private final UserRepository userRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final boolean enabled;
    private final String username;
    private final String email;
    private final String passwordSourceUsername;

    public ServiceStaffAccountInitializer(
            UserRepository userRepository,
            SequenceGeneratorService sequenceGeneratorService,
            @Value("${mayhotel.bootstrap.service-staff.enabled:true}") boolean enabled,
            @Value("${mayhotel.bootstrap.service-staff.username:dichvu}") String username,
            @Value("${mayhotel.bootstrap.service-staff.email:dichvu@mayhotel.com}") String email,
            @Value("${mayhotel.bootstrap.service-staff.password-source:letan}") String passwordSourceUsername) {
        this.userRepository = userRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.enabled = enabled;
        this.username = username.trim();
        this.email = email.trim();
        this.passwordSourceUsername = passwordSourceUsername.trim();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || username.isBlank() || userRepository.findFirstByUsername(username).isPresent()) {
            return;
        }
        if (userRepository.findFirstByEmail(email).isPresent()) {
            log.warn("Không tạo tài khoản dịch vụ {} vì email {} đã tồn tại", username, email);
            return;
        }

        User passwordSource = userRepository.findFirstByUsername(passwordSourceUsername)
                .orElseGet(() -> userRepository.findAll().stream()
                        .filter(user -> user.getPassword() != null && !user.getPassword().isBlank())
                        .filter(user -> user.getRoleId() != null && user.getRoleId() != 5L)
                        .min(Comparator.comparing(User::getId, Comparator.nullsLast(Long::compareTo)))
                        .orElse(null));
        if (passwordSource == null) {
            log.warn("Chưa có tài khoản nhân viên nguồn; bỏ qua khởi tạo tài khoản dịch vụ {}", username);
            return;
        }

        User serviceStaff = User.builder()
                .id(sequenceGeneratorService.generateSequence("users_sequence"))
                .username(username)
                .password(passwordSource.getPassword())
                .email(email)
                .fullName("Nhân viên dịch vụ")
                .nationality("Việt Nam")
                .status("Hoạt động")
                .roleId(6L)
                .build();
        userRepository.save(serviceStaff);
        log.info("Đã khởi tạo tài khoản mobile nhân viên dịch vụ: {}", username);
    }
}
