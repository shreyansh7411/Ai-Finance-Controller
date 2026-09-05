package com.aifincontroller.service;

import com.aifincontroller.ai.domain.AiInvestigationRecord;
import com.aifincontroller.ai.dto.AiInvestigationResponse;
import com.aifincontroller.ai.provider.AiProvider;
import com.aifincontroller.ai.repository.AiInvestigationRecordRepository;
import com.aifincontroller.domain.Payment;
import com.aifincontroller.domain.ReconciliationException;
import com.aifincontroller.domain.ReconciliationResult;
import com.aifincontroller.repository.AdjustmentRepository;
import com.aifincontroller.repository.MerchantOrderRepository;
import com.aifincontroller.repository.PaymentRepository;
import com.aifincontroller.repository.ReconciliationExceptionRepository;
import com.aifincontroller.repository.ReconciliationResultRepository;
import com.aifincontroller.repository.RefundRepository;
import com.aifincontroller.repository.SettlementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiInvestigationServiceTest {

    @Test
    void shouldBuildEvidenceAndDelegateToAiProvider() {

        ReconciliationExceptionRepository exceptionRepository =
                mock(ReconciliationExceptionRepository.class);

        ReconciliationResultRepository resultRepository =
                mock(ReconciliationResultRepository.class);

        PaymentRepository paymentRepository =
                mock(PaymentRepository.class);

        MerchantOrderRepository orderRepository =
                mock(MerchantOrderRepository.class);

        SettlementRepository settlementRepository =
                mock(SettlementRepository.class);

        RefundRepository refundRepository =
                mock(RefundRepository.class);

        AdjustmentRepository adjustmentRepository =
                mock(AdjustmentRepository.class);

        AiInvestigationRecordRepository investigationRepository =
                mock(AiInvestigationRecordRepository.class);

        ObjectMapper objectMapper = new ObjectMapper();

        ReconciliationException exception =
                new ReconciliationException();

        exception.setId(1L);
        exception.setReconciliationResultId(10L);
        exception.setType("MISSING_SETTLEMENT");
        exception.setCategory("MISSING_SETTLEMENT");
        exception.setSeverity("HIGH");
        exception.setExpectedAmount(
                new BigDecimal("100.00"));
        exception.setActualAmount(
                BigDecimal.ZERO);
        exception.setDifference(
                new BigDecimal("100.00"));

        ReconciliationResult result =
                new ReconciliationResult();

        result.setId(10L);
        result.setPaymentReference("pay_123");
        result.setExpectedAmount(
                new BigDecimal("100.00"));
        result.setActualAmount(
                BigDecimal.ZERO);
        result.setDifference(
                new BigDecimal("100.00"));

        Payment payment =
                new Payment();

        payment.setPaymentId("pay_123");
        payment.setOrderId("order_123");
        payment.setAmount(
                new BigDecimal("100.00"));
        payment.setCurrency("INR");
        payment.setStatus("CAPTURED");

        when(investigationRepository.findByExceptionId(1L))
                .thenReturn(Optional.empty());

        when(exceptionRepository.findById(1L))
                .thenReturn(Optional.of(exception));

        when(resultRepository.findById(10L))
                .thenReturn(Optional.of(result));

        when(paymentRepository.findByPaymentId("pay_123"))
                .thenReturn(Optional.of(payment));

        when(orderRepository.findByOrderId("order_123"))
                .thenReturn(Optional.empty());

        when(settlementRepository.findByPaymentId("pay_123"))
                .thenReturn(List.of());

        when(refundRepository.findByPaymentId("pay_123"))
                .thenReturn(List.of());

        when(adjustmentRepository.findBySettlementId(any(String.class)))
                .thenReturn(List.of());

        AiInvestigationResponse expectedResponse =
                new AiInvestigationResponse();

        expectedResponse.setConclusion(
                "The payment has no corresponding settlement.");

        expectedResponse.setExplanation(
                "The payment was captured for INR 100, but no settlement record is available.");

        expectedResponse.setWhatHappened(
                "The payment was captured, but a corresponding settlement record is not present.");

        expectedResponse.setRootCause(
                "The settlement record is missing from the available financial records.");

        expectedResponse.setFinancialImpact(
                "INR 100 remains unreconciled because the expected settlement is not present.");

        expectedResponse.setSupportingEvidence(
                List.of(
                        "The payment record shows a captured payment of INR 100.",
                        "No settlement record was found for the payment."
                ));

        expectedResponse.setAlternativeExplanations(
                List.of(
                        "The settlement may have been recorded outside the available settlement data."
                ));

        expectedResponse.setMissingEvidence(
                List.of(
                        "Settlement record"
                ));

        expectedResponse.setConfidenceReasoning(
                "The payment is directly confirmed, but the absence of settlement data prevents confirmation of the final settlement state.");

        expectedResponse.setRecommendedAction(
                "Check the provider settlement records and confirm whether the payment was settled.");

        expectedResponse.setEvidenceReferences(
                List.of(
                        "EXCEPTION_EXPECTED_AMOUNT",
                        "PAYMENT_ID"
                ));

        expectedResponse.setConfidence(
                new BigDecimal("0.90"));

        expectedResponse.setRecommendedStatus(
                "INVESTIGATING");

        AiProvider aiProvider = request -> {

            assertEquals(
                    1L,
                    request.getExceptionId());

            assertEquals(
                    "MISSING_SETTLEMENT",
                    request.getExceptionType());

            assertEquals(
                    "pay_123",
                    request.getPaymentReference());

            assertTrue(
                    request.getEvidenceIds()
                            .contains("PAYMENT_ID"));

            assertNotNull(
                    request.getFinancialAnalysis());

            assertEquals(
                    new BigDecimal("100.0000"),
                    request.getFinancialAnalysis()
                            .getExpectedAmount());

            assertEquals(
                    new BigDecimal("100.0000"),
                    request.getFinancialAnalysis()
                            .getReconciliationDifference());

            assertTrue(
                    request.getFinancialAnalysis()
                            .isPaymentMatchesExpected());

            assertTrue(
                    !request.getFinancialAnalysis()
                            .isSettlementPresent());

            assertTrue(
                    request.getFinancialAnalysis()
                            .getMissingEvidence()
                            .contains("Settlement record"));

            return expectedResponse;
        };

        FinancialAnalysisService financialAnalysisService =
                new FinancialAnalysisService(
                        exceptionRepository,
                        resultRepository,
                        paymentRepository,
                        orderRepository,
                        settlementRepository,
                        refundRepository,
                        adjustmentRepository);

        AiInvestigationEvidenceService evidenceService =
                new AiInvestigationEvidenceService(
                        exceptionRepository,
                        resultRepository,
                        paymentRepository,
                        orderRepository,
                        settlementRepository,
                        refundRepository,
                        adjustmentRepository,
                        financialAnalysisService);

        AiInvestigationService service =
                new AiInvestigationService(
                        evidenceService,
                        aiProvider,
                        investigationRepository,
                        objectMapper);

        AiInvestigationResponse actual =
                service.investigate(1L);

        assertEquals(
                expectedResponse.getConclusion(),
                actual.getConclusion());

        assertEquals(
                expectedResponse.getWhatHappened(),
                actual.getWhatHappened());

        assertEquals(
                expectedResponse.getRootCause(),
                actual.getRootCause());

        assertEquals(
                expectedResponse.getFinancialImpact(),
                actual.getFinancialImpact());

        assertEquals(
                expectedResponse.getSupportingEvidence(),
                actual.getSupportingEvidence());

        assertEquals(
                expectedResponse.getAlternativeExplanations(),
                actual.getAlternativeExplanations());

        assertEquals(
                expectedResponse.getMissingEvidence(),
                actual.getMissingEvidence());

        assertEquals(
                expectedResponse.getConfidenceReasoning(),
                actual.getConfidenceReasoning());

        assertEquals(
                expectedResponse.getRecommendedAction(),
                actual.getRecommendedAction());

        assertEquals(
                expectedResponse.getEvidenceReferences(),
                actual.getEvidenceReferences());

        assertEquals(
                expectedResponse.getConfidence(),
                actual.getConfidence());

        assertEquals(
                expectedResponse.getRecommendedStatus(),
                actual.getRecommendedStatus());

        verify(investigationRepository)
                .save(any(AiInvestigationRecord.class));
    }

    @Test
    void shouldRejectNullExceptionId() {

        AiInvestigationService service =
                new AiInvestigationService(
                        mock(AiInvestigationEvidenceService.class),
                        mock(AiProvider.class),
                        mock(AiInvestigationRecordRepository.class),
                        new ObjectMapper());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.investigate(null));

        assertEquals(
                "Exception ID is required",
                exception.getMessage());
    }
}
