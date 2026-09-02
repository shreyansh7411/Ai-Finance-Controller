package com.aifincontroller.ai.provider;

import com.aifincontroller.ai.dto.AiInvestigationRequest;
import com.aifincontroller.ai.dto.AiInvestigationResponse;
import org.springframework.stereotype.Component;

@Component
public class AiInvestigationConsistencyValidator {

    public void validate(
            AiInvestigationRequest request,
            AiInvestigationResponse response) {

        if (request == null) {
            throw new AiProviderException(
                    "Investigation request is null"
            );
        }

        if (response == null) {
            throw new AiProviderException(
                    "AI investigation response is null"
            );
        }

        if (response.getEvidenceReferences() == null ||
                response.getEvidenceReferences().isEmpty()) {

            throw new AiProviderException(
                    "AI investigation must contain evidence references"
            );
        }

        if ("INSUFFICIENT_EVIDENCE".equals(
                response.getRecommendedStatus())) {

            String explanation =
                    response.getExplanation();

            if (explanation == null ||
                    explanation.isBlank()) {

                throw new AiProviderException(
                        "Insufficient-evidence response must contain an explanation"
                );
            }

            String normalized =
                    explanation.toLowerCase();

            if (!normalized.contains("insufficient") &&
                    !normalized.contains("not enough") &&
                    !normalized.contains("cannot determine") &&
                    !normalized.contains("unable to determine")) {

                throw new AiProviderException(
                        "INSUFFICIENT_EVIDENCE recommendation must explicitly "
                                + "state that the evidence is insufficient"
                );
            }
        }

        if ("RESOLVED".equals(
                response.getRecommendedStatus()) &&
                response.getEvidenceReferences().isEmpty()) {

            throw new AiProviderException(
                    "AI cannot recommend RESOLVED without evidence"
            );
        }
    }
}
