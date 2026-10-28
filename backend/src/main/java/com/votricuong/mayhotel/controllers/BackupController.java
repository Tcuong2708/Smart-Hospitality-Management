package com.votricuong.mayhotel.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpSession;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.services.BackupService;
import java.util.Map;

@Controller
@RequestMapping("/admin/backup")
public class BackupController extends BaseController {
    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping
    public String index(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/account/login?error=forbidden";
        setPageTitle(model, "Sao lưu và Phục hồi Hệ thống");
        model.addAttribute("backups", backupService.history());
        model.addAttribute("backupSettings", backupService.settings());
        model.addAttribute("backupEnabled", backupService.isBackupEnabled());
        model.addAttribute("restoreEnabled", backupService.isRestoreEnabled());
        return render(model, "view/admin/backup/index");
    }

    @PostMapping("/create")
    public String create(HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        User user = (User) session.getAttribute("user");
        if (!isAdmin(session)) return "redirect:/account/login?error=forbidden";
        try {
            var result = backupService.create(user.getId());
            redirect.addFlashAttribute(result.getStatus().equals("SUCCESS") ? "success" : "error",
                    result.getStatus().equals("SUCCESS") ? "Sao lưu thành công." : result.getMessage());
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/backup";
    }

    @PostMapping("/restore")
    public String restore(@RequestParam String fileName, HttpSession session,
                          org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        User user = (User) session.getAttribute("user");
        if (!isAdmin(session)) return "redirect:/account/login?error=forbidden";
        try {
            backupService.restore(fileName, user == null ? null : user.getId());
            redirect.addFlashAttribute("success", "Đã phục hồi bản sao lưu.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/backup";
    }

    @PostMapping("/schedule")
    public String schedule(@RequestParam(defaultValue = "false") boolean enabled,
                           @RequestParam String frequency, @RequestParam Integer intervalHours,
                           HttpSession session,
                           org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        if (!isAdmin(session)) return "redirect:/account/login?error=forbidden";
        try {
            backupService.saveSettings(enabled, frequency, intervalHours);
            redirect.addFlashAttribute("success", "Đã lưu lịch sao lưu.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/backup";
    }

    private boolean isAdmin(HttpSession session) {
        Object value = session.getAttribute("user");
        return value instanceof User user && Long.valueOf(1L).equals(user.getRoleId());
    }
}
