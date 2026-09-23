package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.Staff;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.repositories.StaffRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StaffService {
    private final StaffRepository staffRepository;
    private final UserService userService;
    private final SequenceGeneratorService sequenceGeneratorService;

    public StaffService(StaffRepository staffRepository, UserService userService, SequenceGeneratorService sequenceGeneratorService) {
        this.staffRepository = staffRepository;
        this.userService = userService;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    public List<Staff> getAllStaffs() {
        return staffRepository.findAll();
    }

    public Optional<Staff> getStaffById(Long id) {
        return staffRepository.findById(id);
    }

    public Staff createStaff(Staff staff, String username, String password, Long roleId) throws Exception {
        // 1. Tạo User (Tài khoản) trước
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setEmail(staff.getPhone() + "@hotel.com"); // Dummy email if none provided
        newUser.setRoleId(roleId != null ? roleId : 2L); // Default role 2 for Employee/Staff
        newUser.setStatus("Hoạt động");
        
        User createdUser = userService.createUser(newUser);
        
        // 2. Tạo Staff (Nhân viên)
        staff.setId(sequenceGeneratorService.generateSequence("staffs_sequence"));
        staff.setUserId(createdUser.getId());
        
        return staffRepository.save(staff);
    }

    public Staff updateStaff(Long id, Staff staffDetails) throws Exception {
        Optional<Staff> staffOpt = staffRepository.findById(id);
        if (staffOpt.isEmpty()) {
            throw new Exception("Không tìm thấy nhân viên.");
        }
        Staff staff = staffOpt.get();
        staff.setFullName(staffDetails.getFullName());
        staff.setDob(staffDetails.getDob());
        staff.setGender(staffDetails.getGender());
        staff.setPhone(staffDetails.getPhone());
        staff.setPosition(staffDetails.getPosition());
        
        return staffRepository.save(staff);
    }

    public void deleteStaff(Long id) throws Exception {
        Optional<Staff> staffOpt = staffRepository.findById(id);
        if (staffOpt.isEmpty()) {
            throw new Exception("Không tìm thấy nhân viên.");
        }
        // Có thể xóa luôn tài khoản liên kết
        try {
            if (staffOpt.get().getUserId() != null) {
                userService.deleteUser(staffOpt.get().getUserId());
            }
        } catch (Exception e) {
            // Ignore if user not found
        }
        
        staffRepository.deleteById(id);
    }
}
