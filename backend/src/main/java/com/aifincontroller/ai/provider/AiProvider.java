package com.aifincontroller.ai.provider;

import com.aifincontroller.ai.dto.AiInvestigationRequest;
import com.aifincontroller.ai.dto.AiInvestigationResponse;

public interface AiProvider {

    AiInvestigationResponse investigate(
            AiInvestigationRequest request
    );
}
