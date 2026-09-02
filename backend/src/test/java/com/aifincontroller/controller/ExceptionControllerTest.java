package com.aifincontroller.controller;

import com.aifincontroller.ai.dto.AiInvestigationResponse;
import com.aifincontroller.domain.AuditLog;
import com.aifincontroller.domain.DecisionRecord;
import com.aifincontroller.domain.ReconciliationException;
import com.aifincontroller.dto.ExceptionResolutionRequest;
import com.aifincontroller.dto.ExceptionStatusUpdateRequest;
import com.aifincontroller.repository.AuditLogRepository;
import com.aifincontroller.repository.DecisionRecordRepository;
import com.aifincontroller.repository.ReconciliationExceptionRepository;
import com.aifincontroller.service.AiInvestigationService;
import com.aifincontroller.service.DecisionService;
import com.aifincontroller.service.ExceptionResolutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExceptionController.class)
class ExceptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReconciliationExceptionRepository exceptionRepository;

    @MockBean
    private DecisionRecordRepository decisionRepository;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @MockBean
    private ExceptionResolutionService resolutionService;

    @MockBean
    private AiInvestigationService aiInvestigationService;

    @MockBean
    private DecisionService decisionService;

    @Test
    void shouldInvestigateException() throws Exception {

        AiInvestigationResponse response =
                new AiInvestigationResponse();

        response.setConclusion(
                "Settlement amount is lower than expected."
        );

        response.setExplanation(
                "The settlement contains fees and tax."
        );

        response.setEvidenceReferences(
                List.of(
                        "payment.amount",
                        "settlement.amount",
                        "settlement.fees",
                        "settlement.tax"
                )
        );

        response.setConfidence(
                new BigDecimal("0.92")
        );

        response.setRecommendedStatus("INVESTIGATING");

        when(aiInvestigationService.investigate(1679L))
                .thenReturn(response);

        mockMvc.perform(
                        post(
                                "/api/v1/reconciliation/exceptions/1679/investigate"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.conclusion")
                                .value(
                                        "Settlement amount is lower than expected."
                                )
                )
                .andExpect(
                        jsonPath("$.confidence")
                                .value(0.92)
                )
                .andExpect(
                        jsonPath("$.recommendedStatus")
                                .value("INVESTIGATING")
                )
                .andExpect(
                        jsonPath("$.evidenceReferences[0]")
                                .value("payment.amount")
                );

        verify(aiInvestigationService)
                .investigate(1679L);
    }

    @Test
    void shouldDecideException() throws Exception {

        AiInvestigationResponse investigation =
                new AiInvestigationResponse();

        investigation.setConclusion(
                "Settlement discrepancy is explained by fees."
        );

        investigation.setExplanation(
                "The settlement fee evidence explains the difference."
        );

        investigation.setEvidenceReferences(
                List.of(
                        "EXCEPTION_DIFFERENCE",
                        "SETTLEMENT_FEES"
                )
        );

        investigation.setConfidence(
                new BigDecimal("0.95")
        );

        investigation.setRecommendedStatus("RESOLVED");

        DecisionRecord decision =
                new DecisionRecord();

        decision.setExceptionId(1679L);
        decision.setOutcome("AUTO_RESOLVE");
        decision.setConfidence(
                new BigDecimal("0.95")
        );
        decision.setReason(
                "High-confidence AI investigation recommended resolution."
        );
        decision.setStatus("AUTO_RESOLVE");

        when(decisionService.processDecision(
                any(Long.class),
                any(AiInvestigationResponse.class)
        )).thenReturn(decision);

        mockMvc.perform(
                        post(
                                "/api/v1/reconciliation/exceptions/1679/decide"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conclusion": "Settlement discrepancy is explained by fees.",
                                  "explanation": "The settlement fee evidence explains the difference.",
                                  "evidenceReferences": [
                                    "EXCEPTION_DIFFERENCE",
                                    "SETTLEMENT_FEES"
                                  ],
                                  "confidence": 0.95,
                                  "recommendedStatus": "RESOLVED"
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.exceptionId")
                                .value(1679)
                )
                .andExpect(
                        jsonPath("$.outcome")
                                .value("AUTO_RESOLVE")
                )
                .andExpect(
                        jsonPath("$.confidence")
                                .value(0.95)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("AUTO_RESOLVE")
                );

        verify(decisionService)
                .processDecision(
                        any(Long.class),
                        any(AiInvestigationResponse.class)
                );
    }

    @Test
    void shouldGetDecision() throws Exception {

        DecisionRecord decision =
                new DecisionRecord();

        decision.setExceptionId(1679L);
        decision.setOutcome("AUTO_RESOLVE");
        decision.setConfidence(
                new BigDecimal("0.95")
        );
        decision.setReason(
                "High-confidence AI investigation recommended resolution."
        );
        decision.setStatus("AUTO_RESOLVE");

        when(decisionRepository.findByExceptionId(1679L))
                .thenReturn(Optional.of(decision));

        mockMvc.perform(
                        get(
                                "/api/v1/reconciliation/exceptions/1679/decision"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.exceptionId")
                                .value(1679)
                )
                .andExpect(
                        jsonPath("$.outcome")
                                .value("AUTO_RESOLVE")
                )
                .andExpect(
                        jsonPath("$.confidence")
                                .value(0.95)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("AUTO_RESOLVE")
                );

        verify(decisionRepository)
                .findByExceptionId(1679L);
    }

    @Test
    void shouldReturnNotFoundWhenDecisionDoesNotExist()
            throws Exception {

        when(decisionRepository.findByExceptionId(1679L))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        get(
                                "/api/v1/reconciliation/exceptions/1679/decision"
                        )
                )
                .andExpect(status().isNotFound());

        verify(decisionRepository)
                .findByExceptionId(1679L);
    }
}

