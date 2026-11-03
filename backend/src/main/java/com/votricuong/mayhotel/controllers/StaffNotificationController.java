package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.services.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/account/staff-notifications")
public class StaffNotificationController {
    private final NotificationService notifications;

    public StaffNotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @PostMapping("/read")
    public String read(@RequestParam Long id, @RequestParam(defaultValue = "/") String redirect, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) notifications.markUserNotificationAsRead(id, user.getId());
        return "redirect:" + safeRedirect(redirect);
    }

    @PostMapping("/read-all")
    public String readAll(@RequestParam(defaultValue = "/") String redirect, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) notifications.markAllUserNotificationsAsRead(user.getId());
        return "redirect:" + safeRedirect(redirect);
    }

    private String safeRedirect(String value) {
        return value != null && value.startsWith("/") && !value.startsWith("//") ? value : "/";
    }
}
