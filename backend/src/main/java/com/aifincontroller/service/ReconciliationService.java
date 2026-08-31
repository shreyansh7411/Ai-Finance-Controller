package com.aifincontroller.service;

import com.aifincontroller.domain.Adjustment;
import com.aifincontroller.domain.MerchantOrder;
import com.aifincontroller.domain.Payment;
import com.aifincontroller.domain.Refund;
import com.aifincontroller.domain.ReconciliationResult;
import com.aifincontroller.domain.Settlement;
import com.aifincontroller.repository.AdjustmentRepository;
import com.aifincontroller.repository.MerchantOrderRepository;
import com.aifincontroller.repository.PaymentRepository;
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

    /*
     * A settlement occurring more than three days after capture
     * is treated as a timing anomaly.
     */
    private static final long TIMING_THRESHOLD_SECONDS =
            3L * 24 * 60 * 60;

    private final MerchantOrderRepository merchantOrderRepository;
    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final RefundRepository refundRepository;
    private final AdjustmentRepository adjustmentRepository;
    private final ReconciliationResultRepository reconciliationResultRepository;

    public ReconciliationService(
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
    }

    @Transactional
    public List<ReconciliationResult> reconcileBatch(String batchId) {

            List<ReconciliationResult> existingResults = reconciliationResultRepository.findByBatchId(batchId);

            if (!existingResults.isEmpty()) {
                    return existingResults;
            }

            List<Payment> payments = paymentRepository.findByBatchId(batchId);

            return payments.stream()
                            .map(payment -> reconcilePayment(batchId, payment))
                            .toList();
    }

    @Transactional
    public ReconciliationResult reconcilePayment(
            String batchId,
            Payment payment) {

        MerchantOrder order = merchantOrderRepository
                .findByOrderId(payment.getOrderId())
                .orElse(null);

        /*
         * First establish whether this payment itself is part of a
         * duplicate order relationship.
         */
        List<Payment> orderPayments =
                paymentRepository.findByOrderId(payment.getOrderId());

        if (orderPayments.size() > 1) {

            return saveResult(
                    batchId,
                    payment,
                    "DUPLICATE",
                    "EXCEPTION",
                    MEDIUM_CONFIDENCE,
                    payment.getAmount(),
                    payment.getAmount(),
                    ZERO,
                    null
            );
        }

        if (order == null) {

            return saveResult(
                    batchId,
                    payment,
                    "UNEXPLAINED_MISMATCH",
                    "EXCEPTION",
                    LOW_CONFIDENCE,
                    payment.getAmount(),
                    payment.getAmount(),
                    payment.getAmount(),
                    null
            );
        }

        List<Settlement> settlements =
                settlementRepository.findByPaymentId(
                        payment.getPaymentId()
                );

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

        BigDecimal netSettlement =
                settlementAmount
                        .subtract(fees)
                        .subtract(tax)
                        .setScale(4, RoundingMode.HALF_UP);

        /*
         * Adjustments can alter the effective settlement amount.
         */
        List<Adjustment> adjustments =
                adjustmentRepository.findBySettlementId(
                        settlement.getSettlementId()
                );

        BigDecimal adjustmentTotal = adjustments.stream()
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
         * Refunds are independent evidence and should be identified
         * before declaring an unexplained mismatch.
         */
        List<Refund> refunds =
                refundRepository.findByPaymentId(
                        payment.getPaymentId()
                );

        BigDecimal refundTotal = refunds.stream()
                .map(Refund::getAmount)
                .map(this::nullSafe)
                .reduce(ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);

        /*
         * Timing is based on actual capture and settlement timestamps.
         */
        boolean timingDifference =
                isTimingDifference(payment, settlement);

        /*
         * Adjustment evidence gets priority over a generic amount
         * mismatch because we have a concrete financial record
         * explaining the difference.
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
         * Fee/tax evidence:
         *
         * If the amount difference corresponds to an explicitly
         * represented fee or tax component, classify it accordingly.
         */
        if (difference.abs().compareTo(fees) == 0) {

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

        if (difference.abs().compareTo(tax) == 0) {

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
         * At this point the system has inspected all deterministic
         * evidence available to it and still cannot explain the
         * mismatch.
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
        result.setExpectedAmount(
                scale(expectedAmount)
        );
        result.setActualAmount(
                scale(actualAmount)
        );
        result.setDifference(
                scale(difference)
        );
        result.setStatus(status);
        result.setConfidenceScore(
                scale(confidence)
        );

        return reconciliationResultRepository.save(result);
    }

    private boolean isTimingDifference(
            Payment payment,
            Settlement settlement) {

        if (payment.getCapturedAt() == null ||
                settlement.getSettledAt() == null) {

            return false;
        }

        long seconds = Duration.between(
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
