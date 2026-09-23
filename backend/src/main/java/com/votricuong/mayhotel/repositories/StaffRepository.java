package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.Staff;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface StaffRepository extends MongoRepository<Staff, Long> {
    Optional<Staff> findByPhone(String phone);
}
