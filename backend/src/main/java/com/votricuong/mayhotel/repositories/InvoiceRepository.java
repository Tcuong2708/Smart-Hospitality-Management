package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Date;
import org.springframework.data.mongodb.repository.Query;

@Repository
public interface InvoiceRepository extends MongoRepository<Invoice, Long> {
    List<Invoice> findByUserId(Long userId);
    List<Invoice> findByPhone(String phone);
    List<Invoice> findByBookingId(Long bookingId);
    List<Invoice> findByBookingIdIn(List<Long> bookingIds);
    List<Invoice> findByPromoIdAndUserId(Long promoId, Long userId);
    List<Invoice> findByPromoIdAndPhone(Long promoId, String phone);

    @Query("{ 'isPaid': true, '$or': [" +
            "{ 'created_at': { '$gte': ?0, '$lte': ?1 } }," +
            "{ 'payment_history.paymentDate': { '$gte': ?0, '$lte': ?1 } } ] }")
    List<Invoice> findPaidCandidatesForRevenue(Date from, Date to);

    boolean existsByStaffId(Long staffId);
}
