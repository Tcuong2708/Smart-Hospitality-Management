package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.BookingRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRequestRepository extends MongoRepository<BookingRequest, Long> {
}
