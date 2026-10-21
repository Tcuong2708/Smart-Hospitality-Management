package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.BackupHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackupHistoryRepository extends MongoRepository<BackupHistory, Long> {
}
