package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.RoomChangeHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomChangeHistoryRepository extends MongoRepository<RoomChangeHistory, Long> {
}
