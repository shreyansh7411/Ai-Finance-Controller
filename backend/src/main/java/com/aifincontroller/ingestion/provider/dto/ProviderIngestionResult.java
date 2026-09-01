package com.aifincontroller.ingestion.provider.dto;

public class ProviderIngestionResult {

    private String provider;
    private String entityType;
    private long fetchedRows;
    private long importedRows;
    private long skippedRows;
    private long failedRows;

    public ProviderIngestionResult() {
    }

    public ProviderIngestionResult(
            String provider,
            String entityType,
            long fetchedRows,
            long importedRows,
            long skippedRows,
            long failedRows) {

        this.provider = provider;
        this.entityType = entityType;
        this.fetchedRows = fetchedRows;
        this.importedRows = importedRows;
        this.skippedRows = skippedRows;
        this.failedRows = failedRows;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public long getFetchedRows() {
        return fetchedRows;
    }

    public void setFetchedRows(long fetchedRows) {
        this.fetchedRows = fetchedRows;
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
}
