package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends MongoRepository<Notification, Long> {
    List<Notification> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Notification> findByCustomerIdAndIsReadFalse(Long customerId);
    long countByCustomerIdAndIsReadFalse(Long customerId);
    Optional<Notification> findByIdAndCustomerId(Long id, Long customerId);
}
