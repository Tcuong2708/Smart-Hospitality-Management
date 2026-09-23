package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.BookingDetail;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingDetailRepository extends MongoRepository<BookingDetail, Long> {
    List<BookingDetail> findByBookingId(Long bookingId);
}
