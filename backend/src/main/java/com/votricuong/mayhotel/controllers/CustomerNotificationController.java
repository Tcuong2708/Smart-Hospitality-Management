package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.Customer;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.services.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/customer/notifications")
public class CustomerNotificationController extends BaseController {
    private final NotificationService notificationService;
    private final CustomerRepository customerRepository;

    public CustomerNotificationController(NotificationService notificationService,
                                          CustomerRepository customerRepository) {
        this.notificationService = notificationService;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public String page(Model model, HttpSession session) {
        Customer customer = currentCustomer(session);
        if (customer == null) return "redirect:/account/login";
        setPageTitle(model, "Thông báo");
        model.addAttribute("notifications", notificationService.getByCustomer(customer.getId()));
        model.addAttribute("unreadCount", notificationService.countUnread(customer.getId()));
        return "view/customer/notifications/index";
    }

    @GetMapping("/api/count")
    @ResponseBody
    public Map<String, Object> unreadCount(HttpSession session) {
        Customer customer = currentCustomer(session);
        return Map.of("count", customer == null ? 0 : notificationService.countUnread(customer.getId()));
    }

    @PostMapping("/api/{id}/read")
    @ResponseBody
    public ResponseEntity<?> markRead(@PathVariable Long id, HttpSession session) {
        Customer customer = currentCustomer(session);
        if (customer == null) return ResponseEntity.status(401).body(Map.of("success", false));
        if (!notificationService.markAsRead(id, customer.getId())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("success", true,
                "count", notificationService.countUnread(customer.getId())));
    }

    @PostMapping("/api/read-all")
    @ResponseBody
    public ResponseEntity<?> markAllRead(HttpSession session) {
        Customer customer = currentCustomer(session);
        if (customer == null) return ResponseEntity.status(401).body(Map.of("success", false));
        notificationService.markAllAsRead(customer.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }

    private Customer currentCustomer(HttpSession session) {
        Object value = session.getAttribute("user");
        if (!(value instanceof User user) || user.getId() == null) return null;
        return customerRepository.findFirstByUserIdOrderByIdAsc(user.getId()).orElse(null);
    }
}
