package com.aifincontroller.ingestion.service;

import com.aifincontroller.ingestion.domain.IngestionBatch;
import com.aifincontroller.ingestion.domain.IngestionBatchStatus;
import com.aifincontroller.ingestion.repository.IngestionBatchRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngestionBatchService {

    private final IngestionBatchRepository batchRepository;

    public IngestionBatchService(IngestionBatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    @Transactional
    public IngestionBatch createBatch(
            String entityType,
            String filename) {

        IngestionBatch batch = new IngestionBatch();

        batch.setBatchId("batch_" + UUID.randomUUID());
        batch.setEntityType(entityType);
        batch.setFilename(filename);
        batch.setStatus(IngestionBatchStatus.PROCESSING);
        batch.setTotalRows(0);
        batch.setImportedRows(0);
        batch.setSkippedRows(0);
        batch.setFailedRows(0);
        batch.setStartedAt(Instant.now());

        return batchRepository.save(batch);
    }

    @Transactional(readOnly = true)
    public List<IngestionBatch> getAllBatches() {
        return batchRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<IngestionBatch> getBatch(String batchId) {
        return batchRepository.findByBatchId(batchId);
    }

    @Transactional
    public IngestionBatch completeBatch(
            IngestionBatch batch,
            long totalRows,
            long importedRows,
            long skippedRows,
            long failedRows) {

        batch.setTotalRows(totalRows);
        batch.setImportedRows(importedRows);
        batch.setSkippedRows(skippedRows);
        batch.setFailedRows(failedRows);
        batch.setCompletedAt(Instant.now());

        if (failedRows > 0) {
            batch.setStatus(IngestionBatchStatus.COMPLETED_WITH_ERRORS);
        } else {
            batch.setStatus(IngestionBatchStatus.COMPLETED);
        }

        return batchRepository.save(batch);
    }

    @Transactional
    public IngestionBatch failBatch(
            IngestionBatch batch,
            long totalRows,
            long importedRows,
            long skippedRows,
            long failedRows) {

        batch.setTotalRows(totalRows);
        batch.setImportedRows(importedRows);
        batch.setSkippedRows(skippedRows);
        batch.setFailedRows(failedRows);
        batch.setStatus(IngestionBatchStatus.FAILED);
        batch.setCompletedAt(Instant.now());

        return batchRepository.save(batch);
    }
}
