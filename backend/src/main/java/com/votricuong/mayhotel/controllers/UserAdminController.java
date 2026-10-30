package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class UserAdminController extends BaseController {

    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String index(Model model) {
        List<User> users = userService.getAllUsers();
        setPageTitle(model, "Quản lý Người dùng");
        setExtraCSS(model, "view/admin/user/index :: extra_css");
        setExtraJS(model, "view/admin/user/index :: extra_js");
        model.addAttribute("users", users);
        return render(model, "view/admin/user/index");
    }

    @GetMapping("/create")
    public String createView(Model model) {
        setPageTitle(model, "Thêm Người dùng");
        model.addAttribute("user", new User());
        return render(model, "view/admin/user/create");
    }

    @PostMapping("/create")
    public String create(@ModelAttribute User user, RedirectAttributes ra) {
        try {
            userService.createUser(user);
            ra.addFlashAttribute("success", "Thêm người dùng thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/edit/{id}")
    public String editView(@PathVariable Long id, Model model, RedirectAttributes ra) {
        User user = userService.getUserById(id).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "Không tìm thấy người dùng!");
            return "redirect:/admin/users";
        }
        setPageTitle(model, "Chỉnh sửa Người dùng");
        model.addAttribute("user", user);
        return render(model, "view/admin/user/edit");
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id, @ModelAttribute User userDetails, RedirectAttributes ra) {
        try {
            userService.updateUser(id, userDetails);
            ra.addFlashAttribute("success", "Cập nhật người dùng thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @RequestMapping(value = "/toggle-status/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String toggleStatus(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.toggleStatus(id);
            ra.addFlashAttribute("success", "Đã thay đổi trạng thái!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @RequestMapping(value = "/delete/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.deleteUser(id);
            ra.addFlashAttribute("success", "Xóa người dùng thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
