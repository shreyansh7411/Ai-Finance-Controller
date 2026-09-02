package com.aifincontroller.controller;

import com.aifincontroller.ai.dto.AiInvestigationResponse;
import com.aifincontroller.service.AiInvestigationService;
import com.aifincontroller.domain.AuditLog;
import com.aifincontroller.domain.ReconciliationException;
import com.aifincontroller.dto.ExceptionResolutionRequest;
import com.aifincontroller.dto.ExceptionStatusUpdateRequest;
import com.aifincontroller.repository.AuditLogRepository;
import com.aifincontroller.repository.ReconciliationExceptionRepository;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    private AuditLogRepository auditLogRepository;

    @MockBean
    private ExceptionResolutionService resolutionService;

    @MockBean
    private AiInvestigationService aiInvestigationService;

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
}
