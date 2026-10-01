package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends MongoRepository<Review, Long> {
    Optional<Review> findByBookingId(Long bookingId);
    List<Review> findByCustomerId(Long customerId);
}
