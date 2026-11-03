package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.UserPermissionOverride;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserPermissionOverrideRepository extends MongoRepository<UserPermissionOverride, String> {
    Optional<UserPermissionOverride> findByUserIdAndPermissionCode(Long userId, String permissionCode);
    List<UserPermissionOverride> findByUserId(Long userId);
    void deleteByUserIdAndPermissionCode(Long userId, String permissionCode);
}
