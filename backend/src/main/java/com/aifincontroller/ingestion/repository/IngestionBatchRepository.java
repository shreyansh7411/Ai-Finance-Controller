package com.aifincontroller.ingestion.repository;

import com.aifincontroller.ingestion.domain.IngestionBatch;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionBatchRepository
        extends JpaRepository<IngestionBatch, Long> {

    Optional<IngestionBatch> findByBatchId(String batchId);

    boolean existsByBatchId(String batchId);
}
