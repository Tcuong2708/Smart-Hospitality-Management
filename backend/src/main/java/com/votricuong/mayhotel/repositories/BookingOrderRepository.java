package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.BookingOrder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingOrderRepository extends MongoRepository<BookingOrder, Long> {
    boolean existsByCustomerId(Long customerId);
    List<BookingOrder> findByCustomerIdOrderByBookingDateDesc(Long customerId);
}
