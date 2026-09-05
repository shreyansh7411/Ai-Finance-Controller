package com.aifincontroller.service;

import com.aifincontroller.ai.dto.FinancialAnalysis;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class FinancialAnalysisService {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private final ReconciliationExceptionRepository exceptionRepository;
    private final ReconciliationResultRepository resultRepository;
    private final PaymentRepository paymentRepository;
    private final MerchantOrderRepository orderRepository;
    private final SettlementRepository settlementRepository;
    private final RefundRepository refundRepository;
    private final AdjustmentRepository adjustmentRepository;

    public FinancialAnalysisService(
            ReconciliationExceptionRepository exceptionRepository,
            ReconciliationResultRepository resultRepository,
            PaymentRepository paymentRepository,
            MerchantOrderRepository orderRepository,
            SettlementRepository settlementRepository,
            RefundRepository refundRepository,
            AdjustmentRepository adjustmentRepository) {

        this.exceptionRepository = exceptionRepository;
        this.resultRepository = resultRepository;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.settlementRepository = settlementRepository;
        this.refundRepository = refundRepository;
        this.adjustmentRepository = adjustmentRepository;
    }

    @Transactional(readOnly = true)
    public FinancialAnalysis analyze(Long exceptionId) {

        ReconciliationException exception =
                exceptionRepository.findById(exceptionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Exception not found: " + exceptionId));

        ReconciliationResult result =
                resultRepository.findById(
                                exception.getReconciliationResultId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Reconciliation result not found: "
                                                + exception.getReconciliationResultId()));

        FinancialAnalysis analysis = new FinancialAnalysis();

        BigDecimal expectedAmount = scale(result.getExpectedAmount());
        BigDecimal actualAmount = scale(result.getActualAmount());
        BigDecimal difference = scale(result.getDifference());

        analysis.setExpectedAmount(expectedAmount);
        analysis.setActualAmount(actualAmount);
        analysis.setReconciliationDifference(difference);

        Payment payment =
                paymentRepository.findByPaymentId(
                        result.getPaymentReference())
                        .orElse(null);

        if (payment == null) {

            analysis.setPaymentMatchesExpected(false);
            analysis.setPaymentMatchesOrder(false);
            analysis.setSettlementMatchesActual(false);
            analysis.setSettlementPresent(false);
            analysis.setRefundPresent(false);
            analysis.setAdjustmentPresent(false);

            analysis.setKnownDeductions(ZERO);
            analysis.setExplainedDifference(ZERO);
            analysis.setUnexplainedDifference(difference.abs());
            analysis.setDifferenceFullyExplained(false);

            analysis.setCandidateCauses(
                    List.of("The payment record required for investigation is unavailable."));

            analysis.setContradictions(List.of());

            analysis.setMissingEvidence(
                    List.of("Payment record"));

            analysis.setFinancialAssessment(
                    "The available records are not sufficient to determine the financial cause of this exception.");

            return analysis;
        }

        BigDecimal paymentAmount = scale(payment.getAmount());

        analysis.setPaymentAmount(paymentAmount);
        analysis.setPaymentMatchesExpected(
                paymentAmount.compareTo(expectedAmount) == 0);

        MerchantOrder order =
                orderRepository.findByOrderId(payment.getOrderId())
                        .orElse(null);

        if (order != null) {

            BigDecimal orderAmount = scale(order.getAmount());

            analysis.setOrderAmount(orderAmount);

            analysis.setPaymentMatchesOrder(
                    paymentAmount.compareTo(orderAmount) == 0);

        } else {

            analysis.setPaymentMatchesOrder(false);
            analysis.setMissingEvidence(
                    new ArrayList<>(List.of("Merchant order record")));
        }

        List<Settlement> settlements =
                settlementRepository.findByPaymentId(
                        payment.getPaymentId());

        analysis.setSettlementPresent(!settlements.isEmpty());

        BigDecimal settlementAmount = ZERO;
        BigDecimal settlementFees = ZERO;
        BigDecimal settlementTax = ZERO;
        BigDecimal adjustmentTotal = ZERO;

        if (!settlements.isEmpty()) {

            Settlement settlement = settlements.get(0);

            settlementAmount = scale(settlement.getAmount());
            settlementFees = scale(settlement.getFees());
            settlementTax = scale(settlement.getTax());

            analysis.setSettlementAmount(settlementAmount);
            analysis.setSettlementFees(settlementFees);
            analysis.setSettlementTax(settlementTax);

            BigDecimal netSettlement =
                    settlementAmount
                            .subtract(settlementFees)
                            .subtract(settlementTax)
                            .setScale(4, RoundingMode.HALF_UP);

            List<Adjustment> adjustments =
                    adjustmentRepository.findBySettlementId(
                            settlement.getSettlementId());

            adjustmentTotal = adjustments.stream()
                    .map(Adjustment::getAmount)
                    .map(this::scale)
                    .reduce(ZERO, BigDecimal::add)
                    .setScale(4, RoundingMode.HALF_UP);

            BigDecimal adjustedSettlement =
                    netSettlement
                            .add(adjustmentTotal)
                            .setScale(4, RoundingMode.HALF_UP);

            analysis.setAdjustmentPresent(!adjustments.isEmpty());

            analysis.setSettlementMatchesActual(
                    adjustedSettlement.compareTo(actualAmount) == 0);

        } else {

            analysis.setSettlementMatchesActual(false);
            analysis.setAdjustmentPresent(false);
        }

        List<Refund> refunds =
                refundRepository.findByPaymentId(
                        payment.getPaymentId());

        BigDecimal refundTotal = refunds.stream()
                .map(Refund::getAmount)
                .map(this::scale)
                .reduce(ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);

        analysis.setTotalRefundAmount(refundTotal);
        analysis.setRefundPresent(!refunds.isEmpty());
        analysis.setTotalAdjustmentAmount(adjustmentTotal);

        /*
         * Reconciliation semantics:
         *
         * Net settlement = gross settlement - fees - tax
         * Adjusted settlement = net settlement + adjustments
         *
         * Refunds are recorded as independent evidence and therefore
         * are not included in the reconciliation amount calculation.
         */
        BigDecimal knownDeductions =
                settlementFees
                        .add(settlementTax)
                        .setScale(4, RoundingMode.HALF_UP);

        analysis.setKnownDeductions(knownDeductions);

        BigDecimal explainedDifference = ZERO;

        if (!settlements.isEmpty()) {

            BigDecimal settlementContribution =
                    settlementFees
                            .add(settlementTax)
                            .subtract(adjustmentTotal)
                            .setScale(4, RoundingMode.HALF_UP);

            explainedDifference =
                    minAbs(difference.abs(), settlementContribution.abs());
        }

        analysis.setExplainedDifference(explainedDifference);

        BigDecimal unexplainedDifference =
                difference.abs()
                        .subtract(explainedDifference)
                        .max(ZERO)
                        .setScale(4, RoundingMode.HALF_UP);

        analysis.setUnexplainedDifference(unexplainedDifference);

        boolean fullyExplained =
                unexplainedDifference.compareTo(ZERO) == 0;

        analysis.setDifferenceFullyExplained(fullyExplained);

        List<String> candidateCauses = new ArrayList<>();
        List<String> contradictions = new ArrayList<>();
        List<String> missingEvidence =
                analysis.getMissingEvidence() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(analysis.getMissingEvidence());

        if (!analysis.isPaymentMatchesExpected()) {
            candidateCauses.add(
                    "The payment amount does not match the expected reconciliation amount.");
        }

        if (order != null && !analysis.isPaymentMatchesOrder()) {
            contradictions.add(
                    "The payment amount does not match the merchant order amount.");
        }

        if (settlements.isEmpty()) {

            candidateCauses.add(
                    "No settlement record is available for the payment.");

            missingEvidence.add("Settlement record");

        } else {

            if (settlementTax.compareTo(ZERO) > 0) {
                candidateCauses.add(
                        "A settlement tax deduction contributes to the difference.");
            }

            if (settlementFees.compareTo(ZERO) > 0) {
                candidateCauses.add(
                        "Settlement fees contribute to the difference.");
            }

            if (adjustmentTotal.compareTo(ZERO) != 0) {
                candidateCauses.add(
                        "Settlement adjustments affect the final settled amount.");
            }

            if (!analysis.isSettlementMatchesActual()) {
                contradictions.add(
                        "The calculated adjusted settlement does not match the recorded actual amount.");
            }
        }

        if (!refunds.isEmpty()) {
            candidateCauses.add(
                    "Refund activity exists for this payment and should be considered during investigation.");
        }

        if (fullyExplained) {

            analysis.setFinancialAssessment(
                    "The available financial records fully explain the reconciliation difference.");

        } else if (explainedDifference.compareTo(ZERO) > 0) {

            analysis.setFinancialAssessment(
                    "The available records explain part of the reconciliation difference, but a residual amount remains unexplained.");

        } else {

            analysis.setFinancialAssessment(
                    "The available financial records do not provide a direct explanation for the reconciliation difference.");
        }

        analysis.setCandidateCauses(candidateCauses);
        analysis.setContradictions(contradictions);
        analysis.setMissingEvidence(removeDuplicates(missingEvidence));

        return analysis;
    }

    private BigDecimal minAbs(
            BigDecimal first,
            BigDecimal second) {

        return first.compareTo(second) <= 0
                ? first
                : second;
    }

    private List<String> removeDuplicates(List<String> values) {
        return values.stream().distinct().toList();
    }

    private BigDecimal scale(BigDecimal value) {

        if (value == null) {
            return ZERO;
        }

        return value.setScale(
                4,
                RoundingMode.HALF_UP);
    }
}
