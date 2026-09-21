package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.OtpCode;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpCodeRepository extends MongoRepository<OtpCode, String> {
    Optional<OtpCode> findByIdentifierAndCode(String identifier, String code);
    Optional<OtpCode> findByIdentifier(String identifier);
}
