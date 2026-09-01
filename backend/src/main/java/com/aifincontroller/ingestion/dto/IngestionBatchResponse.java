package com.aifincontroller.ingestion.dto;

import com.aifincontroller.ingestion.domain.IngestionBatchStatus;
import java.time.Instant;

public record IngestionBatchResponse(
        String batchId,
        String entityType,
        String filename,
        IngestionBatchStatus status,
        long totalRows,
        long importedRows,
        long skippedRows,
        long failedRows,
        Instant startedAt,
        Instant completedAt) {
}
