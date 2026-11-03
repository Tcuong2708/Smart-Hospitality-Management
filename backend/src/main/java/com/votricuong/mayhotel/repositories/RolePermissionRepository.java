package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.RolePermission;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RolePermissionRepository extends MongoRepository<RolePermission, String> {
    boolean existsByRoleIdAndPermissionCode(Long roleId, String permissionCode);
    List<RolePermission> findByRoleId(Long roleId);
    void deleteByRoleIdAndPermissionCode(Long roleId, String permissionCode);
}
