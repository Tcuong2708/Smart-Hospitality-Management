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
        return createStaff(staff, username, password, roleId, staff == null ? null : staff.getPhone() + "@hotel.com");
    }

    public Staff createStaff(Staff staff, String username, String password, Long roleId, String email) throws Exception {
        validate(staff, null);
        username = com.votricuong.mayhotel.utils.InputValidator.username(username);
        password = com.votricuong.mayhotel.utils.InputValidator.password(password);
        long validatedRole = roleId == null ? 3L : roleId;
        if (validatedRole < 1 || validatedRole > 7 || validatedRole == 5L) {
            throw new IllegalArgumentException("Vai trò nhân viên không hợp lệ");
        }
        // 1. Tạo User (Tài khoản) trước
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setEmail(email);
        newUser.setFullName(staff.getFullName());
        newUser.setPhone(staff.getPhone());
        newUser.setRoleId(validatedRole);
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
        validate(staffDetails, id);
        Staff staff = staffOpt.get();
        staff.setFullName(staffDetails.getFullName());
        staff.setDob(staffDetails.getDob());
        staff.setGender(staffDetails.getGender());
        staff.setPhone(staffDetails.getPhone());
        staff.setPosition(staffDetails.getPosition());
        staff.setBaseSalary(staffDetails.getBaseSalary() == null ? 0D :
                com.votricuong.mayhotel.utils.InputValidator.nonNegativeMoney(staffDetails.getBaseSalary(), "Lương cơ bản"));
        
        return staffRepository.save(staff);
    }

    public void deleteStaff(Long id) throws Exception {
        Optional<Staff> staffOpt = staffRepository.findById(id);
        if (staffOpt.isEmpty()) {
            throw new Exception("Không tìm thấy nhân viên.");
        }
        Long linkedUserId = staffOpt.get().getUserId();
        staffRepository.deleteById(id);
        // Sau khi bỏ liên kết nhân viên mới được phép xóa tài khoản tương ứng.
        try {
            if (linkedUserId != null) {
                userService.deleteUser(linkedUserId);
            }
        } catch (Exception e) {
            // Giữ tài khoản nếu còn được tham chiếu ở hồ sơ khác.
        }
    }

    private void validate(Staff staff, Long excludedId) {
        if (staff == null) throw new IllegalArgumentException("Dữ liệu nhân viên không được để trống");
        staff.setFullName(com.votricuong.mayhotel.utils.InputValidator.requiredText(staff.getFullName(), "Họ và tên", 100));
        staff.setPhone(com.votricuong.mayhotel.utils.InputValidator.phone(staff.getPhone()));
        staff.setPosition(com.votricuong.mayhotel.utils.InputValidator.requiredText(staff.getPosition(), "Chức vụ", 100));
        staff.setBaseSalary(staff.getBaseSalary() == null ? 0D :
                com.votricuong.mayhotel.utils.InputValidator.nonNegativeMoney(staff.getBaseSalary(), "Lương cơ bản"));
        if (staff.getDob() != null && staff.getDob().after(new java.util.Date())) {
            throw new IllegalArgumentException("Ngày sinh không được ở tương lai");
        }
        if (staffRepository.findByPhone(staff.getPhone()).filter(item -> !item.getId().equals(excludedId)).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại nhân viên đã tồn tại");
        }
    }
}
