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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiInvestigationEvidenceService {

    private final ReconciliationExceptionRepository exceptionRepository;
    private final ReconciliationResultRepository resultRepository;
    private final PaymentRepository paymentRepository;
    private final MerchantOrderRepository orderRepository;
    private final SettlementRepository settlementRepository;
    private final RefundRepository refundRepository;
    private final AdjustmentRepository adjustmentRepository;

    public AiInvestigationEvidenceService(
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
    public AiInvestigationRequest buildRequest(Long exceptionId) {

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

        Payment payment =
                paymentRepository.findByPaymentId(
                                result.getPaymentReference())
                        .orElse(null);

        AiInvestigationRequest request = new AiInvestigationRequest();

        request.setExceptionId(exception.getId());
        request.setExceptionType(exception.getType());
        request.setCategory(exception.getCategory());
        request.setSeverity(exception.getSeverity());
        request.setPaymentReference(result.getPaymentReference());
        request.setExpectedAmount(result.getExpectedAmount());
        request.setActualAmount(result.getActualAmount());
        request.setDifference(result.getDifference());
        request.setEvidenceSummary(exception.getEvidenceSummary());

        List<String> evidenceIds = new ArrayList<>();

        if (exception.getExpectedAmount() != null) {
            evidenceIds.add("EXCEPTION_EXPECTED_AMOUNT");
        }

        if (exception.getActualAmount() != null) {
            evidenceIds.add("EXCEPTION_ACTUAL_AMOUNT");
        }

        if (exception.getDifference() != null) {
            evidenceIds.add("EXCEPTION_DIFFERENCE");
        }

        if (exception.getEvidenceSummary() != null &&
                !exception.getEvidenceSummary().isBlank()) {
            evidenceIds.add("EVIDENCE_SUMMARY");
        }

        if (payment == null) {
            request.setRefunds(List.of());
            request.setAdjustments(List.of());
            request.setEvidenceIds(evidenceIds);
            return request;
        }

        request.setPaymentId(payment.getPaymentId());
        request.setOrderId(payment.getOrderId());
        request.setPayment(toPaymentEvidence(payment));

        evidenceIds.add("PAYMENT_ID");
        evidenceIds.add("PAYMENT_ORDER_ID");
        evidenceIds.add("PAYMENT_AMOUNT");
        evidenceIds.add("PAYMENT_CURRENCY");
        evidenceIds.add("PAYMENT_STATUS");

        if (payment.getCapturedAt() != null) {
            evidenceIds.add("PAYMENT_CAPTURED_AT");
        }

        if (payment.getCreatedAt() != null) {
            evidenceIds.add("PAYMENT_CREATED_AT");
        }

        orderRepository.findByOrderId(payment.getOrderId())
                .map(this::toOrderEvidence)
                .ifPresent(order -> {
                    request.setOrder(order);

                    evidenceIds.add("ORDER_ID");
                    evidenceIds.add("ORDER_AMOUNT");
                    evidenceIds.add("ORDER_CURRENCY");
                    evidenceIds.add("ORDER_STATUS");

                    if (order.getCreatedAt() != null) {
                        evidenceIds.add("ORDER_CREATED_AT");
                    }
                });

        List<Settlement> settlements =
                settlementRepository.findByPaymentId(
                        payment.getPaymentId());

        if (!settlements.isEmpty()) {

            Settlement settlement = settlements.get(0);

            request.setSettlement(
                    toSettlementEvidence(settlement));

            evidenceIds.add("SETTLEMENT_ID");
            evidenceIds.add("SETTLEMENT_PAYMENT_ID");
            evidenceIds.add("SETTLEMENT_AMOUNT");
            evidenceIds.add("SETTLEMENT_FEES");
            evidenceIds.add("SETTLEMENT_TAX");
            evidenceIds.add("SETTLEMENT_STATUS");

            if (settlement.getUtr() != null &&
                    !settlement.getUtr().isBlank()) {
                evidenceIds.add("SETTLEMENT_UTR");
            }

            if (settlement.getSettledAt() != null) {
                evidenceIds.add("SETTLEMENT_SETTLED_AT");
            }

            request.setAdjustments(
                    adjustmentRepository.findBySettlementId(
                                    settlement.getSettlementId())
                            .stream()
                            .map(adjustment -> {
                                evidenceIds.add("ADJUSTMENT_ID");
                                evidenceIds.add("ADJUSTMENT_SETTLEMENT_ID");
                                evidenceIds.add("ADJUSTMENT_AMOUNT");
                                evidenceIds.add("ADJUSTMENT_TYPE");

                                if (adjustment.getDescription() != null &&
                                        !adjustment.getDescription().isBlank()) {
                                    evidenceIds.add("ADJUSTMENT_DESCRIPTION");
                                }

                                if (adjustment.getCreatedAt() != null) {
                                    evidenceIds.add("ADJUSTMENT_CREATED_AT");
                                }

                                return toAdjustmentEvidence(adjustment);
                            })
                            .toList());

        } else {
            request.setAdjustments(List.of());
        }

        request.setRefunds(
                refundRepository.findByPaymentId(
                                payment.getPaymentId())
                        .stream()
                        .map(refund -> {
                            evidenceIds.add("REFUND_ID");
                            evidenceIds.add("REFUND_PAYMENT_ID");
                            evidenceIds.add("REFUND_AMOUNT");
                            evidenceIds.add("REFUND_STATUS");

                            if (refund.getCreatedAt() != null) {
                                evidenceIds.add("REFUND_CREATED_AT");
                            }

                            return toRefundEvidence(refund);
                        })
                        .toList());

        request.setEvidenceIds(evidenceIds);

        return request;
    }

    private AiInvestigationRequest.PaymentEvidence
    toPaymentEvidence(Payment payment) {
        AiInvestigationRequest.PaymentEvidence evidence =
                new AiInvestigationRequest.PaymentEvidence();
        evidence.setPaymentId(payment.getPaymentId());
        evidence.setOrderId(payment.getOrderId());
        evidence.setAmount(payment.getAmount());
        evidence.setCurrency(payment.getCurrency());
        evidence.setStatus(payment.getStatus());
        evidence.setCapturedAt(payment.getCapturedAt());
        evidence.setCreatedAt(payment.getCreatedAt());
        return evidence;
    }

    private AiInvestigationRequest.OrderEvidence
    toOrderEvidence(MerchantOrder order) {
        AiInvestigationRequest.OrderEvidence evidence =
                new AiInvestigationRequest.OrderEvidence();
        evidence.setOrderId(order.getOrderId());
        evidence.setAmount(order.getAmount());
        evidence.setCurrency(order.getCurrency());
        evidence.setStatus(order.getStatus());
        evidence.setCreatedAt(order.getCreatedAt());
        return evidence;
    }

    private AiInvestigationRequest.SettlementEvidence
    toSettlementEvidence(Settlement settlement) {
        AiInvestigationRequest.SettlementEvidence evidence =
                new AiInvestigationRequest.SettlementEvidence();
        evidence.setSettlementId(settlement.getSettlementId());
        evidence.setPaymentId(settlement.getPaymentId());
        evidence.setAmount(settlement.getAmount());
        evidence.setFees(settlement.getFees());
        evidence.setTax(settlement.getTax());
        evidence.setStatus(settlement.getStatus());
        evidence.setUtr(settlement.getUtr());
        evidence.setSettledAt(settlement.getSettledAt());
        return evidence;
    }

    private AiInvestigationRequest.RefundEvidence
    toRefundEvidence(Refund refund) {
        AiInvestigationRequest.RefundEvidence evidence =
                new AiInvestigationRequest.RefundEvidence();
        evidence.setRefundId(refund.getRefundId());
        evidence.setPaymentId(refund.getPaymentId());
        evidence.setAmount(refund.getAmount());
        evidence.setStatus(refund.getStatus());
        evidence.setCreatedAt(refund.getCreatedAt());
        return evidence;
    }

    private AiInvestigationRequest.AdjustmentEvidence
    toAdjustmentEvidence(Adjustment adjustment) {
        AiInvestigationRequest.AdjustmentEvidence evidence =
                new AiInvestigationRequest.AdjustmentEvidence();
        evidence.setAdjustmentId(adjustment.getAdjustmentId());
        evidence.setSettlementId(adjustment.getSettlementId());
        evidence.setAmount(adjustment.getAmount());
        evidence.setType(adjustment.getType());
        evidence.setDescription(adjustment.getDescription());
        evidence.setCreatedAt(adjustment.getCreatedAt());
        return evidence;
    }
}
