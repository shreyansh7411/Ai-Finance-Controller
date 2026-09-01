package com.aifincontroller.service;

import com.aifincontroller.domain.MerchantOrder;
import com.aifincontroller.domain.Payment;
import com.aifincontroller.domain.ReconciliationResult;
import com.aifincontroller.domain.Settlement;
import com.aifincontroller.repository.AdjustmentRepository;
import com.aifincontroller.repository.MerchantOrderRepository;
import com.aifincontroller.repository.PaymentRepository;
import com.aifincontroller.repository.ReconciliationExceptionRepository;
import com.aifincontroller.repository.ReconciliationResultRepository;
import com.aifincontroller.repository.RefundRepository;
import com.aifincontroller.repository.SettlementRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReconciliationServiceTest {

    private final MerchantOrderRepository merchantOrderRepository = mock(MerchantOrderRepository.class);

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);

    private final ReconciliationExceptionRepository reconciliationExceptionRepository = mock(
            ReconciliationExceptionRepository.class);

    private final SettlementRepository settlementRepository = mock(SettlementRepository.class);

    private final RefundRepository refundRepository = mock(RefundRepository.class);

    private final AdjustmentRepository adjustmentRepository = mock(AdjustmentRepository.class);

    private final ReconciliationResultRepository reconciliationResultRepository = mock(
            ReconciliationResultRepository.class);

    private final ReconciliationService service = new ReconciliationService(
            reconciliationExceptionRepository,
            merchantOrderRepository,
            paymentRepository,
            settlementRepository,
            refundRepository,
            adjustmentRepository,
            reconciliationResultRepository);

    @Test
    void ingestedRecordsRemainCompatibleWithReconciliationEngine() {

        String batchId = "batch_test";
        String paymentId = "pay_test123";
        String orderId = "order_test123";
        String settlementId = "setl_test123";

        MerchantOrder order = new MerchantOrder();
        order.setOrderId(orderId);
        order.setAmount(new BigDecimal("150.0000"));
        order.setCurrency("INR");
        order.setStatus("created");

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setBatchId(batchId);
        payment.setOrderId(orderId);
        payment.setAmount(new BigDecimal("150.0000"));
        payment.setCurrency("INR");
        payment.setStatus("captured");

        Settlement settlement = new Settlement();
        settlement.setSettlementId(settlementId);
        settlement.setPaymentId(paymentId);
        settlement.setAmount(new BigDecimal("150.0000"));
        settlement.setFees(new BigDecimal("0.0000"));
        settlement.setTax(new BigDecimal("0.0000"));
        settlement.setStatus("processed");

        when(reconciliationResultRepository.findByBatchId(batchId))
                .thenReturn(List.of());

        when(paymentRepository.findByBatchId(batchId))
                .thenReturn(List.of(payment));

        when(merchantOrderRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(order));

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(List.of(payment));

        when(settlementRepository.findByPaymentId(paymentId))
                .thenReturn(List.of(settlement));

        when(adjustmentRepository.findBySettlementId(settlementId))
                .thenReturn(List.of());

        when(refundRepository.findByPaymentId(paymentId))
                .thenReturn(List.of());

        ReconciliationResult savedResult = new ReconciliationResult();

        savedResult.setId(1L);
        savedResult.setBatchId(batchId);
        savedResult.setPaymentReference(paymentId);
        savedResult.setMatchedRecord(settlementId);
        savedResult.setMatchType("EXACT_MATCH");
        savedResult.setExpectedAmount(new BigDecimal("150.0000"));
        savedResult.setActualAmount(new BigDecimal("150.0000"));
        savedResult.setDifference(new BigDecimal("0.0000"));
        savedResult.setStatus("MATCHED");
        savedResult.setConfidenceScore(new BigDecimal("1.0000"));

        when(reconciliationResultRepository.save(any(
                ReconciliationResult.class)))
                .thenReturn(savedResult);

        List<ReconciliationResult> results = service.reconcileBatch(batchId);

        assertNotNull(results);
        assertEquals(1, results.size());

        ReconciliationResult result = results.get(0);

        assertEquals("EXACT_MATCH", result.getMatchType());
        assertEquals("MATCHED", result.getStatus());
        assertEquals(
                new BigDecimal("0.0000"),
                result.getDifference());
        assertEquals(
                paymentId,
                result.getPaymentReference());
        assertEquals(
                settlementId,
                result.getMatchedRecord());

        verify(paymentRepository).findByBatchId(batchId);
        verify(settlementRepository).findByPaymentId(paymentId);
        verify(reconciliationResultRepository).save(any(
                ReconciliationResult.class));
    }
}