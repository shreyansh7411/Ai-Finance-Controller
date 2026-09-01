package com.aifincontroller.ingestion.normalization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FinancialDataNormalizerTest {

    private final FinancialDataNormalizer normalizer =
            new FinancialDataNormalizer();

    @Test
    void normalizesCurrencyToUpperCase() {
        assertThat(normalizer.normalizeCurrency("  inr  "))
                .isEqualTo("INR");
    }

    @Test
    void normalizesStatusToLowerCase() {
        assertThat(normalizer.normalizeStatus("  Captured  "))
                .isEqualTo("captured");
    }

    @Test
    void normalizesTypeToLowerCase() {
        assertThat(normalizer.normalizeType("  CREDIT  "))
                .isEqualTo("credit");
    }

    @Test
    void trimsNormalText() {
        assertThat(normalizer.normalizeText("  payment_123  "))
                .isEqualTo("payment_123");
    }

    @Test
    void convertsBlankTextToNull() {
        assertThat(normalizer.normalizeText("   "))
                .isNull();
    }

    @Test
    void normalizesAmountWithoutChangingItsValue() {
        BigDecimal result =
                normalizer.normalizeAmount(" 123.4500 ");

        assertThat(result)
                .isEqualByComparingTo(new BigDecimal("123.4500"));
    }

    @Test
    void blankAmountBecomesNull() {
        assertThat(normalizer.normalizeAmount("   "))
                .isNull();
    }

    @Test
    void invalidAmountIsRejected() {
        assertThatThrownBy(() ->
                normalizer.normalizeAmount("abc"))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void normalizesTimestamp() {
        Instant result = normalizer.normalizeTimestamp(
                " 2026-08-31T06:00:00Z ");

        assertThat(result)
                .isEqualTo(Instant.parse("2026-08-31T06:00:00Z"));
    }

    @Test
    void blankTimestampBecomesNull() {
        assertThat(normalizer.normalizeTimestamp("   "))
                .isNull();
    }

    @Test
    void invalidTimestampIsRejected() {
        assertThatThrownBy(() ->
                normalizer.normalizeTimestamp("not-a-timestamp"))
                .isInstanceOf(Exception.class);
    }
}
