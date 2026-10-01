package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.LoyaltyPolicy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoyaltyPolicyRepository extends MongoRepository<LoyaltyPolicy, String> {
}
