package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.services.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String showLoginForm(Model model, HttpSession session) {
        setPageTitle(model, "Đăng nhập");
        setExtraCSS(model, "view/Account/login :: extra_css");
        setExtraJS(model, "view/Account/login :: extra_js");
        
        // Lấy thông báo lỗi từ Interceptor (nếu có)
        String sessionError = (String) session.getAttribute("error");
        if (sessionError != null) {
            model.addAttribute("error", sessionError);
            session.removeAttribute("error");
        }
        
        return render(model, "view/Account/login");
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
            session.setAttribute("user", user); // Lưu phiên làm việc

            if (user.getRoleId() != null) {
                if (user.getRoleId() == 1L) {
                    return "redirect:/admin/users";
                } else if (user.getRoleId() == 2L) {
                    return "redirect:/manager/rooms";
                } else if (user.getRoleId() == 3L || user.getRoleId() == 4L) {
                    return "redirect:/receptionist/room-map";
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
        setExtraCSS(model, "view/Account/register :: extra_css");
        setExtraJS(model, "view/Account/register :: extra_js");
        return render(model, "view/Account/register");
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
        return render(model, "view/Account/verify_register_otp");
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

    /**
     * Hàm xử lý Đăng nhập bằng Google Firebase.
     * Được Frontend (Ajax) gọi sau khi lấy được Token từ SDK Google.
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
                } else if (user.getRoleId() == 3L || user.getRoleId() == 4L) {
                    return "{\"success\": true, \"redirect\": \"/receptionist/room-map\"}";
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
}
