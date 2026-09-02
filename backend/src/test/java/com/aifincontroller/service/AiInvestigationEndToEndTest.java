package com.aifincontroller.service;

import com.aifincontroller.ai.dto.AiInvestigationRequest;
import com.aifincontroller.ai.dto.AiInvestigationResponse;
import com.aifincontroller.ai.provider.AiProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true"
})
class AiInvestigationEndToEndTest {

    @Autowired
    private AiInvestigationEvidenceService evidenceService;

    @Autowired
    private AiInvestigationService investigationService;

    @MockBean
    private AiProvider aiProvider;

    @Test
    void shouldCompleteInvestigationFlow() {

        Long exceptionId = 1679L;

        AiInvestigationRequest request =
                new AiInvestigationRequest();

        request.setExceptionId(exceptionId);
        request.setExceptionType("FEE_DIFFERENCE");
        request.setCategory("AMOUNT_MISMATCH");
        request.setSeverity("MEDIUM");
        request.setPaymentReference("pay_test");
        request.setExpectedAmount(
                new BigDecimal("100.0000"));
        request.setActualAmount(
                new BigDecimal("97.6400"));
        request.setDifference(
                new BigDecimal("2.3600"));
        request.setEvidenceSummary(
                "Settlement amount differs due to fees and tax.");

        AiInvestigationResponse response =
                new AiInvestigationResponse();

        response.setConclusion(
                "The discrepancy is explained by settlement fees and tax.");

        response.setExplanation(
                "The payment amount is 100 INR while the settlement "
                        + "amount is reduced by 2 INR fees and 0.36 INR tax.");

        response.setEvidenceReferences(
                List.of(
                        "payment.amount",
                        "settlement.fees",
                        "settlement.tax"
                ));

        response.setConfidence(
                new BigDecimal("0.92"));

        response.setRecommendedStatus(
                "INVESTIGATING");

        when(aiProvider.investigate(any(AiInvestigationRequest.class)))
                .thenReturn(response);

        AiInvestigationResponse result =
                investigationService.investigate(exceptionId);

        assertThat(result).isNotNull();

        assertThat(result.getConclusion())
                .isEqualTo(
                        "The discrepancy is explained by settlement fees and tax.");

        assertThat(result.getExplanation())
                .contains("2 INR fees");

        assertThat(result.getEvidenceReferences())
                .containsExactly(
                        "payment.amount",
                        "settlement.fees",
                        "settlement.tax"
                );

        assertThat(result.getConfidence())
                .isEqualByComparingTo("0.92");

        assertThat(result.getRecommendedStatus())
                .isEqualTo("INVESTIGATING");

        verify(aiProvider, times(1))
                .investigate(any(AiInvestigationRequest.class));

        verifyNoMoreInteractions(aiProvider);
    }
}
