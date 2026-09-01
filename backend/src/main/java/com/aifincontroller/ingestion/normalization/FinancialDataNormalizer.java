package com.aifincontroller.ingestion.normalization;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Component;

@Component
public class FinancialDataNormalizer {

    public String normalizeCurrency(String value) {
        return normalizeText(value).toUpperCase();
    }

    public String normalizeStatus(String value) {
        return normalizeText(value).toLowerCase();
    }

    public String normalizeType(String value) {
        return normalizeText(value).toLowerCase();
    }

    public String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    public BigDecimal normalizeAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return new BigDecimal(value.trim());
    }

    public Instant normalizeTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Instant.parse(value.trim());
    }
}
