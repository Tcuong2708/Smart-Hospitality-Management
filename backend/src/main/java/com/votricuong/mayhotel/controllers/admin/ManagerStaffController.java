package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import com.votricuong.mayhotel.documents.Staff;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.services.StaffService;
import com.votricuong.mayhotel.services.UserService;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping({"/manager/staff", "/manager/staffs"})
public class ManagerStaffController extends BaseController {

    private final StaffService staffService;
    private final UserService userService;
    private final com.votricuong.mayhotel.repositories.StaffRepository staffRepository;
    private final com.votricuong.mayhotel.repositories.InvoiceRepository invoiceRepository;

    public ManagerStaffController(StaffService staffService, UserService userService,
                                  com.votricuong.mayhotel.repositories.StaffRepository staffRepository,
                                  com.votricuong.mayhotel.repositories.InvoiceRepository invoiceRepository) {
        this.staffService = staffService;
        this.userService = userService;
        this.staffRepository = staffRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Data
    @Builder
    public static class StaffDTO {
        private Long id;
        private Long userId;
        private String name;
        private String dept;
        private String phone;
        private String email;
        private String username;
        private Long roleId;
        private Double salary;
        private Integer status;
        private String note;
    }

    @GetMapping
    public String index(Model model) {
        List<Staff> staffs = staffService.getAllStaffs();
        List<User> users = userService.getAllUsers();
        
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        
        List<StaffDTO> dtoList = staffs.stream().map(s -> {
            User u = s.getUserId() != null ? userMap.get(s.getUserId()) : null;
            Integer stt = 1; // Default: Đang làm việc
            if (u != null) {
                if ("Bị khóa".equalsIgnoreCase(u.getStatus()) || "Inactive".equalsIgnoreCase(u.getStatus())) {
                    stt = 2; // Nghỉ phép/Khóa
                }
            }
            return StaffDTO.builder()
                    .id(s.getId())
                    .userId(s.getUserId())
                    .name(s.getFullName() != null ? s.getFullName() : (u != null ? u.getFullName() : "N/A"))
                    .dept(s.getPosition() != null ? s.getPosition() : "Chưa xác định")
                    .phone(s.getPhone() != null ? s.getPhone() : (u != null ? u.getPhone() : ""))
                    .email(u != null ? u.getEmail() : "")
                    .username(u != null ? u.getUsername() : "")
                    .roleId(u != null ? u.getRoleId() : null)
                    .salary(s.getBaseSalary() == null ? 0D : s.getBaseSalary())
                    .status(stt)
                    .note("")
                    .build();
        }).collect(Collectors.toList());

        setPageTitle(model, "Quản Lý Nhân Viên");
        setExtraCSS(model, "view/Manager/Staff/index :: extra_css");
        setExtraJS(model, "view/Manager/Staff/index :: extra_js");
        model.addAttribute("staffs", dtoList);
        
        return render(model, "view/Manager/Staff/index");
    }

    @PostMapping("/create")
    public String create(@RequestParam String fullName, @RequestParam String phone,
                         @RequestParam String position, @RequestParam Double baseSalary,
                         @RequestParam String username, @RequestParam String email,
                         @RequestParam String password, @RequestParam Long roleId,
                         RedirectAttributes redirect) {
        try {
            validateManagedRole(roleId);
            Staff staff = Staff.builder().fullName(fullName).phone(phone).position(position).baseSalary(baseSalary).build();
            staffService.createStaff(staff, username, password, roleId, email);
            redirect.addFlashAttribute("success", "Thêm nhân viên thành công!");
        } catch (Exception ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/manager/staff";
    }

    @PostMapping("/edit")
    public String edit(@RequestParam Long id, @RequestParam String fullName,
                       @RequestParam String phone, @RequestParam String position,
                       @RequestParam Double baseSalary, @RequestParam String email,
                       @RequestParam(required = false) String password,
                       @RequestParam Long roleId, RedirectAttributes redirect) {
        try {
            validateManagedRole(roleId);
            Staff current = staffRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên"));
            Staff input = Staff.builder().fullName(fullName).phone(phone).position(position).baseSalary(baseSalary)
                    .dob(current.getDob()).gender(current.getGender()).build();
            Staff saved = staffService.updateStaff(id, input);
            if (saved.getUserId() != null) {
                User user = new User(); user.setFullName(fullName); user.setPhone(phone); user.setEmail(email);
                user.setRoleId(roleId); user.setPassword(password);
                userService.updateUser(saved.getUserId(), user);
            }
            redirect.addFlashAttribute("success", "Cập nhật nhân viên thành công!");
        } catch (Exception ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/manager/staff";
    }

    @PostMapping("/{id}/status")
    public String status(@org.springframework.web.bind.annotation.PathVariable Long id, RedirectAttributes redirect) {
        try {
            Staff staff = staffRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên"));
            if (staff.getUserId() == null) throw new IllegalArgumentException("Nhân viên chưa có tài khoản liên kết");
            userService.toggleStatus(staff.getUserId());
            redirect.addFlashAttribute("success", "Đã cập nhật trạng thái tài khoản!");
        } catch (Exception ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/manager/staff";
    }

    @PostMapping("/{id}/delete")
    public String delete(@org.springframework.web.bind.annotation.PathVariable Long id, RedirectAttributes redirect) {
        try {
            if (invoiceRepository.existsByStaffId(id)) throw new IllegalArgumentException("Nhân viên đã phát sinh hóa đơn; hãy khóa tài khoản thay vì xóa");
            staffService.deleteStaff(id);
            redirect.addFlashAttribute("success", "Xóa nhân viên thành công!");
        } catch (Exception ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/manager/staff";
    }

    private void validateManagedRole(Long roleId) {
        if (!java.util.Set.of(3L, 4L, 6L, 7L).contains(roleId)) {
            throw new IllegalArgumentException("Quản lý chỉ được phân công lễ tân, buồng phòng, dịch vụ hoặc kế toán");
        }
    }
}
