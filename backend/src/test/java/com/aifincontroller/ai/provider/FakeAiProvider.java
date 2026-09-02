package com.aifincontroller.ai.provider;

import com.aifincontroller.ai.dto.AiInvestigationRequest;
import com.aifincontroller.ai.dto.AiInvestigationResponse;

import java.math.BigDecimal;
import java.util.List;

public class FakeAiProvider implements AiProvider {

    private AiInvestigationResponse response;

    @Override
    public AiInvestigationResponse investigate(
            AiInvestigationRequest request) {

        if (response != null) {
            return response;
        }

        AiInvestigationResponse defaultResponse =
                new AiInvestigationResponse();

        defaultResponse.setConclusion(
                "Investigation completed using supplied evidence."
        );

        defaultResponse.setExplanation(
                "The conclusion is based only on backend-supplied evidence."
        );

        defaultResponse.setEvidenceReferences(
                request.getEvidenceIds() == null
                        ? List.of()
                        : List.of(request.getEvidenceIds().get(0))
        );

        defaultResponse.setConfidence(
                new BigDecimal("0.90")
        );

        defaultResponse.setRecommendedStatus(
                "INVESTIGATING"
        );

        return defaultResponse;
    }

    public void setResponse(
            AiInvestigationResponse response) {
        this.response = response;
    }
}
