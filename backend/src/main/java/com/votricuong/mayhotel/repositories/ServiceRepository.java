package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.Service;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends MongoRepository<Service, Long> {
}
