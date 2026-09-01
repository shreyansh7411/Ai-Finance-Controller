package com.aifincontroller.ingestion.validation;

import com.aifincontroller.ingestion.dto.IngestionError;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
public class CsvRowValidator {

    public List<IngestionError> validate(
            String entityType,
            CSVRecord record) {

        List<IngestionError> errors = new ArrayList<>();

        switch (entityType.toUpperCase()) {
            case "PAYMENT" -> validatePayment(record, errors);
            case "SETTLEMENT" -> validateSettlement(record, errors);
            case "REFUND" -> validateRefund(record, errors);
            case "ADJUSTMENT" -> validateAdjustment(record, errors);
            default -> errors.add(new IngestionError(
                    record.getRecordNumber(),
                    null,
                    "Unsupported ingestion entity type: " + entityType
            ));
        }

        return errors;
    }

    private void validatePayment(
            CSVRecord record,
            List<IngestionError> errors) {

        required(record, "payment_id", errors);
        required(record, "order_id", errors);
        required(record, "amount", errors);
        required(record, "currency", errors);
        required(record, "status", errors);
        required(record, "created_at", errors);

        decimal(record, "amount", errors);
        timestamp(record, "created_at", errors);
        timestamp(record, "captured_at", errors);
    }

    private void validateSettlement(
            CSVRecord record,
            List<IngestionError> errors) {

        required(record, "settlement_id", errors);
        required(record, "amount", errors);
        required(record, "status", errors);

        decimal(record, "amount", errors);
        decimal(record, "fees", errors);
        decimal(record, "tax", errors);
        timestamp(record, "settled_at", errors);
    }

    private void validateRefund(
            CSVRecord record,
            List<IngestionError> errors) {

        required(record, "refund_id", errors);
        required(record, "payment_id", errors);
        required(record, "amount", errors);
        required(record, "status", errors);
        required(record, "created_at", errors);

        decimal(record, "amount", errors);
        timestamp(record, "created_at", errors);
    }

    private void validateAdjustment(
            CSVRecord record,
            List<IngestionError> errors) {

        required(record, "adjustment_id", errors);
        required(record, "amount", errors);
        required(record, "type", errors);
        required(record, "created_at", errors);

        decimal(record, "amount", errors);
        timestamp(record, "created_at", errors);
    }

    private void required(
            CSVRecord record,
            String field,
            List<IngestionError> errors) {

        String value = value(record, field);

        if (value == null || value.isBlank()) {
            errors.add(new IngestionError(
                    record.getRecordNumber(),
                    field,
                    "Required field is missing"
            ));
        }
    }

    private void decimal(
            CSVRecord record,
            String field,
            List<IngestionError> errors) {

        String value = value(record, field);

        if (value == null || value.isBlank()) {
            return;
        }

        try {
            new BigDecimal(value);
        } catch (NumberFormatException e) {
            errors.add(new IngestionError(
                    record.getRecordNumber(),
                    field,
                    "Invalid decimal value"
            ));
        }
    }

    private void timestamp(
            CSVRecord record,
            String field,
            List<IngestionError> errors) {

        String value = value(record, field);

        if (value == null || value.isBlank()) {
            return;
        }

        try {
            Instant.parse(value);
        } catch (Exception e) {
            errors.add(new IngestionError(
                    record.getRecordNumber(),
                    field,
                    "Invalid timestamp. Expected ISO-8601 format"
            ));
        }
    }

    private String value(
            CSVRecord record,
            String field) {

        if (!record.isMapped(field)) {
            return null;
        }

        return record.get(field).trim();
    }
}
