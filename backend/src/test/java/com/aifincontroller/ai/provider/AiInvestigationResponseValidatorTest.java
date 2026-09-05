package com.aifincontroller.ai.provider;

import com.aifincontroller.ai.dto.AiInvestigationRequest;
import com.aifincontroller.ai.dto.AiInvestigationResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiInvestigationResponseValidatorTest {

    private final AiInvestigationResponseValidator validator =
            new AiInvestigationResponseValidator(
                    new AiConfidenceValidator(),
                    new AiRecommendationValidator(),
                    new AiEvidenceReferenceValidator(),
                    new AiInvestigationConsistencyValidator()
            );

    private AiInvestigationRequest validRequest() {

        AiInvestigationRequest request =
                new AiInvestigationRequest();

        request.setEvidenceIds(
                List.of(
                        "PAYMENT_AMOUNT",
                        "SETTLEMENT_AMOUNT",
                        "SETTLEMENT_FEES",
                        "SETTLEMENT_TAX"
                )
        );

        return request;
    }

    private AiInvestigationResponse validResponse() {

        AiInvestigationResponse response =
                new AiInvestigationResponse();

        response.setConclusion(
                "Settlement amount differs from expected amount."
        );

        response.setExplanation(
                "The supplied settlement evidence shows a lower net amount."
        );

        response.setWhatHappened(
                "The payment was received for the expected amount, but the settlement amount was lower after applicable deductions."
        );

        response.setRootCause(
                "The difference is explained by settlement fees and taxes recorded in the supplied settlement evidence."
        );

        response.setFinancialImpact(
                "The merchant received a lower net settlement amount than the original payment amount."
        );

        response.setSupportingEvidence(
                List.of(
                        "The payment amount matches the expected payment amount.",
                        "The settlement amount is lower than the payment amount.",
                        "Settlement fees and taxes are present in the supplied evidence."
                )
        );

        response.setAlternativeExplanations(
                List.of(
                        "Additional deductions may exist if further settlement records are available."
                )
        );

        response.setMissingEvidence(
                List.of()
        );

        response.setConfidenceReasoning(
                "Confidence is high because the payment and settlement evidence directly supports the identified difference."
        );

        response.setRecommendedAction(
                "Review the settlement deductions and confirm that the recorded fees and taxes are expected."
        );

        response.setEvidenceReferences(
                List.of(
                        "SETTLEMENT_AMOUNT",
                        "PAYMENT_AMOUNT"
                )
        );

        response.setConfidence(
                new BigDecimal("0.85")
        );

        response.setRecommendedStatus("INVESTIGATING");

        return response;
    }

    @Test
    void validResponseIsAccepted() {
        assertDoesNotThrow(() ->
                validator.validate(
                        validRequest(),
                        validResponse()
                )
        );
    }

    @Test
    void nullResponseIsRejected() {
        assertThrows(
                AiProviderException.class,
                () -> validator.validate(
                        validRequest(),
                        null
                )
        );
    }

    @Test
    void missingConclusionIsRejected() {
        AiInvestigationResponse response = validResponse();
        response.setConclusion(null);

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(
                        validRequest(),
                        response
                )
        );
    }

    @Test
    void blankConclusionIsRejected() {
        AiInvestigationResponse response = validResponse();
        response.setConclusion("   ");

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(
                        validRequest(),
                        response
                )
        );
    }

    @Test
    void missingExplanationIsRejected() {
        AiInvestigationResponse response = validResponse();
        response.setExplanation(null);

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(
                        validRequest(),
                        response
                )
        );
    }

    @Test
    void missingEvidenceReferencesAreRejected() {
        AiInvestigationResponse response = validResponse();
        response.setEvidenceReferences(null);

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(
                        validRequest(),
                        response
                )
        );
    }

    @Test
    void emptyEvidenceReferencesAreRejected() {
        AiInvestigationResponse response = validResponse();
        response.setEvidenceReferences(List.of());

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(
                        validRequest(),
                        response
                )
        );
    }

    @Test
    void invalidConfidenceIsRejected() {
        AiInvestigationResponse response = validResponse();
        response.setConfidence(new BigDecimal("1.5"));

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(
                        validRequest(),
                        response
                )
        );
    }

    @Test
    void invalidRecommendationIsRejected() {
        AiInvestigationResponse response = validResponse();
        response.setRecommendedStatus("UNKNOWN");

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(
                        validRequest(),
                        response
                )
        );
    }

    @Test
    void unsupportedEvidenceReferenceIsRejected() {
        AiInvestigationResponse response = validResponse();

        response.setEvidenceReferences(
                List.of("FABRICATED_EVIDENCE_ID")
        );

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(
                        validRequest(),
                        response
                )
        );
    }
}
