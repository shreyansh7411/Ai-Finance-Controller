package com.aifincontroller.service;

import com.aifincontroller.ai.dto.FinancialAnalysis;
import com.aifincontroller.domain.MerchantOrder;
import com.aifincontroller.domain.Payment;
import com.aifincontroller.domain.ReconciliationException;
import com.aifincontroller.domain.ReconciliationResult;
import com.aifincontroller.domain.Settlement;
import com.aifincontroller.repository.AdjustmentRepository;
import com.aifincontroller.repository.MerchantOrderRepository;
import com.aifincontroller.repository.PaymentRepository;
import com.aifincontroller.repository.ReconciliationExceptionRepository;
import com.aifincontroller.repository.ReconciliationResultRepository;
import com.aifincontroller.repository.RefundRepository;
import com.aifincontroller.repository.SettlementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialAnalysisServiceTest {

        @Mock
        private ReconciliationExceptionRepository exceptionRepository;

        @Mock
        private ReconciliationResultRepository resultRepository;

        @Mock
        private PaymentRepository paymentRepository;

        @Mock
        private MerchantOrderRepository orderRepository;

        @Mock
        private SettlementRepository settlementRepository;

        @Mock
        private RefundRepository refundRepository;

        @Mock
        private AdjustmentRepository adjustmentRepository;

        @InjectMocks
        private FinancialAnalysisService financialAnalysisService;

        @Test
        void shouldIdentifyFullyExplainedTaxDifference() {

                ReconciliationException exception = exception(5303L, 1L);

                ReconciliationResult result = result(
                                1L,
                                "pay_demo_0969",
                                2291.86,
                                2281.86,
                                10.00);

                Payment payment = payment(
                                "pay_demo_0969",
                                "order_demo_0969",
                                2291.86);

                MerchantOrder order = order(
                                "order_demo_0969",
                                2291.86);

                Settlement settlement = settlement(
                                "set_demo_0969",
                                "pay_demo_0969",
                                2291.86,
                                0.00,
                                10.00);

                when(exceptionRepository.findById(5303L))
                                .thenReturn(Optional.of(exception));

                when(resultRepository.findById(1L))
                                .thenReturn(Optional.of(result));

                when(paymentRepository.findByPaymentId("pay_demo_0969"))
                                .thenReturn(Optional.of(payment));

                when(orderRepository.findByOrderId("order_demo_0969"))
                                .thenReturn(Optional.of(order));

                when(settlementRepository.findByPaymentId("pay_demo_0969"))
                                .thenReturn(List.of(settlement));

                when(refundRepository.findByPaymentId("pay_demo_0969"))
                                .thenReturn(List.of());

                when(adjustmentRepository.findBySettlementId("set_demo_0969"))
                                .thenReturn(List.of());

                FinancialAnalysis analysis = financialAnalysisService.analyze(5303L);

                assertNotNull(analysis);

                assertEquals(
                                BigDecimal.valueOf(2291.86).setScale(4),
                                analysis.getExpectedAmount());

                assertEquals(
                                BigDecimal.valueOf(2281.86).setScale(4),
                                analysis.getActualAmount());

                assertEquals(
                                BigDecimal.valueOf(10.00).setScale(4),
                                analysis.getReconciliationDifference());

                assertEquals(
                                BigDecimal.valueOf(2291.86).setScale(4),
                                analysis.getPaymentAmount());

                assertEquals(
                                BigDecimal.valueOf(2291.86).setScale(4),
                                analysis.getSettlementAmount());

                assertEquals(
                                BigDecimal.ZERO.setScale(4),
                                analysis.getSettlementFees());

                assertEquals(
                                BigDecimal.valueOf(10.00).setScale(4),
                                analysis.getSettlementTax());

                assertEquals(
                                BigDecimal.valueOf(10.00).setScale(4),
                                analysis.getKnownDeductions());

                assertEquals(
                                BigDecimal.valueOf(10.00).setScale(4),
                                analysis.getExplainedDifference());

                assertEquals(
                                BigDecimal.ZERO.setScale(4),
                                analysis.getUnexplainedDifference());

                assertTrue(analysis.isPaymentMatchesOrder());
                assertTrue(analysis.isPaymentMatchesExpected());
                assertTrue(analysis.isSettlementMatchesActual());
                assertTrue(analysis.isDifferenceFullyExplained());
                assertTrue(analysis.isSettlementPresent());
                assertFalse(analysis.isRefundPresent());
                assertFalse(analysis.isAdjustmentPresent());

                assertTrue(
                                analysis.getCandidateCauses()
                                                .stream()
                                                .anyMatch(cause -> cause.toLowerCase().contains("tax")));
        }

        @Test
        void shouldIdentifyPartiallyExplainedDifference() {

                ReconciliationException exception = exception(2001L, 2L);

                ReconciliationResult result = result(
                                2L,
                                "pay_partial",
                                1000.00,
                                950.00,
                                50.00);

                Payment payment = payment(
                                "pay_partial",
                                "order_partial",
                                1000.00);

                MerchantOrder order = order(
                                "order_partial",
                                1000.00);

                Settlement settlement = settlement(
                                "set_partial",
                                "pay_partial",
                                1000.00,
                                30.00,
                                10.00);

                when(exceptionRepository.findById(2001L))
                                .thenReturn(Optional.of(exception));

                when(resultRepository.findById(2L))
                                .thenReturn(Optional.of(result));

                when(paymentRepository.findByPaymentId("pay_partial"))
                                .thenReturn(Optional.of(payment));

                when(orderRepository.findByOrderId("order_partial"))
                                .thenReturn(Optional.of(order));

                when(settlementRepository.findByPaymentId("pay_partial"))
                                .thenReturn(List.of(settlement));

                when(refundRepository.findByPaymentId("pay_partial"))
                                .thenReturn(List.of());

                when(adjustmentRepository.findBySettlementId("set_partial"))
                                .thenReturn(List.of());

                FinancialAnalysis analysis = financialAnalysisService.analyze(2001L);

                assertNotNull(analysis);

                assertEquals(
                                BigDecimal.valueOf(1000.00).setScale(4),
                                analysis.getExpectedAmount());

                assertEquals(
                                BigDecimal.valueOf(950.00).setScale(4),
                                analysis.getActualAmount());

                assertEquals(
                                BigDecimal.valueOf(50.00).setScale(4),
                                analysis.getReconciliationDifference());

                assertEquals(
                                BigDecimal.valueOf(40.00).setScale(4),
                                analysis.getKnownDeductions());

                assertEquals(
                                BigDecimal.valueOf(40.00).setScale(4),
                                analysis.getExplainedDifference());

                assertEquals(
                                BigDecimal.valueOf(10.00).setScale(4),
                                analysis.getUnexplainedDifference());

                assertFalse(analysis.isDifferenceFullyExplained());

                assertTrue(analysis.isSettlementPresent());
                assertTrue(analysis.isPaymentMatchesOrder());
                assertTrue(analysis.isPaymentMatchesExpected());
                assertFalse(analysis.isSettlementMatchesActual());

                assertTrue(
                                analysis.getFinancialAssessment()
                                                .toLowerCase()
                                                .contains("residual"));
        }

        @Test
        void shouldIdentifyMissingSettlement() {

                ReconciliationException exception = exception(3001L, 3L);

                ReconciliationResult result = result(
                                3L,
                                "pay_missing",
                                500.00,
                                0.00,
                                500.00);

                Payment payment = payment(
                                "pay_missing",
                                "order_missing",
                                500.00);

                MerchantOrder order = order(
                                "order_missing",
                                500.00);

                when(exceptionRepository.findById(3001L))
                                .thenReturn(Optional.of(exception));

                when(resultRepository.findById(3L))
                                .thenReturn(Optional.of(result));

                when(paymentRepository.findByPaymentId("pay_missing"))
                                .thenReturn(Optional.of(payment));

                when(orderRepository.findByOrderId("order_missing"))
                                .thenReturn(Optional.of(order));

                when(settlementRepository.findByPaymentId("pay_missing"))
                                .thenReturn(List.of());

                when(refundRepository.findByPaymentId("pay_missing"))
                                .thenReturn(List.of());

                FinancialAnalysis analysis = financialAnalysisService.analyze(3001L);

                assertNotNull(analysis);

                assertFalse(analysis.isSettlementPresent());

                assertEquals(
                                BigDecimal.valueOf(500.00).setScale(4),
                                analysis.getUnexplainedDifference());

                assertEquals(
                                BigDecimal.ZERO.setScale(4),
                                analysis.getExplainedDifference());

                assertFalse(analysis.isDifferenceFullyExplained());

                assertTrue(
                                analysis.getCandidateCauses()
                                                .stream()
                                                .anyMatch(cause -> cause.toLowerCase().contains("settlement")));

                assertTrue(
                                analysis.getMissingEvidence()
                                                .stream()
                                                .anyMatch(evidence -> evidence.toLowerCase().contains("settlement")));
        }

        @Test
        void shouldDetectPaymentOrderMismatch() {

                ReconciliationException exception = exception(4001L, 4L);

                ReconciliationResult result = result(
                                4L,
                                "pay_mismatch",
                                1000.00,
                                1000.00,
                                0.00);

                Payment payment = payment(
                                "pay_mismatch",
                                "order_mismatch",
                                1000.00);

                MerchantOrder order = order(
                                "order_mismatch",
                                900.00);

                when(exceptionRepository.findById(4001L))
                                .thenReturn(Optional.of(exception));

                when(resultRepository.findById(4L))
                                .thenReturn(Optional.of(result));

                when(paymentRepository.findByPaymentId("pay_mismatch"))
                                .thenReturn(Optional.of(payment));

                when(orderRepository.findByOrderId("order_mismatch"))
                                .thenReturn(Optional.of(order));

                when(settlementRepository.findByPaymentId("pay_mismatch"))
                                .thenReturn(List.of());

                when(refundRepository.findByPaymentId("pay_mismatch"))
                                .thenReturn(List.of());

                FinancialAnalysis analysis = financialAnalysisService.analyze(4001L);

                assertNotNull(analysis);

                assertFalse(analysis.isPaymentMatchesOrder());

                assertTrue(analysis.isPaymentMatchesExpected());

                assertTrue(
                                analysis.getContradictions()
                                                .stream()
                                                .anyMatch(contradiction -> contradiction.toLowerCase()
                                                                .contains("order")));

                assertTrue(
                                analysis.getCandidateCauses()
                                                .stream()
                                                .anyMatch(cause -> cause.toLowerCase().contains("payment")));
        }

        private ReconciliationException exception(
                        Long id,
                        Long resultId) {

                ReconciliationException exception = new ReconciliationException();

                exception.setId(id);
                exception.setReconciliationResultId(resultId);

                return exception;
        }

        private ReconciliationResult result(
                        Long id,
                        String paymentReference,
                        double expected,
                        double actual,
                        double difference) {

                ReconciliationResult result = new ReconciliationResult();

                result.setId(id);

                result.setPaymentReference(paymentReference);

                result.setExpectedAmount(
                                BigDecimal.valueOf(expected));

                result.setActualAmount(
                                BigDecimal.valueOf(actual));

                result.setDifference(
                                BigDecimal.valueOf(difference));

                return result;
        }

        private Payment payment(
                        String paymentId,
                        String orderId,
                        double amount) {

                Payment payment = new Payment();

                payment.setPaymentId(paymentId);
                payment.setOrderId(orderId);
                payment.setAmount(
                                BigDecimal.valueOf(amount));
                payment.setCurrency("INR");

                return payment;
        }

        private MerchantOrder order(
                        String orderId,
                        double amount) {

                MerchantOrder order = new MerchantOrder();

                order.setOrderId(orderId);
                order.setAmount(
                                BigDecimal.valueOf(amount));
                order.setCurrency("INR");

                return order;
        }

        private Settlement settlement(
                        String settlementId,
                        String paymentId,
                        double amount,
                        double fees,
                        double tax) {

                Settlement settlement = new Settlement();

                settlement.setSettlementId(settlementId);
                settlement.setPaymentId(paymentId);

                settlement.setAmount(
                                BigDecimal.valueOf(amount));

                settlement.setFees(
                                BigDecimal.valueOf(fees));

                settlement.setTax(
                                BigDecimal.valueOf(tax));

                settlement.setStatus("SETTLED");

                return settlement;
        }
}