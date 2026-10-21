package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.Promotion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRepository extends MongoRepository<Promotion, Long> {
}