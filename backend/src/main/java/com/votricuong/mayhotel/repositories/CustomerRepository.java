package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends MongoRepository<Customer, Long> {
    Optional<Customer> findByUserId(Long userId);
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findByEmail(String email);
}
