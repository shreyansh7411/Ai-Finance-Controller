package com.aifincontroller.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ingestion_batches")
public class IngestionBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false, unique = true, length = 100)
    private String batchId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "filename", length = 255)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IngestionBatchStatus status;

    @Column(name = "total_rows", nullable = false)
    private long totalRows;

    @Column(name = "imported_rows", nullable = false)
    private long importedRows;

    @Column(name = "skipped_rows", nullable = false)
    private long skippedRows;

    @Column(name = "failed_rows", nullable = false)
    private long failedRows;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void onCreate() {
        if (startedAt == null) {
            startedAt = Instant.now();
        }

        if (status == null) {
            status = IngestionBatchStatus.PROCESSING;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public IngestionBatchStatus getStatus() {
        return status;
    }

    public void setStatus(IngestionBatchStatus status) {
        this.status = status;
    }

    public long getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(long totalRows) {
        this.totalRows = totalRows;
    }

    public long getImportedRows() {
        return importedRows;
    }

    public void setImportedRows(long importedRows) {
        this.importedRows = importedRows;
    }

    public long getSkippedRows() {
        return skippedRows;
    }

    public void setSkippedRows(long skippedRows) {
        this.skippedRows = skippedRows;
    }

    public long getFailedRows() {
        return failedRows;
    }

    public void setFailedRows(long failedRows) {
        this.failedRows = failedRows;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
