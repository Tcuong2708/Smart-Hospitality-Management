package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.*;
import com.votricuong.mayhotel.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionService {
    public static final long SUPER_ADMIN_ROLE_ID = 8L;
    public static final String ALLOW = "ALLOW";
    public static final String DENY = "DENY";

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionOverrideRepository overrideRepository;
    private final PermissionAuditRepository auditRepository;
    private final SequenceGeneratorService sequenceGenerator;

    public PermissionService(PermissionRepository permissionRepository,
                             RolePermissionRepository rolePermissionRepository,
                             UserPermissionOverrideRepository overrideRepository,
                             PermissionAuditRepository auditRepository,
                             SequenceGeneratorService sequenceGenerator) {
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.overrideRepository = overrideRepository;
        this.auditRepository = auditRepository;
        this.sequenceGenerator = sequenceGenerator;
    }

    public boolean hasPermission(User user, String permissionCode) {
        if (user == null || permissionCode == null || permissionCode.isBlank()) return false;
        if (Objects.equals(user.getRoleId(), SUPER_ADMIN_ROLE_ID)) return true;
        Optional<UserPermissionOverride> override = overrideRepository
                .findByUserIdAndPermissionCode(user.getId(), permissionCode);
        if (override.isPresent()) return ALLOW.equalsIgnoreCase(override.get().getEffect());
        return rolePermissionRepository.existsByRoleIdAndPermissionCode(user.getRoleId(), permissionCode);
    }

    public List<Permission> getPermissions() {
        return permissionRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Permission::getSortOrder).thenComparing(Permission::getCode))
                .toList();
    }

    public Set<String> getRolePermissionCodes(Long roleId) {
        if (Objects.equals(roleId, SUPER_ADMIN_ROLE_ID)) {
            return getPermissions().stream().map(Permission::getCode).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return rolePermissionRepository.findByRoleId(roleId).stream()
                .map(RolePermission::getPermissionCode).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Map<String, String> getUserOverrides(Long userId) {
        return overrideRepository.findByUserId(userId).stream().collect(Collectors.toMap(
                UserPermissionOverride::getPermissionCode, UserPermissionOverride::getEffect,
                (left, right) -> right, LinkedHashMap::new));
    }

    public Set<String> getEffectivePermissionCodes(User user) {
        if (user == null) return Set.of();
        Set<String> result = new LinkedHashSet<>(getRolePermissionCodes(user.getRoleId()));
        getUserOverrides(user.getId()).forEach((code, effect) -> {
            if (ALLOW.equals(effect)) result.add(code); else if (DENY.equals(effect)) result.remove(code);
        });
        return result;
    }

    @Transactional
    public boolean setRolePermission(Long actorId, Long targetRoleId, String code, boolean allowed) {
        if (Objects.equals(targetRoleId, SUPER_ADMIN_ROLE_ID)) {
            throw new IllegalArgumentException("Không thể thu hồi quyền cố định của Super Admin");
        }
        if (!permissionRepository.existsById(code)) throw new IllegalArgumentException("Quyền không tồn tại");
        String id = targetRoleId + ":" + code;
        boolean before = rolePermissionRepository.existsById(id);
        if (before == allowed) return false;
        if (allowed) {
            rolePermissionRepository.save(RolePermission.builder().id(id).roleId(targetRoleId)
                    .permissionCode(code).build());
        } else {
            rolePermissionRepository.deleteById(id);
        }
        auditRepository.save(PermissionAudit.builder()
                .id(sequenceGenerator.generateSequence("permission_audits_sequence"))
                .actorUserId(actorId).targetRoleId(targetRoleId).permissionCode(code)
                .oldEffect(before ? ALLOW : DENY).newEffect(allowed ? ALLOW : DENY)
                .createdAt(new Date()).build());
        return true;
    }

    @Transactional
    public void setUserOverride(Long actorId, User target, String code, String effect) {
        if (target == null) throw new IllegalArgumentException("Tài khoản không tồn tại");
        if (Objects.equals(target.getRoleId(), SUPER_ADMIN_ROLE_ID)) {
            throw new IllegalArgumentException("Không thể ghi đè quyền Super Admin");
        }
        if (!permissionRepository.existsById(code)) throw new IllegalArgumentException("Quyền không tồn tại");
        String id = target.getId() + ":" + code;
        String old = overrideRepository.findById(id).map(UserPermissionOverride::getEffect).orElse("INHERIT");
        String normalized = effect == null ? "INHERIT" : effect.toUpperCase(Locale.ROOT);
        if ("INHERIT".equals(normalized)) overrideRepository.deleteById(id);
        else if (ALLOW.equals(normalized) || DENY.equals(normalized)) {
            overrideRepository.save(UserPermissionOverride.builder().id(id).userId(target.getId())
                    .permissionCode(code).effect(normalized).build());
        } else throw new IllegalArgumentException("Giá trị quyền ngoại lệ không hợp lệ");
        auditRepository.save(PermissionAudit.builder()
                .id(sequenceGenerator.generateSequence("permission_audits_sequence"))
                .actorUserId(actorId).targetUserId(target.getId()).permissionCode(code)
                .oldEffect(old).newEffect(normalized).createdAt(new Date()).build());
    }
}
