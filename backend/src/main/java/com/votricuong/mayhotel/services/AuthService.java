package com.votricuong.mayhotel.services;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.votricuong.mayhotel.documents.OtpCode;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.enums.Role;
import com.votricuong.mayhotel.repositories.OtpCodeRepository;
import com.votricuong.mayhotel.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Service
public class AuthService {

    private static final SecureRandom OTP_RANDOM = new SecureRandom();

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
        int otp = 100000 + OTP_RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Hàm băm mật khẩu bảo mật (SHA-256).
     */
    public String hashPassword(String password) {
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
        if (userRepository.findFirstByEmail(newUser.getEmail()).isPresent()) {
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
        // Tài khoản đăng ký trực tuyến luôn là khách hàng; lễ tân được quản lý riêng.
        pendingUser.setRoleId(5L);
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

    public String sendPasswordResetOtp(String keyword) throws Exception {
        if (keyword == null || keyword.isBlank()) {
            throw new Exception("Vui lòng nhập email hoặc số điện thoại.");
        }
        String identifier = keyword.trim();
        Optional<User> userOptional = userRepository.findFirstByEmail(identifier);
        if (userOptional.isEmpty()) userOptional = userRepository.findFirstByPhone(identifier);
        User user = userOptional.orElseThrow(() -> new Exception("Không tìm thấy tài khoản phù hợp."));
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new Exception("Tài khoản chưa có email để nhận mã xác thực.");
        }

        String otp = generateOtpCode();
        OtpCode resetCode = OtpCode.builder()
                .identifier(identifier)
                .code(otp)
                .pendingUser(user)
                .pendingCustomer(null)
                .createdAt(new Date())
                .build();
        otpCodeRepository.findByIdentifier(identifier).ifPresent(otpCodeRepository::delete);
        otpCodeRepository.save(resetCode);
        try {
            emailService.sendOtpEmail(user.getEmail(), otp);
        } catch (RuntimeException ex) {
            otpCodeRepository.delete(resetCode);
            throw new Exception("Không thể gửi email xác thực lúc này. Vui lòng thử lại sau.");
        }
        return identifier;
    }

    public void resetPassword(String identifier, String otp, String newPassword) throws Exception {
        if (identifier == null || identifier.isBlank()) {
            throw new Exception("Thiếu thông tin tài khoản cần khôi phục.");
        }
        if (otp == null || !otp.matches("\\d{6}")) {
            throw new Exception("Mã OTP phải gồm 6 chữ số.");
        }
        String validatedPassword;
        try {
            validatedPassword = com.votricuong.mayhotel.utils.InputValidator.password(newPassword);
        } catch (IllegalArgumentException ex) {
            throw new Exception(ex.getMessage());
        }

        OtpCode validOtp = otpCodeRepository.findByIdentifierAndCode(identifier.trim(), otp)
                .filter(item -> item.getPendingUser() != null && item.getPendingCustomer() == null)
                .orElseThrow(() -> new Exception("Mã OTP không hợp lệ hoặc đã hết hạn."));
        User user = userRepository.findById(validOtp.getPendingUser().getId())
                .orElseThrow(() -> new Exception("Tài khoản không còn tồn tại."));
        user.setPassword(hashPassword(validatedPassword));
        userRepository.save(user);
        otpCodeRepository.delete(validOtp);
    }

    /**
     * Hàm xử lý đăng nhập cơ bản (Bằng Email hoặc SĐT).
     */
    public User processLogin(String usernameOrEmailOrPhone, String password) throws Exception {
        String hashedPass = hashPassword(password);
        
        Optional<User> userOpt = userRepository.findFirstByEmail(usernameOrEmailOrPhone);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findFirstByUsername(usernameOrEmailOrPhone);
        }
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findFirstByPhone(usernameOrEmailOrPhone);
        }

        if (userOpt.isEmpty()) {
            throw new Exception("Tài khoản không tồn tại.");
        }
        
        String dbPassword = userOpt.get().getPassword();
        
        // Hỗ trợ cả mật khẩu đã băm (ví dụ: letan@email.com) lẫn chưa băm (admin)
        if (dbPassword == null || (!dbPassword.equals(hashedPass) && !dbPassword.equals(password))) {
            throw new Exception("Mật khẩu không chính xác.");
        }
        
        String status = userOpt.get().getStatus();
        if (status == null || (!"Hoạt động".equalsIgnoreCase(status) && !"Active".equalsIgnoreCase(status))) {
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
            initializeFirebaseIfNeeded();
            // Xác thực token bằng Firebase Admin SDK
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();

            // Kiểm tra email đã có trong hệ thống chưa
            Optional<User> existingUser = userRepository.findFirstByEmail(email);
            if (existingUser.isPresent()) {
                return existingUser.get(); // Trả về user nếu đã tồn tại
            } else {
                // Tự động tạo tài khoản mới nếu chưa có
                User newUser = new User();
                newUser.setId(sequenceGeneratorService.generateSequence("users_sequence"));
                newUser.setEmail(email);
                newUser.setUsername(email);
                newUser.setRoleId(5L);
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

    private synchronized void initializeFirebaseIfNeeded() throws Exception {
        if (!FirebaseApp.getApps().isEmpty()) return;
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .build();
        FirebaseApp.initializeApp(options);
    }
}
