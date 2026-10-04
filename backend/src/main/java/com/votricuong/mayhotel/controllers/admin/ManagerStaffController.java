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
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping({"/manager/staff", "/manager/staffs"})
public class ManagerStaffController extends BaseController {

    private final StaffService staffService;
    private final UserService userService;

    public ManagerStaffController(StaffService staffService, UserService userService) {
        this.staffService = staffService;
        this.userService = userService;
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
                    .salary(5000000.0) // Mock
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
}
