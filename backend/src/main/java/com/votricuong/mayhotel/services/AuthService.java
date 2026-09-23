package com.votricuong.mayhotel.services;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.votricuong.mayhotel.documents.OtpCode;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.enums.Role;
import com.votricuong.mayhotel.repositories.OtpCodeRepository;
import com.votricuong.mayhotel.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final EmailService emailService;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final com.votricuong.mayhotel.repositories.CustomerRepository customerRepository;

    public AuthService(UserRepository userRepository, OtpCodeRepository otpCodeRepository, EmailService emailService, SequenceGeneratorService sequenceGeneratorService, com.votricuong.mayhotel.repositories.CustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.emailService = emailService;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.customerRepository = customerRepository;
    }

    /**
     * Hàm sinh mã OTP ngẫu nhiên gồm 6 chữ số.
     */
    private String generateOtpCode() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Hàm băm mật khẩu bảo mật (SHA-256).
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi thuật toán mã hóa", e);
        }
    }

    /**
     * Hàm xử lý đăng ký tài khoản (UC02).
     * Kiểm tra tính hợp lệ và gửi mã OTP qua Email.
     */
    public void processRegistration(User newUser, com.votricuong.mayhotel.documents.Customer newCustomer) throws Exception {
        if (userRepository.findByEmail(newUser.getEmail()).isPresent()) {
            throw new Exception("Email đã tồn tại trong hệ thống.");
        }
        if (com.votricuong.mayhotel.repositories.CustomerRepository.class != null) {
            // Check if phone exists (will be implemented via DI if needed)
        }

        // Tạo và lưu mã OTP
        String otp = generateOtpCode();
        
        OtpCode otpCode = OtpCode.builder()
                .identifier(newUser.getEmail())
                .code(otp)
                .pendingUser(newUser)
                .pendingCustomer(newCustomer)
                .createdAt(new Date())
                .build();
        otpCodeRepository.save(otpCode);

        // Gọi Email Service để gửi mã
        emailService.sendOtpEmail(newUser.getEmail(), otp);
    }

    /**
     * Hàm xác thực mã OTP. Nếu hợp lệ thì tiến hành lưu vào hệ thống.
     */
    public User verifyIdentity(String email, String otpCode) throws Exception {
        Optional<OtpCode> optionalOtp = otpCodeRepository.findByIdentifierAndCode(email, otpCode);
        
        if (optionalOtp.isEmpty()) {
            throw new Exception("Mã OTP không hợp lệ hoặc đã hết hạn.");
        }

        OtpCode validOtp = optionalOtp.get();
        User pendingUser = validOtp.getPendingUser();
        com.votricuong.mayhotel.documents.Customer pendingCustomer = validOtp.getPendingCustomer();
        
        // Hoàn thiện thông tin User và lưu CSDL
        pendingUser.setId(sequenceGeneratorService.generateSequence("users_sequence"));
        pendingUser.setPassword(hashPassword(pendingUser.getPassword()));
        // Note: Role is mapping to Customer (ID=3 in this project context, but we will leave null or 3)
        pendingUser.setRoleId(3L);
        pendingUser.setStatus("Hoạt động");

        User savedUser = userRepository.save(pendingUser);
        
        if (pendingCustomer != null) {
            pendingCustomer.setId(sequenceGeneratorService.generateSequence("customers_sequence"));
            pendingCustomer.setUserId(savedUser.getId());
            customerRepository.save(pendingCustomer);
        }
        
        // Xóa mã OTP sau khi dùng
        otpCodeRepository.delete(validOtp);
        
        return savedUser;
    }

    /**
     * Hàm xử lý đăng nhập cơ bản (Bằng Email hoặc SĐT).
     */
    public User processLogin(String usernameOrEmailOrPhone, String password) throws Exception {
        String hashedPass = hashPassword(password);
        
        Optional<User> userOpt = userRepository.findByEmail(usernameOrEmailOrPhone);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(usernameOrEmailOrPhone);
        }
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByPhone(usernameOrEmailOrPhone);
        }

        if (userOpt.isEmpty()) {
            throw new Exception("Tài khoản không tồn tại.");
        }
        
        String dbPassword = userOpt.get().getPassword();
        
        // Hỗ trợ cả mật khẩu đã băm (ví dụ: letan@email.com) lẫn chưa băm (admin)
        if (dbPassword == null || (!dbPassword.equals(hashedPass) && !dbPassword.equals(password))) {
            throw new Exception("Mật khẩu không chính xác.");
        }
        
        if (!"Hoạt động".equals(userOpt.get().getStatus())) {
            throw new Exception("Tài khoản đã bị vô hiệu hóa.");
        }

        return userOpt.get();
    }

    /**
     * Hàm xác thực Token của Google Firebase.
     * [NOTE]: Cần điền Firebase Key (google-services.json) thì code Firebase mới hoạt động thực tế.
     */
    public User verifyGoogleToken(String idToken) throws Exception {
        try {
            // Xác thực token bằng Firebase Admin SDK
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();

            // Kiểm tra email đã có trong hệ thống chưa
            Optional<User> existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent()) {
                return existingUser.get(); // Trả về user nếu đã tồn tại
            } else {
                // Tự động tạo tài khoản mới nếu chưa có
                User newUser = new User();
                newUser.setId(sequenceGeneratorService.generateSequence("users_sequence"));
                newUser.setEmail(email);
                newUser.setUsername(email);
                newUser.setRoleId(3L);
                newUser.setStatus("Hoạt động");
                
                // Mật khẩu ngẫu nhiên cho account Google (Hoặc để trống tùy quy tắc bảo mật)
                newUser.setPassword(hashPassword(generateOtpCode() + "google")); 
                
                User savedUser = userRepository.save(newUser);
                
                com.votricuong.mayhotel.documents.Customer newCustomer = new com.votricuong.mayhotel.documents.Customer();
                newCustomer.setId(sequenceGeneratorService.generateSequence("customers_sequence"));
                newCustomer.setUserId(savedUser.getId());
                newCustomer.setEmail(email);
                newCustomer.setFullName(name);
                customerRepository.save(newCustomer);
                
                return savedUser;
            }
        } catch (Exception e) {
            throw new Exception("Xác thực Google thất bại: " + e.getMessage());
        }
    }
}
