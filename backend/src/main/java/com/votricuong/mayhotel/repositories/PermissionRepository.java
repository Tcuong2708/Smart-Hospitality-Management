package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.Permission;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PermissionRepository extends MongoRepository<Permission, String> {
}
