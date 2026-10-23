package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.BackupSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BackupSettingsRepository extends MongoRepository<BackupSettings, String> {
}
