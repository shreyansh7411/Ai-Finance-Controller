package com.aifincontroller.ingestion.dto;

import java.util.List;

public class IngestionResult {

    private String batchId;
    private String entityType;
    private long totalRows;
    private long importedRows;
    private long skippedRows;
    private long failedRows;
    private List<IngestionError> errors;

    public IngestionResult() {
    }

    public IngestionResult(
            String batchId,
            String entityType,
            long totalRows,
            long importedRows,
            long skippedRows,
            long failedRows,
            List<IngestionError> errors) {

        this.batchId = batchId;
        this.entityType = entityType;
        this.totalRows = totalRows;
        this.importedRows = importedRows;
        this.skippedRows = skippedRows;
        this.failedRows = failedRows;
        this.errors = errors;
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

    public List<IngestionError> getErrors() {
        return errors;
    }

    public void setErrors(List<IngestionError> errors) {
        this.errors = errors;
    }
}
