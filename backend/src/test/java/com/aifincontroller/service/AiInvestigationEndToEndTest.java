package com.aifincontroller.service;

import com.aifincontroller.ai.dto.AiInvestigationResponse;
import com.aifincontroller.ai.provider.AiProvider;
import com.aifincontroller.domain.Payment;
import com.aifincontroller.domain.ReconciliationException;
import com.aifincontroller.domain.ReconciliationResult;
import com.aifincontroller.repository.PaymentRepository;
import com.aifincontroller.repository.ReconciliationExceptionRepository;
import com.aifincontroller.repository.ReconciliationResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true"
})
class AiInvestigationEndToEndTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ReconciliationResultRepository resultRepository;

    @Autowired
    private ReconciliationExceptionRepository exceptionRepository;

    @Autowired
    private AiInvestigationService investigationService;

    @MockBean
    private AiProvider aiProvider;

    @Test
    void shouldCompleteInvestigationFlow() {

        Payment payment = new Payment();
        payment.setPaymentId("pay_test");
        payment.setBatchId("batch_test");
        payment.setOrderId("order_test");
        payment.setAmount(new BigDecimal("100.0000"));
        payment.setCurrency("INR");
        payment.setStatus("captured");

        payment = paymentRepository.save(payment);

        ReconciliationResult result = new ReconciliationResult();
        result.setBatchId("batch_test");
        result.setPaymentReference(payment.getPaymentId());
        result.setMatchedRecord("settlement_test");
        result.setMatchType("FEE_DIFFERENCE");
        result.setExpectedAmount(new BigDecimal("100.0000"));
        result.setActualAmount(new BigDecimal("97.6400"));
        result.setDifference(new BigDecimal("2.3600"));
        result.setStatus("EXCEPTION");
        result.setConfidenceScore(new BigDecimal("0.90"));

        result = resultRepository.save(result);

        ReconciliationException exception =
                new ReconciliationException();

        exception.setReconciliationResultId(result.getId());
        exception.setType("FEE_DIFFERENCE");
        exception.setCategory("AMOUNT_MISMATCH");
        exception.setSeverity("MEDIUM");
        exception.setStatus("OPEN");
        exception.setExpectedAmount(new BigDecimal("100.0000"));
        exception.setActualAmount(new BigDecimal("97.6400"));
        exception.setDifference(new BigDecimal("2.3600"));
        exception.setSourceReference("pay_test");
        exception.setCandidateRecord("settlement_test");
        exception.setEvidenceSummary(
                "Settlement amount differs due to fees and tax.");

        exception = exceptionRepository.save(exception);

        AiInvestigationResponse response =
                new AiInvestigationResponse();

        response.setConclusion(
                "The discrepancy is explained by settlement fees and tax.");

        response.setExplanation(
                "The payment amount is 100 INR while the settlement "
                        + "amount is reduced by 2 INR fees and 0.36 INR tax.");

        response.setEvidenceReferences(
                java.util.List.of(
                        "payment.amount",
                        "settlement.fees",
                        "settlement.tax"
                ));

        response.setConfidence(
                new BigDecimal("0.92"));

        response.setRecommendedStatus(
                "INVESTIGATING");

        when(aiProvider.investigate(any()))
                .thenReturn(response);

        AiInvestigationResponse investigationResult =
                investigationService.investigate(exception.getId());

        assertThat(investigationResult).isNotNull();

        assertThat(investigationResult.getConclusion())
                .isEqualTo(
                        "The discrepancy is explained by settlement fees and tax.");

        assertThat(investigationResult.getExplanation())
                .contains("2 INR fees");

        assertThat(investigationResult.getEvidenceReferences())
                .containsExactly(
                        "payment.amount",
                        "settlement.fees",
                        "settlement.tax"
                );

        assertThat(investigationResult.getConfidence())
                .isEqualByComparingTo("0.92");

        assertThat(investigationResult.getRecommendedStatus())
                .isEqualTo("INVESTIGATING");

        verify(aiProvider, times(1))
                .investigate(any());

        verifyNoMoreInteractions(aiProvider);
    }
}
