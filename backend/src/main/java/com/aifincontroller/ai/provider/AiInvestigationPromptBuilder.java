package com.aifincontroller.ai.provider;

import com.aifincontroller.ai.dto.AiInvestigationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class AiInvestigationPromptBuilder {

    private final ObjectMapper objectMapper;

    public AiInvestigationPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String build(AiInvestigationRequest request) {

        try {
            String evidenceJson =
                    objectMapper.writeValueAsString(request);

            return """
                    You are a financial reconciliation investigator.

                    Your task is to investigate the reconciliation exception
                    using ONLY the evidence supplied by the backend.

                    STRICT RULES:

                    1. Do not invent, assume, estimate, or infer financial
                       facts that are not directly supported by the supplied
                       evidence.

                    2. Do not modify, request modification of, or propose
                       changes to source financial records.

                    3. Every factual conclusion must be supported by the
                       supplied evidence.

                    4. Every evidence reference must use an EXACT stable
                       evidence ID from the backend-supplied evidenceIds list.

                    5. Never create, alter, abbreviate, or guess an evidence ID.

                    6. If the supplied evidence is insufficient to determine
   the cause of the exception, use recommendedStatus
   INSUFFICIENT_EVIDENCE.

   When recommendedStatus is INSUFFICIENT_EVIDENCE:
   - The conclusion MUST explicitly state that the evidence
     is insufficient.
   - The explanation MUST explicitly state that the evidence
     is insufficient.
   - Explain what information is missing, conflicting, or
     ambiguous.
   - Do not claim that a root cause has been established.
   - Do not recommend RESOLVED.

7. Confidence must be a number between 0 and 1.

                    8. recommendedStatus must be exactly one of:
                       INVESTIGATING,
                       RESOLVED,
                       IGNORED,
                       INSUFFICIENT_EVIDENCE.

                    9. Do not return OPEN as a recommended status.

                    10. Return ONLY valid JSON.
                        Do not return markdown.
                        Do not return code fences.
                        Do not return explanatory text outside the JSON.

                    REQUIRED JSON FORMAT:

                    {
                      "conclusion": "short evidence-grounded conclusion",
                      "explanation": "evidence-grounded explanation",
                      "evidenceReferences": [
                        "EXACT_STABLE_EVIDENCE_ID"
                      ],
                      "confidence": 0.0,
                      "recommendedStatus": "INVESTIGATING"
                    }

                    IMPORTANT:

The evidenceReferences array must contain only IDs that
appear exactly in the backend-supplied evidenceIds list.

Examples of valid evidence IDs include:
PAYMENT_AMOUNT
PAYMENT_ORDER_ID
SETTLEMENT_AMOUNT
SETTLEMENT_FEES
SETTLEMENT_TAX
REFUND_AMOUNT
ADJUSTMENT_AMOUNT

If recommending INSUFFICIENT_EVIDENCE, the response must
explicitly state in both the conclusion and explanation
that the evidence is insufficient. It must explain what
information is missing, conflicting, or ambiguous and must
not claim that the root cause has been established.

Example:

{
  "conclusion": "The available evidence is insufficient to determine the cause of this exception.",
  "explanation": "The available evidence is insufficient because the records do not provide enough information to distinguish between the possible causes.",
  "evidenceReferences": ["EXACT_STABLE_EVIDENCE_ID"],
  "confidence": 0.4,
  "recommendedStatus": "INSUFFICIENT_EVIDENCE"
}

BACKEND-SUPPLIED INVESTIGATION EVIDENCE:

                    %s
                    """.formatted(evidenceJson);

        } catch (JsonProcessingException e) {

            throw new AiProviderException(
                    "Failed to serialize investigation evidence",
                    e
            );
        }
    }
}
