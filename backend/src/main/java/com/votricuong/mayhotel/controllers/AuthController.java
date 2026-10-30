package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.services.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/account")
public class AuthController extends BaseController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Hàm hiển thị trang đăng nhập.
     */
    @GetMapping("/login")
    public String showLoginForm(Model model, HttpSession session, @RequestParam(value = "error", required = false) String urlError) {
        setPageTitle(model, "Đăng nhập");
        setExtraCSS(model, "view/account/login :: extra_css");
        setExtraJS(model, "view/account/login :: extra_js");
        
        if ("timeout".equals(urlError)) {
            model.addAttribute("error", "Phiên đăng nhập của bạn đã hết hạn!");
        } else if ("auth".equals(urlError)) {
            model.addAttribute("error", "Bạn cần đăng nhập trước khi thực hiện!");
        }

        // Lấy thông báo lỗi từ Interceptor (nếu có)
        String sessionError = (String) session.getAttribute("error");
        if (sessionError != null) {
            model.addAttribute("error", sessionError);
            session.removeAttribute("error");
        }
        
        return "view/account/login";
    }

    /**
     * Hàm xử lý đăng nhập truyền thống.
     */
    @PostMapping("/login")
    public String handleLogin(@RequestParam("username") String username,
                              @RequestParam("password") String password,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = authService.processLogin(username, password);
            
            if (user.getRoleId() != null && (user.getRoleId() == 4L || user.getRoleId() == 6L)) {
                throw new Exception("Tài khoản của bạn (NV Buồng phòng / NV Dịch vụ) chỉ được phép đăng nhập trên thiết bị Mobile!");
            }
            
            session.setAttribute("user", user); // Lưu phiên làm việc

            if (user.getRoleId() != null) {
                if (user.getRoleId() == 1L) {
                    return "redirect:/admin/users";
                } else if (user.getRoleId() == 2L) {
                    return "redirect:/manager/rooms";
                } else if (user.getRoleId() == 3L) {
                    return "redirect:/receptionist/room-map";
                } else if (user.getRoleId() == 7L) {
                    return "redirect:/admin/statistical";
                }
            }
            return "redirect:/"; // Trở về trang chủ
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/account/login";
        }
    }

    /**
     * Hàm hiển thị trang đăng ký.
     */
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        setPageTitle(model, "Đăng ký tài khoản");
        setExtraCSS(model, "view/account/register :: extra_css");
        setExtraJS(model, "view/account/register :: extra_js");
        return "view/account/register";
    }

    /**
     * Hàm xử lý yêu cầu đăng ký tài khoản (UC02).
     */
    @PostMapping("/register")
    public String handleRegistration(@RequestParam("fullName") String fullName,
                                     @RequestParam("phone") String phone,
                                     @RequestParam("email") String email,
                                     @RequestParam("password") String password,
                                     @RequestParam("confirmPassword") String confirmPassword,
                                     RedirectAttributes redirectAttributes) {
        
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp.");
            return "redirect:/account/register";
        }

        try {
            User newUser = User.builder()
                    .username(email)
                    .email(email)
                    .password(password)
                    .build();
                    
            com.votricuong.mayhotel.documents.Customer newCustomer = com.votricuong.mayhotel.documents.Customer.builder()
                    .fullName(fullName)
                    .phone(phone)
                    .email(email)
                    .build();
            
            authService.processRegistration(newUser, newCustomer); // Sinh OTP và gửi Email
            
            // Chuyển hướng sang trang nhập mã OTP, truyền email qua param
            redirectAttributes.addFlashAttribute("success", "Mã xác thực OTP đã được gửi đến email của bạn.");
            return "redirect:/account/verify-otp?email=" + email;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/account/register";
        }
    }

    /**
     * Hàm hiển thị trang nhập mã OTP.
     */
    @GetMapping("/verify-otp")
    public String showVerifyOtpForm(@RequestParam("email") String email, Model model) {
        setPageTitle(model, "Xác thực OTP");
        model.addAttribute("email", email);
        return "view/account/verify_register_otp";
    }

    /**
     * Hàm xử lý xác thực mã OTP.
     */
    @PostMapping("/verify-otp")
    public String handleVerifyOtp(@RequestParam("email") String email,
                                  @RequestParam("otp") String otp,
                                  RedirectAttributes redirectAttributes) {
        try {
            authService.verifyIdentity(email, otp);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/account/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/account/verify-otp?email=" + email;
        }
    }

    @GetMapping("/find_account")
    public String showFindAccount() {
        return "view/account/find_account";
    }

    @PostMapping("/find_account")
    public String sendPasswordResetCode(@RequestParam("keyword") String keyword,
                                        RedirectAttributes redirectAttributes) {
        try {
            String identifier = authService.sendPasswordResetOtp(keyword);
            redirectAttributes.addFlashAttribute("success", "Mã xác thực đã được gửi đến email của tài khoản.");
            return "redirect:/account/verify_otp?keyword="
                    + UriUtils.encode(identifier, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/account/find_account";
        }
    }

    @GetMapping("/verify_otp")
    public String showPasswordReset(@RequestParam("keyword") String keyword, Model model) {
        model.addAttribute("keyword", keyword);
        return "view/account/verify_otp";
    }

    @PostMapping("/verify_otp")
    public String resetPassword(@RequestParam("keyword") String keyword,
                                @RequestParam("otp") String otp,
                                @RequestParam("newpassword") String newPassword,
                                RedirectAttributes redirectAttributes) {
        try {
            authService.resetPassword(keyword, otp, newPassword);
            redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công. Vui lòng đăng nhập.");
            return "redirect:/account/login";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/account/verify_otp?keyword="
                    + UriUtils.encode(keyword, StandardCharsets.UTF_8);
        }
    }

    /**
     * Xử lý đăng nhập Google OpenID Connect dùng chung Web và Mobile.
     * Frontend gửi Google ID token để backend kiểm tra chữ ký, audience và email xác minh.
     */
    @PostMapping("/google-login")
    @ResponseBody // Trả về JSON cho Ajax xử lý thay vì điều hướng trang
    public String handleGoogleLogin(@RequestParam("idToken") String idToken, HttpSession session) {
        try {
            User user = authService.verifyGoogleToken(idToken);
            session.setAttribute("user", user); // Lưu phiên làm việc
            
            if (user.getRoleId() != null) {
                if (user.getRoleId() == 1L) {
                    return "{\"success\": true, \"redirect\": \"/admin/users\"}";
                } else if (user.getRoleId() == 2L) {
                    return "{\"success\": true, \"redirect\": \"/manager/rooms\"}";
                } else if (user.getRoleId() == 3L) {
                    return "{\"success\": true, \"redirect\": \"/receptionist/room-map\"}";
                } else if (user.getRoleId() == 4L || user.getRoleId() == 6L) {
                    return "{\"success\": false, \"message\": \"Tài khoản này chỉ được phép đăng nhập trên Mobile\"}";
                } else if (user.getRoleId() == 7L) {
                    return "{\"success\": true, \"redirect\": \"/admin/statistical\"}";
                }
            }
            return "{\"success\": true, \"redirect\": \"/\"}";
        } catch (Exception e) {
            return "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * Hàm xử lý đăng xuất.
     */
    @GetMapping("/logout")
    public String handleLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    /**
     * Layout quản trị dùng form POST để tránh trình duyệt/prefetch vô tình đăng xuất.
     * Giữ GET để tương thích các menu cũ, đồng thời hỗ trợ POST cho layout mới.
     */
    @PostMapping("/logout")
    public String handleLogoutPost(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
