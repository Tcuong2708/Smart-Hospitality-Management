package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.dto.ProfileResponse;
import com.votricuong.mayhotel.dto.ProfileUpdateRequest;
import com.votricuong.mayhotel.services.ProfileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class AccountProfileController extends BaseController {

    private final ProfileService profileService;

    public AccountProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public String showProfile(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User sessionUser = getSessionUser(session);
        if (sessionUser == null) {
            redirectAttributes.addAttribute("error", "timeout");
            return "redirect:/account/login";
        }

        ProfileResponse profile = profileService.findProfile(sessionUser.getId())
                .orElse(null);
        if (profile == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin tài khoản.");
            return "redirect:/";
        }

        setPageTitle(model, "Hồ sơ tài khoản");
        setExtraCSS(model, "view/account/profile :: extra_css");
        model.addAttribute("user", profile);
        return "view/account/profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(
            @RequestParam("hoTen") String fullName,
            @RequestParam(value = "soDienThoai", required = false) String phone,
            @RequestParam(value = "quocTich", required = false) String nationality,
            @RequestParam(value = "diaChi", required = false) String address,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User sessionUser = getSessionUser(session);
        if (sessionUser == null) {
            redirectAttributes.addAttribute("error", "timeout");
            return "redirect:/account/login";
        }

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullName(fullName);
        request.setPhone(phone);
        request.setNationality(nationality);
        request.setAddress(address);

        try {
            ProfileResponse updatedProfile = profileService.updateProfile(sessionUser.getId(), request);
            sessionUser.setFullName(updatedProfile.getHoTen());
            sessionUser.setPhone(updatedProfile.getSoDienThoai());
            sessionUser.setNationality(updatedProfile.getQuocTich());
            sessionUser.setAddress(updatedProfile.getDiaChi());
            session.setAttribute("user", sessionUser);
            redirectAttributes.addFlashAttribute("success", "Cập nhật hồ sơ thành công.");
        } catch (IllegalArgumentException | ProfileService.ProfileNotFoundException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }

        return "redirect:/account/profile";
    }

    private User getSessionUser(HttpSession session) {
        Object value = session.getAttribute("user");
        return value instanceof User ? (User) value : null;
    }
}
