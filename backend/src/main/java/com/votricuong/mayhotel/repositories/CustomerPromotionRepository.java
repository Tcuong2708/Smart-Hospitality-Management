package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.CustomerPromotion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerPromotionRepository extends MongoRepository<CustomerPromotion, Long> {
    List<CustomerPromotion> findByCustomerIdAndStatus(Long customerId, String status);
    Optional<CustomerPromotion> findByPromotionIdAndCustomerId(Long promotionId, Long customerId);
    boolean existsByPromotionIdAndCustomerId(Long promotionId, Long customerId);
    boolean existsByPromotionIdAndCustomerIdAndStatus(Long promotionId, Long customerId, String status);
    long countByPromotionId(Long promotionId);
}
