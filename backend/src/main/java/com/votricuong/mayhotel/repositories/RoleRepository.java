package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.Role;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends MongoRepository<Role, Long> {
}
