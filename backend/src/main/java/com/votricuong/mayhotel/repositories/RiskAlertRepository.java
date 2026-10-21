package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.RiskAlert;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RiskAlertRepository extends MongoRepository<RiskAlert, Long> {
    Optional<RiskAlert> findFirstByBookingId(Long bookingId);
}
