package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.Staff;
import com.votricuong.mayhotel.services.StaffService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.servlet.http.HttpSession;
import com.votricuong.mayhotel.documents.User;

@RestController
@RequestMapping("/api/staffs")
public class StaffApiController {

    private final StaffService staffService;

    public StaffApiController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public ResponseEntity<?> getAllStaffs(HttpSession session) {
        if (!allowed(session)) return ResponseEntity.status(403).body("Không có quyền quản lý nhân viên");
        return ResponseEntity.ok(staffService.getAllStaffs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStaffById(@PathVariable Long id, HttpSession session) {
        if (!allowed(session)) return ResponseEntity.status(403).body("Không có quyền quản lý nhân viên");
        Optional<Staff> staffOpt = staffService.getStaffById(id);
        return staffOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createStaff(@RequestBody Map<String, Object> payload, HttpSession session) {
        if (!allowed(session)) return ResponseEntity.status(403).body("Không có quyền quản lý nhân viên");
        try {
            // Lấy thông tin Staff
            Staff staff = new Staff();
            staff.setFullName((String) payload.get("fullName"));
            if (payload.get("dob") != null) {
                staff.setDob(new java.util.Date((Long) payload.get("dob")));
            }
            staff.setGender((String) payload.get("gender"));
            staff.setPhone((String) payload.get("phone"));
            staff.setPosition((String) payload.get("position"));

            // Lấy thông tin User
            String username = (String) payload.get("username");
            String password = (String) payload.get("password");
            Long roleId = payload.get("roleId") != null ? Long.valueOf(payload.get("roleId").toString()) : 2L;

            if (username == null || password == null) {
                throw new Exception("Vui lòng cung cấp username và password để tạo tài khoản nhân viên.");
            }

            Staff createdStaff = staffService.createStaff(staff, username, password, roleId);
            return ResponseEntity.ok(createdStaff);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStaff(@PathVariable Long id, @RequestBody Staff staffDetails, HttpSession session) {
        if (!allowed(session)) return ResponseEntity.status(403).body("Không có quyền quản lý nhân viên");
        try {
            Staff updatedStaff = staffService.updateStaff(id, staffDetails);
            return ResponseEntity.ok(updatedStaff);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStaff(@PathVariable Long id, HttpSession session) {
        if (!allowed(session)) return ResponseEntity.status(403).body("Không có quyền quản lý nhân viên");
        try {
            staffService.deleteStaff(id);
            return ResponseEntity.ok("Xóa nhân viên thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private boolean allowed(HttpSession session) {
        Object current = session == null ? null : session.getAttribute("user");
        return current instanceof User user && (Long.valueOf(1L).equals(user.getRoleId()) || Long.valueOf(2L).equals(user.getRoleId()));
    }
}
