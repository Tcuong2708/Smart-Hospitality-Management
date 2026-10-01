package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.LoyalCustomer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoyalCustomerRepository extends MongoRepository<LoyalCustomer, Long> {
    Optional<LoyalCustomer> findByCustomerId(Long customerId);
}
