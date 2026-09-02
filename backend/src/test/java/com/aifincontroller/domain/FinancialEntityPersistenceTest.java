package com.aifincontroller.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aifincontroller.repository.AdjustmentRepository;
import com.aifincontroller.repository.AuditLogRepository;
import com.aifincontroller.repository.MerchantOrderRepository;
import com.aifincontroller.repository.PaymentRepository;
import com.aifincontroller.repository.ReconciliationExceptionRepository;
import com.aifincontroller.repository.ReconciliationResultRepository;
import com.aifincontroller.repository.RefundRepository;
import com.aifincontroller.repository.SettlementRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true"
})
class FinancialEntityPersistenceTest {

    private static final BigDecimal AMOUNT = new BigDecimal("100.5000");
    private static final BigDecimal FEES = new BigDecimal("2.3600");
    private static final BigDecimal TAX = new BigDecimal("0.4200");

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
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatedExpectedTablesConstraintsAndIndexes() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
                String.class);
        assertThat(tables).contains(
                "orders",
                "payments",
                "settlements",
                "refunds",
                "adjustments",
                "reconciliation_results",
                "reconciliation_exceptions",
                "audit_logs",
                "flyway_schema_history");

        Integer uniqueOrderId = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint c JOIN pg_class t ON c.conrelid = t.oid "
                        + "WHERE t.relname = 'orders' AND c.contype = 'u'",
                Integer.class);
        assertThat(uniqueOrderId).isGreaterThanOrEqualTo(1);

        Integer settlementComposite = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint c JOIN pg_class t ON c.conrelid = t.oid "
                        + "WHERE t.relname = 'settlements' AND c.contype = 'u'",
                Integer.class);
        assertThat(settlementComposite).isGreaterThanOrEqualTo(1);

        Integer exceptionFk = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint c JOIN pg_class t ON c.conrelid = t.oid "
                        + "WHERE t.relname = 'reconciliation_exceptions' AND c.contype = 'f'",
                Integer.class);
        assertThat(exceptionFk).isEqualTo(1);

        Integer amountCheck = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'ck_payments_amount_non_negative'",
                Integer.class);
        assertThat(amountCheck).isEqualTo(1);

        List<String> indexNames = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public'",
                String.class);
        assertThat(indexNames).contains(
                "idx_payments_order_id",
                "idx_settlements_payment_id",
                "idx_settlements_utr",
                "idx_refunds_payment_id",
                "idx_adjustments_settlement_id",
                "idx_recon_results_batch_id",
                "idx_recon_results_payment_ref",
                "idx_recon_exceptions_status",
                "idx_audit_logs_entity",
                "uk_settlements_settlement_id_null_payment");
    }

    @Test
    void persistsAllFinancialEntitiesAndLooksThemUpBySourceIds() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String orderId = "order_" + suffix;
        String paymentId = "pay_" + suffix;
        String settlementId = "setl_" + suffix;
        Instant now = Instant.parse("2026-08-31T06:00:00Z");

        MerchantOrder order = new MerchantOrder();
        order.setOrderId(orderId);
        order.setAmount(AMOUNT);
        order.setCurrency("INR");
        order.setStatus("paid");
        order.setCreatedAt(now);
        orderRepository.saveAndFlush(order);

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setOrderId(orderId);
        payment.setAmount(AMOUNT);
        payment.setCurrency("INR");
        payment.setStatus("captured");
        payment.setCapturedAt(now);
        payment.setCreatedAt(now);
        paymentRepository.saveAndFlush(payment);

        Settlement settlement = new Settlement();
        settlement.setSettlementId(settlementId);
        settlement.setPaymentId(paymentId);
        settlement.setAmount(AMOUNT);
        settlement.setFees(FEES);
        settlement.setTax(TAX);
        settlement.setStatus("processed");
        settlement.setUtr("UTR" + suffix);
        settlement.setSettledAt(now);
        settlementRepository.saveAndFlush(settlement);

        Refund refund = new Refund();
        refund.setRefundId("rfnd_" + suffix);
        refund.setPaymentId(paymentId);
        refund.setAmount(new BigDecimal("10.0000"));
        refund.setStatus("processed");
        refund.setCreatedAt(now);
        refundRepository.saveAndFlush(refund);

        Adjustment adjustment = new Adjustment();
        adjustment.setAdjustmentId("adj_" + suffix);
        adjustment.setSettlementId(settlementId);
        adjustment.setAmount(new BigDecimal("-1.2500"));
        adjustment.setType("fee_credit");
        adjustment.setDescription("Fee reversal");
        adjustment.setCreatedAt(now);
        adjustmentRepository.saveAndFlush(adjustment);

        ReconciliationResult result = new ReconciliationResult();
        result.setBatchId("batch_" + suffix);
        result.setPaymentReference(paymentId);
        result.setMatchedRecord(settlementId);
        result.setMatchType("EXACT");
        result.setExpectedAmount(AMOUNT);
        result.setActualAmount(AMOUNT);
        result.setDifference(BigDecimal.ZERO.setScale(4));
        result.setStatus("MATCHED");
        result.setConfidenceScore(new BigDecimal("1.0000"));
        result.setCreatedAt(now);
        resultRepository.saveAndFlush(result);

        ReconciliationException exception = new ReconciliationException();
        exception.setReconciliationResultId(result.getId());
        exception.setType("NONE");
        exception.setCategory("NONE");
        exception.setSeverity("INFO");
        exception.setStatus("CLOSED");
        exception.setEvidenceSummary("Exact match");
        exception.setAiConfidence(new BigDecimal("0.9900"));
        exception.setCreatedAt(now);
        exceptionRepository.saveAndFlush(exception);

        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType("PAYMENT");
        auditLog.setEntityId(paymentId);
        auditLog.setAction("INGESTED");
        auditLog.setActor("system");
        auditLog.setEvidenceReference("source=test");
        auditLog.setDecision("accepted");
        auditLog.setCreatedAt(now);
        auditLogRepository.saveAndFlush(auditLog);

        MerchantOrder loadedOrder = orderRepository.findByOrderId(orderId).orElseThrow();
        assertThat(loadedOrder.getAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(loadedOrder.getCurrency()).isEqualTo("INR");

        Payment loadedPayment = paymentRepository.findByPaymentId(paymentId).orElseThrow();
        assertThat(loadedPayment.getOrderId()).isEqualTo(orderId);
        assertThat(paymentRepository.findByOrderId(orderId)).hasSize(1);

        assertThat(settlementRepository.findBySettlementIdAndPaymentId(settlementId, paymentId)).isPresent();
        assertThat(settlementRepository.findByUtr("UTR" + suffix)).hasSize(1);

        assertThat(refundRepository.findByPaymentId(paymentId)).hasSize(1);
        assertThat(adjustmentRepository.findBySettlementId(settlementId).get(0).getAmount())
                .isEqualByComparingTo(new BigDecimal("-1.2500"));

        assertThat(resultRepository.findByBatchId("batch_" + suffix)).hasSize(1);
        assertThat(exceptionRepository.findByReconciliationResultId(result.getId())).hasSize(1);
        assertThat(auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc("PAYMENT", paymentId))
                .hasSize(1);
    }

    @Test
    void duplicateSourceOrderIdIsRejected() {
        String orderId = "order_dup_" + UUID.randomUUID();
        orderRepository.saveAndFlush(order(orderId));

        assertThatThrownBy(() -> orderRepository.saveAndFlush(order(orderId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateSettlementPaymentPairIsRejected() {
        String settlementId = "setl_dup_" + UUID.randomUUID();
        String paymentId = "pay_dup_" + UUID.randomUUID();
        settlementRepository.saveAndFlush(settlement(settlementId, paymentId));

        assertThatThrownBy(() -> settlementRepository.saveAndFlush(settlement(settlementId, paymentId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void negativePaymentAmountIsRejected() {
        Payment payment = new Payment();
        payment.setPaymentId("pay_neg_" + UUID.randomUUID());
        payment.setOrderId("order_neg");
        payment.setAmount(new BigDecimal("-1.0000"));
        payment.setCurrency("INR");
        payment.setStatus("captured");
        payment.setCreatedAt(Instant.now());

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(payment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void exceptionRequiresExistingReconciliationResult() {
        ReconciliationException exception = new ReconciliationException();
        exception.setReconciliationResultId(9_999_999L);
        exception.setType("MISSING_SETTLEMENT");
        exception.setSeverity("HIGH");
        exception.setStatus("OPEN");
        exception.setCreatedAt(Instant.now());

        assertThatThrownBy(() -> exceptionRepository.saveAndFlush(exception))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private MerchantOrder order(String orderId) {
        MerchantOrder order = new MerchantOrder();
        order.setOrderId(orderId);
        order.setAmount(AMOUNT);
        order.setCurrency("INR");
        order.setStatus("created");
        order.setCreatedAt(Instant.now());
        return order;
    }

    private Settlement settlement(String settlementId, String paymentId) {
        Settlement settlement = new Settlement();
        settlement.setSettlementId(settlementId);
        settlement.setPaymentId(paymentId);
        settlement.setAmount(AMOUNT);
        settlement.setFees(FEES);
        settlement.setTax(TAX);
        settlement.setStatus("processed");
        return settlement;
    }
}
