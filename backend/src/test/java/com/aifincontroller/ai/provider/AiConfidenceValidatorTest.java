package com.aifincontroller.ai.provider;

import com.aifincontroller.ai.dto.AiInvestigationResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiConfidenceValidatorTest {

    private final AiConfidenceValidator validator =
            new AiConfidenceValidator();

    @Test
    void validConfidenceIsAccepted() {
        AiInvestigationResponse response =
                new AiInvestigationResponse();

        response.setConfidence(new BigDecimal("0.85"));

        assertDoesNotThrow(() ->
                validator.validate(response)
        );
    }

    @Test
    void zeroConfidenceIsAccepted() {
        AiInvestigationResponse response =
                new AiInvestigationResponse();

        response.setConfidence(BigDecimal.ZERO);

        assertDoesNotThrow(() ->
                validator.validate(response)
        );
    }

    @Test
    void oneConfidenceIsAccepted() {
        AiInvestigationResponse response =
                new AiInvestigationResponse();

        response.setConfidence(BigDecimal.ONE);

        assertDoesNotThrow(() ->
                validator.validate(response)
        );
    }

    @Test
    void negativeConfidenceIsRejected() {
        AiInvestigationResponse response =
                new AiInvestigationResponse();

        response.setConfidence(new BigDecimal("-0.1"));

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(response)
        );
    }

    @Test
    void confidenceAboveOneIsRejected() {
        AiInvestigationResponse response =
                new AiInvestigationResponse();

        response.setConfidence(new BigDecimal("1.1"));

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(response)
        );
    }

    @Test
    void missingConfidenceIsRejected() {
        AiInvestigationResponse response =
                new AiInvestigationResponse();

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(response)
        );
    }
}
