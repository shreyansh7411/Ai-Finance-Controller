package com.aifincontroller.ai.provider;

import com.aifincontroller.ai.dto.AiInvestigationResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AiConfidenceValidator {

    public void validate(AiInvestigationResponse response) {

        if (response == null) {
            throw new AiProviderException(
                    "AI investigation response is null"
            );
        }

        BigDecimal confidence = response.getConfidence();

        if (confidence == null) {
            throw new AiProviderException(
                    "AI investigation confidence is missing"
            );
        }

        if (confidence.compareTo(BigDecimal.ZERO) < 0 ||
                confidence.compareTo(BigDecimal.ONE) > 0) {
            throw new AiProviderException(
                    "AI investigation confidence must be between 0 and 1"
            );
        }
    }
}
