package com.aifincontroller.service;

import com.aifincontroller.ai.dto.AiInvestigationRequest;
import com.aifincontroller.ai.dto.AiInvestigationResponse;
import com.aifincontroller.ai.provider.AiProvider;
import org.springframework.stereotype.Service;

@Service
public class AiInvestigationService {

    private final AiInvestigationEvidenceService evidenceService;
    private final AiProvider aiProvider;

    public AiInvestigationService(
            AiInvestigationEvidenceService evidenceService,
            AiProvider aiProvider) {

        this.evidenceService = evidenceService;
        this.aiProvider = aiProvider;
    }

    public AiInvestigationResponse investigate(Long exceptionId) {

        if (exceptionId == null) {
            throw new IllegalArgumentException(
                    "Exception ID is required"
            );
        }

        AiInvestigationRequest request =
                evidenceService.buildRequest(exceptionId);

        return aiProvider.investigate(request);
    }
}
