package com.aifincontroller.service;

import com.aifincontroller.domain.Adjustment;
import com.aifincontroller.domain.MerchantOrder;
import com.aifincontroller.domain.Payment;
import com.aifincontroller.domain.Refund;
import com.aifincontroller.domain.Settlement;
import com.aifincontroller.domain.SyntheticGroundTruth;
import com.aifincontroller.domain.SyntheticScenario;
import com.aifincontroller.dto.SyntheticGenerationResponse;
import com.aifincontroller.repository.AdjustmentRepository;
import com.aifincontroller.repository.MerchantOrderRepository;
import com.aifincontroller.repository.PaymentRepository;
import com.aifincontroller.repository.RefundRepository;
import com.aifincontroller.repository.SettlementRepository;
import com.aifincontroller.repository.SyntheticGroundTruthRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyntheticDataGeneratorService {

        private static final BigDecimal MIN_AMOUNT = new BigDecimal("100.00");
        private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000.00");

        private final MerchantOrderRepository orderRepository;
        private final PaymentRepository paymentRepository;
        private final SettlementRepository settlementRepository;
        private final RefundRepository refundRepository;
        private final AdjustmentRepository adjustmentRepository;
        private final SyntheticGroundTruthRepository groundTruthRepository;

        public SyntheticDataGeneratorService(
                        MerchantOrderRepository orderRepository,
                        PaymentRepository paymentRepository,
                        SettlementRepository settlementRepository,
                        RefundRepository refundRepository,
                        AdjustmentRepository adjustmentRepository,
                        SyntheticGroundTruthRepository groundTruthRepository) {

                this.orderRepository = orderRepository;
                this.paymentRepository = paymentRepository;
                this.settlementRepository = settlementRepository;
                this.refundRepository = refundRepository;
                this.adjustmentRepository = adjustmentRepository;
                this.groundTruthRepository = groundTruthRepository;
        }

        @Transactional
        public SyntheticGenerationResponse generate(int count) {

                String batchId = "synthetic_" + UUID.randomUUID();

                Map<SyntheticScenario, Integer> scenarioCounts = new EnumMap<>(SyntheticScenario.class);

                for (SyntheticScenario scenario : SyntheticScenario.values()) {
                        scenarioCounts.put(scenario, 0);
                }

                int generatedOrders = 0;
                int generatedPayments = 0;
                int generatedSettlements = 0;
                int generatedRefunds = 0;
                int generatedAdjustments = 0;

                for (int i = 0; i < count; i++) {

                        SyntheticScenario scenario = selectScenario();
                        scenarioCounts.merge(scenario, 1, Integer::sum);

                        String suffix = UUID.randomUUID().toString().replace("-", "");

                        String orderId = "order_syn_" + suffix;
                        String paymentId = "pay_syn_" + suffix;
                        String settlementId = "setl_syn_" + suffix;

                        BigDecimal amount = randomAmount();

                        Instant createdAt = Instant.now().minusSeconds(
                                        ThreadLocalRandom.current().nextLong(
                                                        0,
                                                        30L * 24 * 60 * 60));

                        MerchantOrder order = new MerchantOrder();
                        order.setOrderId(orderId);
                        order.setAmount(amount);
                        order.setCurrency("INR");
                        order.setStatus("paid");
                        order.setCreatedAt(createdAt);

                        orderRepository.save(order);
                        generatedOrders++;

                        Payment payment = new Payment();
                        payment.setPaymentId(paymentId);
                        payment.setBatchId(batchId);
                        payment.setOrderId(orderId);
                        payment.setAmount(amount);
                        payment.setCurrency("INR");
                        payment.setStatus("captured");
                        payment.setCapturedAt(createdAt.plusSeconds(60));
                        payment.setCreatedAt(createdAt);

                        paymentRepository.save(payment);
                        generatedPayments++;

                        BigDecimal expectedDifference = calculateExpectedDifference(
                                        amount,
                                        scenario);

                        boolean hasSettlement = scenario != SyntheticScenario.MISSING_SETTLEMENT;

                        if (hasSettlement) {

                                BigDecimal settlementAmount = calculateSettlementAmount(amount, scenario);

                                Settlement settlement = new Settlement();
                                settlement.setSettlementId(settlementId);
                                settlement.setPaymentId(paymentId);
                                settlement.setAmount(settlementAmount);
                                settlement.setFees(calculateFees(amount, scenario));
                                settlement.setTax(calculateTax(amount, scenario));
                                settlement.setStatus("processed");
                                settlement.setUtr("UTR" + suffix);

                                settlement.setSettledAt(
                                                createdAt.plusSeconds(
                                                                scenario == SyntheticScenario.TIMING_DIFFERENCE
                                                                                ? 7L * 24 * 60 * 60
                                                                                : 24L * 60 * 60));

                                settlementRepository.save(settlement);
                                generatedSettlements++;
                        }

                        if (scenario == SyntheticScenario.REFUND) {

                                Refund refund = new Refund();
                                refund.setRefundId("rfnd_syn_" + suffix);
                                refund.setPaymentId(paymentId);
                                refund.setAmount(
                                                amount.multiply(new BigDecimal("0.2500")));
                                refund.setStatus("processed");
                                refund.setCreatedAt(createdAt.plusSeconds(3600));

                                refundRepository.save(refund);
                                generatedRefunds++;
                        }

                        if (scenario == SyntheticScenario.ADJUSTMENT) {

                                Adjustment adjustment = new Adjustment();
                                adjustment.setAdjustmentId("adj_syn_" + suffix);
                                adjustment.setSettlementId(settlementId);
                                adjustment.setAmount(new BigDecimal("-25.0000"));
                                adjustment.setType("fee_credit");
                                adjustment.setDescription("Synthetic adjustment");
                                adjustment.setCreatedAt(createdAt.plusSeconds(7200));

                                adjustmentRepository.save(adjustment);
                                generatedAdjustments++;
                        }

                        /*
                         * DUPLICATE means the same order has two captured payments.
                         * Payment IDs remain unique, allowing the database to accept
                         * the records while the future reconciliation engine detects
                         * the duplicate relationship.
                         */
                        if (scenario == SyntheticScenario.DUPLICATE) {

                                Payment duplicatePayment = new Payment();
                                duplicatePayment.setPaymentId(
                                                "pay_dup_syn_" + suffix);
                                duplicatePayment.setBatchId(batchId);
                                duplicatePayment.setOrderId(orderId);
                                duplicatePayment.setAmount(amount);
                                duplicatePayment.setCurrency("INR");
                                duplicatePayment.setStatus("captured");
                                duplicatePayment.setCapturedAt(createdAt.plusSeconds(120));
                                duplicatePayment.setCreatedAt(createdAt);

                                paymentRepository.save(duplicatePayment);
                                generatedPayments++;
                        }

                        /*
                         * Ground truth is stored separately from reconciliation results.
                         * The reconciliation engine must independently determine
                         * the correct outcome later.
                         */
                        SyntheticGroundTruth groundTruth = new SyntheticGroundTruth();
                        groundTruth.setBatchId(batchId);
                        groundTruth.setScenario(scenario);
                        groundTruth.setOrderId(orderId);
                        groundTruth.setPaymentId(paymentId);
                        groundTruth.setSettlementId(
                                        hasSettlement ? settlementId : null);
                        groundTruth.setExpectedOutcome(expectedOutcome(scenario));
                        groundTruth.setExpectedDifference(expectedDifference);

                        groundTruthRepository.save(groundTruth);
                }

                Map<String, Integer> responseScenarios = new LinkedHashMap<>();

                for (Map.Entry<SyntheticScenario, Integer> entry : scenarioCounts.entrySet()) {

                        responseScenarios.put(
                                        entry.getKey().name(),
                                        entry.getValue());
                }

                return new SyntheticGenerationResponse(
                                batchId,
                                count,
                                generatedOrders,
                                generatedPayments,
                                generatedSettlements,
                                generatedRefunds,
                                generatedAdjustments,
                                responseScenarios);
        }

        private SyntheticScenario selectScenario() {

                SyntheticScenario[] scenarios = SyntheticScenario.values();

                return scenarios[ThreadLocalRandom.current().nextInt(scenarios.length)];
        }

        private BigDecimal randomAmount() {

                double value = ThreadLocalRandom.current().nextDouble(
                                MIN_AMOUNT.doubleValue(),
                                MAX_AMOUNT.doubleValue());

                return BigDecimal.valueOf(value)
                                .setScale(4, RoundingMode.HALF_UP);
        }

        private BigDecimal calculateFees(
                        BigDecimal amount,
                        SyntheticScenario scenario) {

                BigDecimal fees = amount.multiply(
                                new BigDecimal("0.0200"));

                if (scenario == SyntheticScenario.FEE_DIFFERENCE) {
                        fees = fees.add(new BigDecimal("15.0000"));
                }

                return fees.setScale(4, RoundingMode.HALF_UP);
        }

        private BigDecimal calculateTax(
                        BigDecimal amount,
                        SyntheticScenario scenario) {

                BigDecimal tax = amount.multiply(
                                new BigDecimal("0.0036"));

                if (scenario == SyntheticScenario.TAX_DIFFERENCE) {
                        tax = tax.add(new BigDecimal("10.0000"));
                }

                return tax.setScale(4, RoundingMode.HALF_UP);
        }

        private BigDecimal calculateSettlementAmount(
                        BigDecimal amount,
                        SyntheticScenario scenario) {

                BigDecimal settlementAmount = amount;

                if (scenario == SyntheticScenario.FEE_DIFFERENCE) {
                        settlementAmount = settlementAmount.subtract(
                                        new BigDecimal("15.0000"));
                }

                if (scenario == SyntheticScenario.TAX_DIFFERENCE) {
                        settlementAmount = settlementAmount.subtract(
                                        new BigDecimal("10.0000"));
                }

                if (scenario == SyntheticScenario.UNEXPLAINED_MISMATCH) {
                        settlementAmount = settlementAmount.subtract(
                                        new BigDecimal("137.4200"));
                }

                return settlementAmount
                                .max(BigDecimal.ZERO)
                                .setScale(4, RoundingMode.HALF_UP);
        }

        private BigDecimal calculateExpectedDifference(
                        BigDecimal amount,
                        SyntheticScenario scenario) {

                return switch (scenario) {
                        case EXACT_MATCH,
                                        REFUND,
                                        ADJUSTMENT,
                                        TIMING_DIFFERENCE,
                                        MISSING_SETTLEMENT,
                                        DUPLICATE ->
                                BigDecimal.ZERO.setScale(4);

                        case FEE_DIFFERENCE ->
                                new BigDecimal("15.0000");

                        case TAX_DIFFERENCE ->
                                new BigDecimal("10.0000");

                        case UNEXPLAINED_MISMATCH ->
                                new BigDecimal("137.4200");
                };
        }

        private String expectedOutcome(SyntheticScenario scenario) {

                return switch (scenario) {
                        case EXACT_MATCH -> "MATCHED";
                        case FEE_DIFFERENCE -> "FEE_MISMATCH";
                        case TAX_DIFFERENCE -> "TAX_MISMATCH";
                        case REFUND -> "REFUND_ADJUSTED";
                        case ADJUSTMENT -> "ADJUSTMENT_REQUIRED";
                        case TIMING_DIFFERENCE -> "TIMING_DIFFERENCE";
                        case MISSING_SETTLEMENT -> "MISSING_SETTLEMENT";
                        case DUPLICATE -> "DUPLICATE_PAYMENT";
                        case UNEXPLAINED_MISMATCH -> "UNEXPLAINED_MISMATCH";
                };
        }
}