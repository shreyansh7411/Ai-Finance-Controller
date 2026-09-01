package com.aifincontroller.ingestion.dto;

public class IngestionError {

    private long rowNumber;
    private String field;
    private String message;

    public IngestionError() {
    }

    public IngestionError(long rowNumber, String field, String message) {
        this.rowNumber = rowNumber;
        this.field = field;
        this.message = message;
    }

    public long getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(long rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
