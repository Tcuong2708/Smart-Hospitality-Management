package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import com.votricuong.mayhotel.documents.Role;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.repositories.RoleRepository;
import com.votricuong.mayhotel.services.UserService;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Hiển thị ma trận vai trò hiện có; không tự động thay đổi dữ liệu MongoDB. */
@Controller
@RequestMapping("/admin/roles")
public class RoleAdminController extends BaseController {
    private final RoleRepository roleRepository;
    private final UserService userService;

    public RoleAdminController(RoleRepository roleRepository, UserService userService) {
        this.roleRepository = roleRepository;
        this.userService = userService;
    }

    @Data
    @Builder
    public static class RoleDTO {
        private Long id;
        private String code;
        private String name;
        private String description;
        private String permissions;
        private long userCount;
        private String badgeClass;
    }

    private record RoleMeta(String code, String badgeClass, String permissions) {}

    private static final Map<Long, RoleMeta> ROLE_META = Map.of(
            1L, new RoleMeta("ADMIN", "adm-badge-danger", "Quản lý tài khoản, sao lưu và phục hồi, phân quyền vai trò"),
            2L, new RoleMeta("MANAGER", "adm-badge-warning", "Phòng, loại phòng, dịch vụ, nhân viên, khách hàng, đánh giá, điểm, khuyến mãi, doanh thu"),
            3L, new RoleMeta("RECEPTIONIST", "adm-badge-info", "Sơ đồ phòng, check-in, check-out, đặt phòng, hóa đơn, dự đoán no-show"),
            4L, new RoleMeta("HOUSEKEEPING", "adm-badge-neutral", "Cập nhật trạng thái buồng phòng trên ứng dụng di động"),
            5L, new RoleMeta("CUSTOMER", "adm-badge-neutral", "Đặt phòng trực tuyến, tích điểm, voucher, đánh giá"),
            6L, new RoleMeta("SERVICE_STAFF", "adm-badge-neutral", "Tiếp nhận và quản lý dịch vụ khách sạn"),
            7L, new RoleMeta("ACCOUNTANT", "adm-badge-success", "Hóa đơn, báo cáo tài chính và đối soát doanh thu"));

    @GetMapping
    public String index(Model model) {
        setPageTitle(model, "Phân quyền và vai trò");
        List<User> users = userService.getAllUsers();
        Map<Long, Long> countMap = users.stream()
                .filter(user -> user.getRoleId() != null)
                .collect(Collectors.groupingBy(User::getRoleId, Collectors.counting()));

        List<RoleDTO> roleList = new ArrayList<>();
        for (Role role : roleRepository.findAll()) {
            RoleMeta meta = ROLE_META.getOrDefault(role.getId(),
                    new RoleMeta("ROLE_" + role.getId(), "adm-badge-neutral", "Chưa xác định"));
            roleList.add(RoleDTO.builder()
                    .id(role.getId())
                    .code(meta.code())
                    .name(role.getRoleName())
                    .description(role.getDescription())
                    .permissions(meta.permissions())
                    .userCount(countMap.getOrDefault(role.getId(), 0L))
                    .badgeClass(meta.badgeClass())
                    .build());
        }
        roleList.sort((left, right) -> Long.compare(left.getId(), right.getId()));
        model.addAttribute("roles", roleList);

        String view = "view/admin/roles/index";
        model.addAttribute("extra_css_template", view);
        model.addAttribute("extra_css_fragment", "extra_css");
        model.addAttribute("extra_js_template", view);
        model.addAttribute("extra_js_fragment", "extra_js");
        return render(model, view);
    }
}
