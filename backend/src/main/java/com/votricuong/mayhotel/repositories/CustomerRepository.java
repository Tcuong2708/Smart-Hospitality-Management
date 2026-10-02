package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import java.util.List;

@Repository
public interface CustomerRepository extends MongoRepository<Customer, Long> {
    List<Customer> findByUserId(Long userId);
    List<Customer> findByPhone(String phone);
    List<Customer> findByEmail(String email);
}
