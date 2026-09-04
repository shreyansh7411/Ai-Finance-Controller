package com.aifincontroller.ai.repository;

import com.aifincontroller.ai.domain.AiInvestigationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiInvestigationRecordRepository
        extends JpaRepository<AiInvestigationRecord, Long> {

    Optional<AiInvestigationRecord> findByExceptionId(Long exceptionId);
}
