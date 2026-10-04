package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import com.votricuong.mayhotel.repositories.CustomerRepository;
import com.votricuong.mayhotel.documents.Customer;

@RestController
@RequestMapping("/api/users")
public class UserApiController {

    private final UserService userService;
    private final CustomerRepository customerRepository;

    public UserApiController(UserService userService, CustomerRepository customerRepository) {
        this.userService = userService;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        
        List<Map<String, Object>> mappedUsers = users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            
            // Map the exact fields expected by the original frontend mock data
            map.put("id", user.getId());
            
            map.put("tenDangNhap", user.getUsername() != null ? user.getUsername() : "");
            
            String hoTen = "";
            String quocTich = "";
            
            List<Customer> customers = customerRepository.findByUserId(user.getId());
            if (customers != null && !customers.isEmpty()) {
                Customer c = customers.get(0);
                if (c.getFullName() != null) hoTen = c.getFullName();
                if (c.getQuocTich() != null) quocTich = c.getQuocTich();
            }
            
            if (hoTen.isEmpty() && user.getEmail() != null) {
                hoTen = user.getEmail();
            }
            if (quocTich.isEmpty()) {
                quocTich = "Việt Nam";
            }
            
            map.put("hoTen", hoTen);
            map.put("quocTich", quocTich);
            
            map.put("roleID", user.getRoleId() != null ? user.getRoleId() : 3L);
            
            int trangThai = 0;
            if (user.getStatus() != null && (user.getStatus().contains("Hoạt động") || user.getStatus().contains("Ho") || user.getStatus().equalsIgnoreCase("Active") || user.getStatus().equals("1"))) {
                trangThai = 1;
            }
            map.put("trangThai", trangThai);
            
            return map;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(mappedUsers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> userOpt = userService.getUserById(id);
        return userOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            return ResponseEntity.ok(createdUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id) {
        try {
            User updatedUser = userService.toggleStatus(id);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok("Xóa người dùng thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
