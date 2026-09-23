package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.BookingOrder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingOrderRepository extends MongoRepository<BookingOrder, Long> {
}
