package com.aifincontroller.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aifincontroller.domain.Adjustment;
import com.aifincontroller.domain.MerchantOrder;
import com.aifincontroller.domain.Payment;
import com.aifincontroller.domain.ReconciliationResult;
import com.aifincontroller.domain.Refund;
import com.aifincontroller.domain.Settlement;
import com.aifincontroller.repository.AdjustmentRepository;
import com.aifincontroller.repository.MerchantOrderRepository;
import com.aifincontroller.repository.PaymentRepository;
import com.aifincontroller.repository.ReconciliationExceptionRepository;
import com.aifincontroller.repository.ReconciliationResultRepository;
import com.aifincontroller.repository.RefundRepository;
import com.aifincontroller.repository.SettlementRepository;
import com.aifincontroller.service.ReconciliationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private MerchantOrderRepository merchantOrderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReconciliationExceptionRepository reconciliationExceptionRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private AdjustmentRepository adjustmentRepository;

    @Mock
    private ReconciliationResultRepository reconciliationResultRepository;

    private ReconciliationService service;

    @BeforeEach
    void setUp() {
        service = new ReconciliationService(
                reconciliationExceptionRepository,
                merchantOrderRepository,
                paymentRepository,
                settlementRepository,
                refundRepository,
                adjustmentRepository,
                reconciliationResultRepository);
    }

    @Test
    void ingestedPaymentRemainsCompatibleWithReconciliationEngine() {

        Payment payment = new Payment();
        payment.setPaymentId("pay_compat_1");
        payment.setBatchId("batch_test");
        payment.setOrderId("order_compat_1");
        payment.setAmount(new BigDecimal("100.0000"));
        payment.setCurrency("INR");
        payment.setStatus("captured");
        payment.setCapturedAt(
                Instant.parse("2026-08-31T06:01:00Z"));

        MerchantOrder order = new MerchantOrder();
        order.setOrderId("order_compat_1");
        order.setAmount(new BigDecimal("100.0000"));
        order.setCurrency("INR");
        order.setStatus("created");

        Settlement settlement = new Settlement();
        settlement.setSettlementId("setl_compat_1");
        settlement.setPaymentId("pay_compat_1");
        settlement.setAmount(new BigDecimal("100.0000"));
        settlement.setFees(new BigDecimal("0.0000"));
        settlement.setTax(new BigDecimal("0.0000"));
        settlement.setStatus("processed");
        settlement.setSettledAt(
                Instant.parse("2026-09-01T06:01:00Z"));

        when(merchantOrderRepository.findByOrderId("order_compat_1"))
                .thenReturn(Optional.of(order));

        when(paymentRepository.findByOrderId("order_compat_1"))
                .thenReturn(List.of(payment));

        when(settlementRepository.findByPaymentId("pay_compat_1"))
                .thenReturn(List.of(settlement));

        when(adjustmentRepository.findBySettlementId("setl_compat_1"))
                .thenReturn(List.of());

        when(refundRepository.findByPaymentId("pay_compat_1"))
                .thenReturn(List.of());

        when(reconciliationResultRepository.save(
                any(ReconciliationResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationResult result =
                service.reconcilePayment("batch_test", payment);

        assertThat(result.getBatchId())
                .isEqualTo("batch_test");

        assertThat(result.getPaymentReference())
                .isEqualTo("pay_compat_1");

        assertThat(result.getMatchedRecord())
                .isEqualTo("setl_compat_1");

        assertThat(result.getMatchType())
                .isEqualTo("EXACT_MATCH");

        assertThat(result.getStatus())
                .isEqualTo("MATCHED");

        assertThat(result.getExpectedAmount())
                .isEqualByComparingTo("100.0000");

        assertThat(result.getActualAmount())
                .isEqualByComparingTo("100.0000");

        assertThat(result.getDifference())
                .isEqualByComparingTo("0.0000");

        assertThat(result.getConfidenceScore())
                .isEqualByComparingTo("1.0000");

        verify(reconciliationResultRepository)
                .save(any(ReconciliationResult.class));
    }
}
