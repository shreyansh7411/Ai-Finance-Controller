package com.aifincontroller.service;

import com.aifincontroller.ai.dto.AiInvestigationRequest;
import com.aifincontroller.domain.Adjustment;
import com.aifincontroller.domain.MerchantOrder;
import com.aifincontroller.domain.Payment;
import com.aifincontroller.domain.ReconciliationException;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true"
})
class AiInvestigationEvidenceServiceTest {

    @Autowired
    private AiInvestigationEvidenceService evidenceService;

    @Autowired
    private MerchantOrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private AdjustmentRepository adjustmentRepository;

    @Autowired
    private ReconciliationResultRepository resultRepository;

    @Autowired
    private ReconciliationExceptionRepository exceptionRepository;

    @Test
    void shouldBuildCompleteInvestigationRequest() {

        String suffix =
                UUID.randomUUID().toString().substring(0, 8);

        String orderId = "order_ai_" + suffix;
        String paymentId = "pay_ai_" + suffix;
        String settlementId = "setl_ai_" + suffix;

        Instant now =
                Instant.parse("2026-08-31T06:00:00Z");

        MerchantOrder order = new MerchantOrder();
        order.setOrderId(orderId);
        order.setAmount(new BigDecimal("100.0000"));
        order.setCurrency("INR");
        order.setStatus("paid");
        order.setCreatedAt(now);
        orderRepository.saveAndFlush(order);

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setOrderId(orderId);
        payment.setAmount(new BigDecimal("100.0000"));
        payment.setCurrency("INR");
        payment.setStatus("captured");
        payment.setCapturedAt(now);
        payment.setCreatedAt(now);
        paymentRepository.saveAndFlush(payment);

        Settlement settlement = new Settlement();
        settlement.setSettlementId(settlementId);
        settlement.setPaymentId(paymentId);
        settlement.setAmount(new BigDecimal("100.0000"));
        settlement.setFees(new BigDecimal("2.0000"));
        settlement.setTax(new BigDecimal("0.3600"));
        settlement.setStatus("processed");
        settlement.setUtr("UTR_AI_" + suffix);
        settlement.setSettledAt(now);
        settlementRepository.saveAndFlush(settlement);

        Refund refund = new Refund();
        refund.setRefundId("rfnd_ai_" + suffix);
        refund.setPaymentId(paymentId);
        refund.setAmount(new BigDecimal("10.0000"));
        refund.setStatus("processed");
        refund.setCreatedAt(now);
        refundRepository.saveAndFlush(refund);

        Adjustment adjustment = new Adjustment();
        adjustment.setAdjustmentId("adj_ai_" + suffix);
        adjustment.setSettlementId(settlementId);
        adjustment.setAmount(new BigDecimal("-1.5000"));
        adjustment.setType("fee_credit");
        adjustment.setDescription("Fee reversal");
        adjustment.setCreatedAt(now);
        adjustmentRepository.saveAndFlush(adjustment);

        ReconciliationResult result =
                new ReconciliationResult();

        result.setBatchId("batch_ai_" + suffix);
        result.setPaymentReference(paymentId);
        result.setMatchedRecord(settlementId);
        result.setMatchType("FEE_DIFFERENCE");
        result.setExpectedAmount(
                new BigDecimal("100.0000"));
        result.setActualAmount(
                new BigDecimal("97.6400"));
        result.setDifference(
                new BigDecimal("2.3600"));
        result.setStatus("EXCEPTION");
        result.setConfidenceScore(
                new BigDecimal("0.8500"));
        result.setCreatedAt(now);

        resultRepository.saveAndFlush(result);

        ReconciliationException exception =
                new ReconciliationException();

        exception.setReconciliationResultId(
                result.getId());
        exception.setType("FEE_DIFFERENCE");
        exception.setCategory("AMOUNT_MISMATCH");
        exception.setSeverity("MEDIUM");
        exception.setStatus("OPEN");
        exception.setExpectedAmount(
                new BigDecimal("100.0000"));
        exception.setActualAmount(
                new BigDecimal("97.6400"));
        exception.setDifference(
                new BigDecimal("2.3600"));
        exception.setSourceReference(
                "payment:" + paymentId
                        + ",order:" + orderId);
        exception.setCandidateRecord(
                "settlement:" + settlementId);
        exception.setEvidenceSummary(
                "Settlement amount differs due to fees and tax.");
        exception.setEvidence(
                "Payment amount is 100 INR; settlement amount is 100 INR "
                        + "with 2 INR fees and 0.36 INR tax.");
        exception.setAiConfidence(
                BigDecimal.ZERO);
        exception.setCreatedAt(now);

        exceptionRepository.saveAndFlush(exception);

        AiInvestigationRequest request =
                evidenceService.buildRequest(
                        exception.getId());

        assertThat(request.getExceptionId())
                .isEqualTo(exception.getId());

        assertThat(request.getExceptionType())
                .isEqualTo("FEE_DIFFERENCE");

        assertThat(request.getCategory())
                .isEqualTo("AMOUNT_MISMATCH");

        assertThat(request.getSeverity())
                .isEqualTo("MEDIUM");

        assertThat(request.getPaymentReference())
                .isEqualTo(paymentId);

        assertThat(request.getPaymentId())
                .isEqualTo(paymentId);

        assertThat(request.getOrderId())
                .isEqualTo(orderId);

        assertThat(request.getExpectedAmount())
                .isEqualByComparingTo("100.0000");

        assertThat(request.getActualAmount())
                .isEqualByComparingTo("97.6400");

        assertThat(request.getDifference())
                .isEqualByComparingTo("2.3600");

        assertThat(request.getPayment())
                .isNotNull();

        assertThat(request.getPayment().getAmount())
                .isEqualByComparingTo("100.0000");

        assertThat(request.getOrder())
                .isNotNull();

        assertThat(request.getOrder().getAmount())
                .isEqualByComparingTo("100.0000");

        assertThat(request.getSettlement())
                .isNotNull();

        assertThat(request.getSettlement().getSettlementId())
                .isEqualTo(settlementId);

        assertThat(request.getSettlement().getFees())
                .isEqualByComparingTo("2.0000");

        assertThat(request.getSettlement().getTax())
                .isEqualByComparingTo("0.3600");

        assertThat(request.getRefunds())
                .hasSize(1);

        assertThat(request.getRefunds().get(0).getRefundId())
                .isEqualTo("rfnd_ai_" + suffix);

        assertThat(request.getAdjustments())
                .hasSize(1);

        assertThat(request.getAdjustments().get(0).getAdjustmentId())
                .isEqualTo("adj_ai_" + suffix);

        assertThat(request.getEvidenceSummary())
                .isEqualTo(
                        "Settlement amount differs due to fees and tax.");
    }
}
