package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.PointTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PointTransactionRepository extends MongoRepository<PointTransaction, String> {
    Optional<PointTransaction> findByReferenceId(String referenceId);
    List<PointTransaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
