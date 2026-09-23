package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.ServiceTicket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceTicketRepository extends MongoRepository<ServiceTicket, Long> {
    List<ServiceTicket> findByBookingId(Long bookingId);
}
