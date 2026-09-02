package com.aifincontroller.ai.provider;

import com.aifincontroller.ai.dto.AiInvestigationRequest;
import com.aifincontroller.ai.dto.AiInvestigationResponse;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AiEvidenceReferenceValidator {

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

        List<String> references =
                response.getEvidenceReferences();

        if (references == null || references.isEmpty()) {
            throw new AiProviderException(
                    "AI investigation evidence references are missing"
            );
        }

        List<String> evidenceIds =
                request.getEvidenceIds();

        if (evidenceIds == null || evidenceIds.isEmpty()) {
            throw new AiProviderException(
                    "Backend investigation evidence IDs are missing"
            );
        }

        Set<String> availableEvidence =
                new HashSet<>(evidenceIds);

        for (String reference : references) {

            if (reference == null || reference.isBlank()) {
                throw new AiProviderException(
                        "AI returned an empty evidence reference"
                );
            }

            if (!availableEvidence.contains(reference)) {
                throw new AiProviderException(
                        "Unsupported AI evidence reference: " + reference
                );
            }
        }
    }
}
