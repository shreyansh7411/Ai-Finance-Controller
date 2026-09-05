package com.aifincontroller.ai.provider;

import com.aifincontroller.ai.dto.AiInvestigationRequest;
import com.aifincontroller.ai.dto.AiInvestigationResponse;
import org.springframework.stereotype.Component;

@Component
public class AiInvestigationResponseValidator {

    private final AiConfidenceValidator confidenceValidator;
    private final AiRecommendationValidator recommendationValidator;
    private final AiEvidenceReferenceValidator evidenceReferenceValidator;
    private final AiInvestigationConsistencyValidator consistencyValidator;

    public AiInvestigationResponseValidator(
            AiConfidenceValidator confidenceValidator,
            AiRecommendationValidator recommendationValidator,
            AiEvidenceReferenceValidator evidenceReferenceValidator,
            AiInvestigationConsistencyValidator consistencyValidator) {

        this.confidenceValidator = confidenceValidator;
        this.recommendationValidator = recommendationValidator;
        this.evidenceReferenceValidator = evidenceReferenceValidator;
        this.consistencyValidator = consistencyValidator;
    }

    public void validate(
            AiInvestigationRequest request,
            AiInvestigationResponse response) {

        if (response == null) {
            throw new AiProviderException(
                    "AI investigation response is null"
            );
        }

        requireText(
                response.getConclusion(),
                "AI investigation conclusion is missing"
        );

        requireText(
                response.getExplanation(),
                "AI investigation explanation is missing"
        );

        requireText(
                response.getWhatHappened(),
                "AI investigation whatHappened is missing"
        );

        requireText(
                response.getRootCause(),
                "AI investigation rootCause is missing"
        );

        requireText(
                response.getFinancialImpact(),
                "AI investigation financialImpact is missing"
        );

        requireText(
                response.getConfidenceReasoning(),
                "AI investigation confidenceReasoning is missing"
        );

        requireText(
                response.getRecommendedAction(),
                "AI investigation recommendedAction is missing"
        );

        if (response.getSupportingEvidence() == null) {
            throw new AiProviderException(
                    "AI investigation supportingEvidence is missing"
            );
        }

        if (response.getAlternativeExplanations() == null) {
            throw new AiProviderException(
                    "AI investigation alternativeExplanations is missing"
            );
        }

        if (response.getMissingEvidence() == null) {
            throw new AiProviderException(
                    "AI investigation missingEvidence is missing"
            );
        }

        if (response.getEvidenceReferences() == null ||
                response.getEvidenceReferences().isEmpty()) {

            throw new AiProviderException(
                    "AI investigation evidence references are missing"
            );
        }

        confidenceValidator.validate(response);

        recommendationValidator.validate(response);

        evidenceReferenceValidator.validate(
                request,
                response
        );

        consistencyValidator.validate(
                request,
                response
        );
    }

    private void requireText(
            String value,
            String message) {

        if (value == null || value.isBlank()) {
            throw new AiProviderException(message);
        }
    }
}
