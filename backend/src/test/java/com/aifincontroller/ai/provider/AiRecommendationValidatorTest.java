package com.aifincontroller.ai.provider;

import com.aifincontroller.ai.dto.AiInvestigationResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiRecommendationValidatorTest {

    private final AiRecommendationValidator validator =
            new AiRecommendationValidator();

    @Test
    void allSupportedStatusesAreAccepted() {

        String[] statuses = {
                "INVESTIGATING",
                "RESOLVED",
                "IGNORED",
                "INSUFFICIENT_EVIDENCE"
        };

        for (String status : statuses) {

            AiInvestigationResponse response =
                    validResponse();

            response.setRecommendedStatus(status);

            assertDoesNotThrow(
                    () -> validator.validate(response)
            );

            assertThat(response.getRecommendedStatus())
                    .isEqualTo(status);
        }
    }

    @Test
    void lowercaseStatusIsNormalized() {

        AiInvestigationResponse response =
                validResponse();

        response.setRecommendedStatus("resolved");

        assertDoesNotThrow(
                () -> validator.validate(response)
        );

        assertThat(response.getRecommendedStatus())
                .isEqualTo("RESOLVED");
    }

    @Test
    void missingStatusIsRejected() {

        AiInvestigationResponse response =
                validResponse();

        response.setRecommendedStatus(null);

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(response)
        );
    }

    @Test
    void blankStatusIsRejected() {

        AiInvestigationResponse response =
                validResponse();

        response.setRecommendedStatus(" ");

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(response)
        );
    }

    @Test
    void invalidStatusIsRejected() {

        AiInvestigationResponse response =
                validResponse();

        response.setRecommendedStatus("OPEN");

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(response)
        );
    }

    private AiInvestigationResponse validResponse() {

        AiInvestigationResponse response =
                new AiInvestigationResponse();

        response.setConclusion("Supported conclusion");
        response.setExplanation("Supported explanation");
        response.setEvidenceReferences(
                java.util.List.of("PAYMENT_AMOUNT")
        );
        response.setConfidence(
                new BigDecimal("0.90")
        );
        response.setRecommendedStatus("INVESTIGATING");

        return response;
    }
}
