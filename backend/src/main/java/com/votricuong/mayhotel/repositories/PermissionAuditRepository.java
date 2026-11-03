package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.PermissionAudit;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PermissionAuditRepository extends MongoRepository<PermissionAudit, Long> {
}
