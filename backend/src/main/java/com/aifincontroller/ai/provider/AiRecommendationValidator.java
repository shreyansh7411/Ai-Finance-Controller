package com.aifincontroller.ai.provider;

import com.aifincontroller.ai.dto.AiInvestigationResponse;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AiRecommendationValidator {

    private static final Set<String> VALID_STATUSES = Set.of(
            "INVESTIGATING",
            "RESOLVED",
            "IGNORED",
            "INSUFFICIENT_EVIDENCE"
    );

    public void validate(AiInvestigationResponse response) {

        if (response == null) {
            throw new AiProviderException(
                    "AI investigation response is null"
            );
        }

        String status = response.getRecommendedStatus();

        if (status == null || status.isBlank()) {
            throw new AiProviderException(
                    "AI recommended status is missing"
            );
        }

        String normalizedStatus = status.toUpperCase();

        if (!VALID_STATUSES.contains(normalizedStatus)) {
            throw new AiProviderException(
                    "Invalid AI recommended status: " + status
            );
        }

        response.setRecommendedStatus(normalizedStatus);
    }
}
