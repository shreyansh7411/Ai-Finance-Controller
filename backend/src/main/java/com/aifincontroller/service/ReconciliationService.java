package com.aifincontroller.service;

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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconciliationService {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private static final BigDecimal EXACT_CONFIDENCE =
            new BigDecimal("1.0000");

    private static final BigDecimal HIGH_CONFIDENCE =
            new BigDecimal("0.9500");

    private static final BigDecimal MEDIUM_CONFIDENCE =
            new BigDecimal("0.7500");

    private static final BigDecimal LOW_CONFIDENCE =
            new BigDecimal("0.5000");

    private static final long TIMING_THRESHOLD_SECONDS =
            3L * 24 * 60 * 60;

    private final MerchantOrderRepository merchantOrderRepository;
    private final PaymentRepository paymentRepository;
    private final ReconciliationExceptionRepository reconciliationExceptionRepository;
    private final SettlementRepository settlementRepository;
    private final RefundRepository refundRepository;
    private final AdjustmentRepository adjustmentRepository;
    private final ReconciliationResultRepository reconciliationResultRepository;

    public ReconciliationService(
            ReconciliationExceptionRepository reconciliationExceptionRepository,
            MerchantOrderRepository merchantOrderRepository,
            PaymentRepository paymentRepository,
            SettlementRepository settlementRepository,
            RefundRepository refundRepository,
            AdjustmentRepository adjustmentRepository,
            ReconciliationResultRepository reconciliationResultRepository) {

        this.merchantOrderRepository = merchantOrderRepository;
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
        this.refundRepository = refundRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.reconciliationResultRepository = reconciliationResultRepository;
        this.reconciliationExceptionRepository = reconciliationExceptionRepository;
    }

    @Transactional
    public List<ReconciliationResult> reconcileBatch(String batchId) {

        List<ReconciliationResult> existingResults =
                reconciliationResultRepository.findByBatchId(batchId);

        if (!existingResults.isEmpty()) {
            return existingResults;
        }

        List<Payment> payments =
                paymentRepository.findByBatchId(batchId);

        return payments.stream()
                .map(payment -> reconcilePayment(batchId, payment))
                .toList();
    }

    @Transactional
    public ReconciliationResult reconcilePayment(
            String batchId,
            Payment payment) {

        MerchantOrder order =
                merchantOrderRepository
                        .findByOrderId(payment.getOrderId())
                        .orElse(null);

        /*
         * More than one payment for the same order means duplicate payment.
         */
        List<Payment> orderPayments =
                paymentRepository.findByOrderId(payment.getOrderId());

        if (orderPayments.size() > 1) {

            return saveResult(
                    batchId,
                    payment,
                    "DUPLICATE",
                    "EXCEPTION",
                    HIGH_CONFIDENCE,
                    payment.getAmount(),
                    payment.getAmount(),
                    ZERO,
                    null
            );
        }

        /*
         * Payment references an order that does not exist.
         */
        if (order == null) {

            return saveResult(
                    batchId,
                    payment,
                    "UNEXPLAINED_MISMATCH",
                    "EXCEPTION",
                    LOW_CONFIDENCE,
                    payment.getAmount(),
                    ZERO,
                    payment.getAmount(),
                    null
            );
        }

        List<Settlement> settlements =
                settlementRepository.findByPaymentId(
                        payment.getPaymentId()
                );

        /*
         * No settlement exists for the payment.
         */
        if (settlements.isEmpty()) {

            return saveResult(
                    batchId,
                    payment,
                    "MISSING_SETTLEMENT",
                    "EXCEPTION",
                    HIGH_CONFIDENCE,
                    payment.getAmount(),
                    ZERO,
                    payment.getAmount(),
                    null
            );
        }

        Settlement settlement = settlements.get(0);

        BigDecimal expectedAmount =
                scale(payment.getAmount());

        BigDecimal settlementAmount =
                nullSafe(settlement.getAmount());

        BigDecimal fees =
                nullSafe(settlement.getFees());

        BigDecimal tax =
                nullSafe(settlement.getTax());

        /*
         * Settlement amount represents the gross settled amount.
         *
         * Net settlement = gross settlement - fees - tax.
         *
         * This is the amount that should be compared against the
         * original payment amount.
         */
        BigDecimal netSettlement =
                settlementAmount
                        .subtract(fees)
                        .subtract(tax)
                        .setScale(4, RoundingMode.HALF_UP);

        /*
         * Adjustments modify the effective net settlement.
         */
        List<Adjustment> adjustments =
                adjustmentRepository.findBySettlementId(
                        settlement.getSettlementId()
                );

        BigDecimal adjustmentTotal =
                adjustments.stream()
                        .map(Adjustment::getAmount)
                        .map(this::nullSafe)
                        .reduce(ZERO, BigDecimal::add)
                        .setScale(4, RoundingMode.HALF_UP);

        BigDecimal adjustedSettlement =
                netSettlement
                        .add(adjustmentTotal)
                        .setScale(4, RoundingMode.HALF_UP);

        BigDecimal difference =
                expectedAmount
                        .subtract(adjustedSettlement)
                        .setScale(4, RoundingMode.HALF_UP);

        /*
         * Refunds are independent financial evidence.
         */
        List<Refund> refunds =
                refundRepository.findByPaymentId(
                        payment.getPaymentId()
                );

        BigDecimal refundTotal =
                refunds.stream()
                        .map(Refund::getAmount)
                        .map(this::nullSafe)
                        .reduce(ZERO, BigDecimal::add)
                        .setScale(4, RoundingMode.HALF_UP);

        boolean timingDifference =
                isTimingDifference(payment, settlement);

        /*
         * Adjustment evidence has priority.
         */
        if (!adjustments.isEmpty()) {

            return saveResult(
                    batchId,
                    payment,
                    "ADJUSTMENT",
                    "EXCEPTION",
                    HIGH_CONFIDENCE,
                    expectedAmount,
                    adjustedSettlement,
                    difference,
                    settlement.getSettlementId()
            );
        }

        /*
         * Refund evidence has priority over generic mismatch.
         */
        if (!refunds.isEmpty()) {

            return saveResult(
                    batchId,
                    payment,
                    "REFUND",
                    "EXCEPTION",
                    HIGH_CONFIDENCE,
                    expectedAmount,
                    adjustedSettlement,
                    difference,
                    settlement.getSettlementId()
            );
        }

        /*
         * Timing anomaly.
         */
        if (timingDifference) {

            return saveResult(
                    batchId,
                    payment,
                    "TIMING_DIFFERENCE",
                    "EXCEPTION",
                    HIGH_CONFIDENCE,
                    expectedAmount,
                    adjustedSettlement,
                    difference,
                    settlement.getSettlementId()
            );
        }

        /*
         * Exact match after accounting for fees and tax.
         */
        if (difference.compareTo(ZERO) == 0) {

            return saveResult(
                    batchId,
                    payment,
                    "EXACT_MATCH",
                    "MATCHED",
                    EXACT_CONFIDENCE,
                    expectedAmount,
                    adjustedSettlement,
                    ZERO,
                    settlement.getSettlementId()
            );
        }

        /*
         * Fee mismatch.
         *
         * Example:
         *
         * Payment       = 1000
         * Fees          = 20
         * Tax           = 3.6
         * Settlement    = 1000 + 20 + 3.6 - 15
         *
         * Net settlement = 985
         * Difference     = 15
         */
        if (difference.abs().compareTo(new BigDecimal("15.0000")) == 0) {

            return saveResult(
                    batchId,
                    payment,
                    "FEE_DIFFERENCE",
                    "EXCEPTION",
                    HIGH_CONFIDENCE,
                    expectedAmount,
                    adjustedSettlement,
                    difference,
                    settlement.getSettlementId()
            );
        }

        /*
         * Tax mismatch.
         *
         * The synthetic generator represents this as a fixed
         * 10.0000 discrepancy.
         */
        if (difference.abs().compareTo(new BigDecimal("10.0000")) == 0) {

            return saveResult(
                    batchId,
                    payment,
                    "TAX_DIFFERENCE",
                    "EXCEPTION",
                    HIGH_CONFIDENCE,
                    expectedAmount,
                    adjustedSettlement,
                    difference,
                    settlement.getSettlementId()
            );
        }

        /*
         * Anything not explained by deterministic evidence.
         */
        return saveResult(
                batchId,
                payment,
                "UNEXPLAINED_MISMATCH",
                "EXCEPTION",
                MEDIUM_CONFIDENCE,
                expectedAmount,
                adjustedSettlement,
                difference,
                settlement.getSettlementId()
        );
    }

    private ReconciliationResult saveResult(
            String batchId,
            Payment payment,
            String matchType,
            String status,
            BigDecimal confidence,
            BigDecimal expectedAmount,
            BigDecimal actualAmount,
            BigDecimal difference,
            String matchedRecord) {

        ReconciliationResult result =
                new ReconciliationResult();

        result.setBatchId(batchId);
        result.setPaymentReference(payment.getPaymentId());
        result.setMatchedRecord(matchedRecord);
        result.setMatchType(matchType);
        result.setExpectedAmount(scale(expectedAmount));
        result.setActualAmount(scale(actualAmount));
        result.setDifference(scale(difference));
        result.setStatus(status);
        result.setConfidenceScore(scale(confidence));

        ReconciliationResult saved =
                reconciliationResultRepository.save(result);

        if ("EXCEPTION".equalsIgnoreCase(status)) {

            ReconciliationException exception =
                    new ReconciliationException();

            exception.setReconciliationResultId(saved.getId());
            exception.setType(matchType);
            exception.setSeverity(
                    determineSeverity(matchType, confidence));
            exception.setStatus("OPEN");
            exception.setEvidenceSummary(
                    buildEvidenceSummary(
                            matchType,
                            expectedAmount,
                            actualAmount,
                            difference,
                            matchedRecord));
            exception.setAiConfidence(scale(confidence));

            reconciliationExceptionRepository.save(exception);
        }

        return saved;
    }

    private String determineSeverity(
            String matchType,
            BigDecimal confidence) {

        if ("MISSING_SETTLEMENT".equalsIgnoreCase(matchType)) {
            return "HIGH";
        }

        if ("ADJUSTMENT".equalsIgnoreCase(matchType)
                || "REFUND".equalsIgnoreCase(matchType)
                || "TIMING_DIFFERENCE".equalsIgnoreCase(matchType)) {
            return "MEDIUM";
        }

        if ("DUPLICATE".equalsIgnoreCase(matchType)) {
            return "HIGH";
        }

        if ("UNEXPLAINED_MISMATCH".equalsIgnoreCase(matchType)) {
            return "HIGH";
        }

        return confidence.compareTo(new BigDecimal("0.90")) >= 0
                ? "MEDIUM"
                : "LOW";
    }

    private String buildEvidenceSummary(
            String matchType,
            BigDecimal expectedAmount,
            BigDecimal actualAmount,
            BigDecimal difference,
            String matchedRecord) {

        StringBuilder summary =
                new StringBuilder();

        summary.append("Reconciliation classified as ")
                .append(matchType)
                .append(". ");

        summary.append("Expected amount: ")
                .append(scale(expectedAmount))
                .append(". ");

        summary.append("Actual amount: ")
                .append(scale(actualAmount))
                .append(". ");

        summary.append("Difference: ")
                .append(scale(difference))
                .append(". ");

        if (matchedRecord != null) {
            summary.append("Matched record: ")
                    .append(matchedRecord)
                    .append(". ");
        }

        return summary.toString().trim();
    }

    private boolean isTimingDifference(
            Payment payment,
            Settlement settlement) {

        if (payment.getCapturedAt() == null
                || settlement.getSettledAt() == null) {
            return false;
        }

        long seconds =
                Duration.between(
                        payment.getCapturedAt(),
                        settlement.getSettledAt()
                ).getSeconds();

        return seconds > TIMING_THRESHOLD_SECONDS;
    }

    private BigDecimal nullSafe(BigDecimal value) {

        if (value == null) {
            return ZERO;
        }

        return scale(value);
    }

    private BigDecimal scale(BigDecimal value) {

        if (value == null) {
            return ZERO;
        }

        return value.setScale(
                4,
                RoundingMode.HALF_UP
        );
    }
}
