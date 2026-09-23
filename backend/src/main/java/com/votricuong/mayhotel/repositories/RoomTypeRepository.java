package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.RoomType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomTypeRepository extends MongoRepository<RoomType, Long> {
}
