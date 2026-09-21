package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends MongoRepository<Invoice, Long> {
    List<Invoice> findByUserId(Long userId);
    List<Invoice> findByPhone(String phone);
}
