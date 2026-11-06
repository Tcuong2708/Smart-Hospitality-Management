package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.services.UserService;
import com.votricuong.mayhotel.services.PermissionService;
import com.votricuong.mayhotel.services.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/admin/users")
public class UserAdminController extends BaseController {

    private final UserService userService;
    private final PermissionService permissionService;
    private final NotificationService notificationService;

    public UserAdminController(UserService userService, PermissionService permissionService,
                               NotificationService notificationService) {
        this.userService = userService;
        this.permissionService = permissionService;
        this.notificationService = notificationService;
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
    public String create(@ModelAttribute User user, RedirectAttributes ra, HttpSession session) {
        try {
            User actor = (User) session.getAttribute("user");
            if (user.getRoleId() != null && user.getRoleId() == 8L
                    && (actor == null || actor.getRoleId() == null || actor.getRoleId() != 8L)) {
                throw new IllegalArgumentException("Chỉ Super Admin được tạo tài khoản Super Admin");
            }
            userService.createUser(user);
            ra.addFlashAttribute("success", "Thêm người dùng thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/edit/{id}")
    public String editView(@PathVariable Long id, Model model, RedirectAttributes ra, HttpSession session) {
        User user = userService.getUserById(id).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "Không tìm thấy người dùng!");
            return "redirect:/admin/users";
        }
        User actor = (User) session.getAttribute("user");
        if (!canManage(actor, user)) {
            ra.addFlashAttribute("error", "Bạn không được chỉnh sửa tài khoản này");
            return "redirect:/admin/users";
        }
        setPageTitle(model, "Chỉnh sửa Người dùng");
        model.addAttribute("user", user);
        return render(model, "view/admin/user/edit");
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id, @ModelAttribute User userDetails, RedirectAttributes ra, HttpSession session) {
        try {
            User actor = (User) session.getAttribute("user");
            User target = userService.getUserById(id).orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại"));
            if (!canManage(actor, target)) throw new IllegalArgumentException("Bạn không được chỉnh sửa tài khoản này");
            if (userDetails.getRoleId() != null && userDetails.getRoleId() == 8L && actor.getRoleId() != 8L)
                throw new IllegalArgumentException("Chỉ Super Admin được cấp vai trò Super Admin");
            userService.updateUser(id, userDetails);
            ra.addFlashAttribute("success", "Cập nhật người dùng thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @RequestMapping(value = "/toggle-status/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String toggleStatus(@PathVariable Long id, RedirectAttributes ra, HttpSession session) {
        try {
            User actor = (User) session.getAttribute("user");
            User target = userService.getUserById(id).orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại"));
            if (!canManage(actor, target)) throw new IllegalArgumentException("Bạn không được khóa tài khoản này");
            userService.toggleStatus(id);
            ra.addFlashAttribute("success", "Đã thay đổi trạng thái!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @RequestMapping(value = "/delete/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String delete(@PathVariable Long id, RedirectAttributes ra, HttpSession session) {
        try {
            User actor = (User) session.getAttribute("user");
            User target = userService.getUserById(id).orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại"));
            if (!canManage(actor, target)) throw new IllegalArgumentException("Bạn không được xóa tài khoản này");
            userService.deleteUser(id);
            ra.addFlashAttribute("success", "Xóa người dùng thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/permissions")
    public String permissions(@PathVariable Long id, Model model, HttpSession session, RedirectAttributes redirect) {
        User target = userService.getUserById(id).orElse(null);
        User actor = (User) session.getAttribute("user");
        if (target == null || actor == null) return "redirect:/admin/users";
        if (!canManage(actor, target) || Objects.equals(target.getRoleId(), 5L)) {
            redirect.addFlashAttribute("error", "Bạn không được thay đổi quyền tài khoản này");
            return "redirect:/admin/users";
        }
        setPageTitle(model, "Quyền ngoại lệ - " + target.getUsername());
        model.addAttribute("targetUser", target);
        model.addAttribute("permissions", permissionService.getPermissions());
        model.addAttribute("rolePermissions", permissionService.getRolePermissionCodes(target.getRoleId()));
        model.addAttribute("overrides", permissionService.getUserOverrides(target.getId()));
        return render(model, "view/admin/user/permissions");
    }

    @PostMapping("/{id}/permissions")
    public String updatePermission(@PathVariable Long id, @RequestParam String permission,
                                   @RequestParam String effect, HttpSession session, RedirectAttributes redirect) {
        User target = userService.getUserById(id).orElse(null);
        User actor = (User) session.getAttribute("user");
        if (target == null || actor == null || !canManage(actor, target)
                || Objects.equals(target.getRoleId(), 5L)) {
            redirect.addFlashAttribute("error", "Bạn không được thay đổi quyền tài khoản này");
            return "redirect:/admin/users";
        }
        try {
            permissionService.setUserOverride(actor.getId(), target, permission, effect);
            notificationService.createForUser(target.getId(), "PERMISSION_CHANGED", "Quyền cá nhân đã thay đổi",
                    "Quản trị viên " + actor.getUsername() + " đã cập nhật quyền truy cập của bạn.", "/");
            redirect.addFlashAttribute("success", "Đã cập nhật quyền ngoại lệ");
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users/" + id + "/permissions";
    }

    private boolean canManage(User actor, User target) {
        if (actor == null || target == null || actor.getRoleId() == null) return false;
        if (target.getRoleId() != null && target.getRoleId() == 8L) return false;
        return actor.getRoleId() != null && (actor.getRoleId() == 8L
                || (actor.getRoleId() == 1L && target.getRoleId() != null && target.getRoleId() >= 2L));
    }
}
