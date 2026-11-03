package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import com.votricuong.mayhotel.documents.Permission;
import com.votricuong.mayhotel.documents.Role;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.repositories.RoleRepository;
import com.votricuong.mayhotel.services.NotificationService;
import com.votricuong.mayhotel.services.PermissionService;
import com.votricuong.mayhotel.services.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/roles")
public class RoleAdminController extends BaseController {
    private final RoleRepository roleRepository;
    private final UserService userService;
    private final PermissionService permissionService;
    private final NotificationService notificationService;

    public RoleAdminController(RoleRepository roleRepository, UserService userService,
                               PermissionService permissionService, NotificationService notificationService) {
        this.roleRepository = roleRepository;
        this.userService = userService;
        this.permissionService = permissionService;
        this.notificationService = notificationService;
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
    public record PermissionGroup(String module, String name, String platform) {}
    public record PermissionAction(String code, String name) {}
    private static final Map<Long, RoleMeta> ROLE_META = Map.of(
            1L, new RoleMeta("QUẢN TRỊ VIÊN", "adm-badge-danger", "Quản lý hệ thống theo quyền được Quản trị cấp cao cấp"),
            2L, new RoleMeta("QUẢN LÝ", "adm-badge-warning", "Quản lý nghiệp vụ khách sạn"),
            3L, new RoleMeta("LỄ TÂN", "adm-badge-info", "Nghiệp vụ lễ tân"),
            4L, new RoleMeta("BUỒNG PHÒNG", "adm-badge-neutral", "Cập nhật trạng thái buồng phòng trên ứng dụng di động"),
            5L, new RoleMeta("KHÁCH HÀNG", "adm-badge-neutral", "Đặt phòng, điểm, phiếu ưu đãi và đánh giá"),
            6L, new RoleMeta("NHÂN VIÊN DỊCH VỤ", "adm-badge-neutral", "Tiếp nhận và quản lý dịch vụ"),
            7L, new RoleMeta("KẾ TOÁN", "adm-badge-success", "Báo cáo và đối soát doanh thu"),
            8L, new RoleMeta("QUẢN TRỊ CẤP CAO", "adm-badge-danger", "Toàn quyền và quản lý phân quyền"));

    @GetMapping
    public String index(Model model, HttpSession session) {
        setPageTitle(model, "Phân quyền và vai trò");
        List<User> users = userService.getAllUsers();
        Map<Long, Long> countMap = users.stream().filter(user -> user.getRoleId() != null)
                .collect(Collectors.groupingBy(User::getRoleId, Collectors.counting()));
        List<RoleDTO> roleList = new ArrayList<>();
        for (Role role : roleRepository.findAll()) {
            RoleMeta meta = ROLE_META.getOrDefault(role.getId(),
                    new RoleMeta("ROLE_" + role.getId(), "adm-badge-neutral", "Chưa xác định"));
            roleList.add(RoleDTO.builder().id(role.getId()).code(meta.code()).name(role.getRoleName())
                    .description(role.getDescription()).permissions(meta.permissions())
                    .userCount(countMap.getOrDefault(role.getId(), 0L)).badgeClass(meta.badgeClass()).build());
        }
        roleList.sort(Comparator.comparing(RoleDTO::getId));
        model.addAttribute("roles", roleList);
        List<Permission> permissionCatalog = permissionService.getPermissions();
        model.addAttribute("permissions", permissionCatalog);
        Map<String, Map<String, Permission>> permissionMatrix = new LinkedHashMap<>();
        Map<String, PermissionGroup> permissionGroups = new LinkedHashMap<>();
        for (Permission permission : permissionCatalog) {
            permissionMatrix.computeIfAbsent(permission.getModule(), ignored -> new LinkedHashMap<>())
                    .put(permission.getAction(), permission);
            permissionGroups.putIfAbsent(permission.getModule(), new PermissionGroup(
                    permission.getModule(), permission.getModuleDisplayName(), permission.getPlatformDisplayName()));
        }
        model.addAttribute("permissionCatalog", permissionMatrix);
        model.addAttribute("permissionGroups", new ArrayList<>(permissionGroups.values()));
        model.addAttribute("permissionActions", List.of(
                new PermissionAction("VIEW", "Xem"),
                new PermissionAction("CREATE", "Thêm"),
                new PermissionAction("UPDATE", "Cập nhật"),
                new PermissionAction("DELETE", "Xóa"),
                new PermissionAction("PRINT", "In"),
                new PermissionAction("EXPORT", "Xuất báo cáo")));
        Map<Long, Set<String>> matrix = new LinkedHashMap<>();
        roleList.forEach(role -> matrix.put(role.getId(), permissionService.getRolePermissionCodes(role.getId())));
        model.addAttribute("rolePermissionMap", matrix);
        User actor = (User) session.getAttribute("user");
        model.addAttribute("isSuperAdmin", actor != null && Objects.equals(actor.getRoleId(), 8L));

        String view = "view/admin/roles/index";
        model.addAttribute("extra_css_template", view);
        model.addAttribute("extra_css_fragment", "extra_css");
        model.addAttribute("extra_js_template", view);
        model.addAttribute("extra_js_fragment", "extra_js");
        return render(model, view);
    }

    @PostMapping("/{roleId}/permissions")
    public String updatePermissions(@PathVariable Long roleId,
                                    @RequestParam(required = false, name = "permissions") List<String> selected,
                                    HttpSession session, RedirectAttributes redirect) {
        User actor = (User) session.getAttribute("user");
        if (actor == null) return "redirect:/account/login";
        boolean superAdmin = Objects.equals(actor.getRoleId(), PermissionService.SUPER_ADMIN_ROLE_ID);
        if (Objects.equals(roleId, PermissionService.SUPER_ADMIN_ROLE_ID)
                || (!superAdmin && Objects.equals(roleId, 1L))) {
            redirect.addFlashAttribute("error", "Bạn không được thay đổi quyền của vai trò này");
            return "redirect:/admin/roles";
        }
        if (!superAdmin && (roleId == null || roleId < 2L || roleId > 7L)) {
            redirect.addFlashAttribute("error", "Admin chỉ được phân quyền cho các vai trò cấp dưới");
            return "redirect:/admin/roles";
        }

        Set<String> desired = selected == null ? new HashSet<>() : new HashSet<>(selected);
        Set<String> before = permissionService.getRolePermissionCodes(roleId);
        int changed = 0;
        for (Permission permission : permissionService.getPermissions()) {
            boolean allowed = desired.contains(permission.getCode());
            if (!superAdmin && permission.getCode().startsWith("SYSTEM.")) allowed = false;
            if (permissionService.setRolePermission(actor.getId(), roleId, permission.getCode(), allowed)) changed++;
        }
        if (changed > 0) {
            Set<String> revoked = new HashSet<>(before);
            revoked.removeAll(desired);
            String message = revoked.isEmpty()
                    ? "Quyền chức năng của vai trò đã được cập nhật bởi " + actor.getUsername()
                    : "Một số quyền chức năng đã bị thu hồi bởi " + actor.getUsername() + ". Vui lòng tải lại ứng dụng.";
            userService.getAllUsers().stream().filter(u -> Objects.equals(u.getRoleId(), roleId)).forEach(u ->
                    notificationService.createForUser(u.getId(), "PERMISSION_CHANGED",
                            "Quyền truy cập đã thay đổi", message, "/"));
        }
        redirect.addFlashAttribute("success", "Đã cập nhật " + changed + " quyền cho vai trò");
        return "redirect:/admin/roles";
    }
}
