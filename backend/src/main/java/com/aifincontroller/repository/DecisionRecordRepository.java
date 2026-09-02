package com.aifincontroller.repository;

import com.aifincontroller.domain.DecisionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DecisionRecordRepository
        extends JpaRepository<DecisionRecord, Long> {

    Optional<DecisionRecord> findByExceptionId(Long exceptionId);

    long countByOutcome(String outcome);
}
