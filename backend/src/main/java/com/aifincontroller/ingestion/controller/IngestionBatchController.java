package com.aifincontroller.ingestion.controller;

import com.aifincontroller.ingestion.domain.IngestionBatch;
import com.aifincontroller.ingestion.dto.IngestionBatchResponse;
import com.aifincontroller.ingestion.service.IngestionBatchService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ingestion/batches")
public class IngestionBatchController {

    private final IngestionBatchService batchService;

    public IngestionBatchController(
            IngestionBatchService batchService) {
        this.batchService = batchService;
    }

    @GetMapping
    public ResponseEntity<List<IngestionBatchResponse>> getBatches() {
        return ResponseEntity.ok(
                batchService.getAllBatches()
                        .stream()
                        .map(this::toResponse)
                        .toList());
    }

    @GetMapping("/{batchId}")
    public ResponseEntity<IngestionBatchResponse> getBatch(
            @PathVariable String batchId) {

        return batchService.getBatch(batchId)
                .map(batch -> ResponseEntity.ok(toResponse(batch)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private IngestionBatchResponse toResponse(
            IngestionBatch batch) {

        return new IngestionBatchResponse(
                batch.getBatchId(),
                batch.getEntityType(),
                batch.getFilename(),
                batch.getStatus(),
                batch.getTotalRows(),
                batch.getImportedRows(),
                batch.getSkippedRows(),
                batch.getFailedRows(),
                batch.getStartedAt(),
                batch.getCompletedAt());
    }
}
