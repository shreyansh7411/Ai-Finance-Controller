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

        if (response.getConclusion() == null ||
                response.getConclusion().isBlank()) {

            throw new AiProviderException(
                    "AI investigation conclusion is missing"
            );
        }

        if (response.getExplanation() == null ||
                response.getExplanation().isBlank()) {

            throw new AiProviderException(
                    "AI investigation explanation is missing"
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
}
