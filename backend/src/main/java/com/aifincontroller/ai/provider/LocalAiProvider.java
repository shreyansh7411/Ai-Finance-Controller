package com.aifincontroller.ai.provider;

import com.aifincontroller.ai.dto.AiInvestigationRequest;
import com.aifincontroller.ai.dto.AiInvestigationResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConditionalOnProperty(
        name = "ai.provider",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalAiProvider implements AiProvider {

    @Override
    public AiInvestigationResponse investigate(
            AiInvestigationRequest request) {

        if (request == null) {
            throw new AiProviderException(
                    "Investigation request is null"
            );
        }

        if (request.getEvidenceIds() == null ||
                request.getEvidenceIds().isEmpty()) {

            throw new AiProviderException(
                    "Backend investigation evidence IDs are missing"
            );
        }

        AiInvestigationResponse response =
                new AiInvestigationResponse();

        response.setConclusion(buildConclusion(request));
        response.setExplanation(buildExplanation(request));
        response.setEvidenceReferences(
                List.of(request.getEvidenceIds().get(0))
        );
        response.setConfidence(
                request.getExceptionType() == null
                        ? BigDecimal.valueOf(0.70)
                        : BigDecimal.valueOf(0.85)
        );
        response.setRecommendedStatus(
                "INVESTIGATING"
        );

        return response;
    }

    private String buildConclusion(
            AiInvestigationRequest request) {

        String category = request.getCategory();

        if (category == null || category.isBlank()) {
            category = request.getExceptionType();
        }

        if (category == null || category.isBlank()) {
            return "Investigation requires further review.";
        }

        return "Investigation identified a " +
                category +
                " reconciliation exception.";
    }

    private String buildExplanation(
            AiInvestigationRequest request) {

        StringBuilder explanation = new StringBuilder();

        explanation.append(
                "The investigation was generated from the "
                        + "backend reconciliation evidence."
        );

        if (request.getExpectedAmount() != null) {
            explanation.append(
                    " Expected amount: "
                            + request.getExpectedAmount()
                            + "."
            );
        }

        if (request.getActualAmount() != null) {
            explanation.append(
                    " Actual amount: "
                            + request.getActualAmount()
                            + "."
            );
        }

        if (request.getDifference() != null) {
            explanation.append(
                    " Difference: "
                            + request.getDifference()
                            + "."
            );
        }

        explanation.append(
                " The available evidence supports continued "
                        + "investigation rather than automatically "
                        + "resolving the exception."
        );

        return explanation.toString();
    }
}
