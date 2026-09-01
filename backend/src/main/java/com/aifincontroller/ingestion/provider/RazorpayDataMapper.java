package com.aifincontroller.ingestion.provider;

import com.aifincontroller.domain.Adjustment;
import com.aifincontroller.domain.Payment;
import com.aifincontroller.domain.Refund;
import com.aifincontroller.domain.Settlement;
import com.aifincontroller.ingestion.provider.dto.RazorpayAdjustmentDto;
import com.aifincontroller.ingestion.provider.dto.RazorpayPaymentDto;
import com.aifincontroller.ingestion.provider.dto.RazorpayRefundDto;
import com.aifincontroller.ingestion.provider.dto.RazorpaySettlementDto;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class RazorpayDataMapper {

    public Payment toPayment(
            RazorpayPaymentDto data,
            String batchId) {

        Payment payment = new Payment();

        payment.setPaymentId(required(data.id(), "id"));
        payment.setBatchId(batchId);
        payment.setOrderId(required(data.orderId(), "order_id"));
        payment.setAmount(amount(data.amount()));
        payment.setCurrency(required(data.currency(), "currency"));
        payment.setStatus(required(data.status(), "status"));
        payment.setCapturedAt(epochSeconds(data.capturedAt()));
        payment.setCreatedAt(epochSeconds(data.createdAt()));

        return payment;
    }

    public Settlement toSettlement(
            RazorpaySettlementDto data) {

        Settlement settlement = new Settlement();

        settlement.setSettlementId(required(data.id(), "id"));
        settlement.setPaymentId(data.paymentId());
        settlement.setAmount(amount(data.amount()));
        settlement.setFees(amount(data.fees()));
        settlement.setTax(amount(data.tax()));
        settlement.setStatus(data.status());
        settlement.setUtr(data.utr());
        settlement.setSettledAt(epochSeconds(data.createdAt()));

        return settlement;
    }

    public Refund toRefund(
            RazorpayRefundDto data) {

        Refund refund = new Refund();

        refund.setRefundId(required(data.id(), "id"));
        refund.setPaymentId(required(data.paymentId(), "payment_id"));
        refund.setAmount(amount(data.amount()));
        refund.setStatus(required(data.status(), "status"));
        refund.setCreatedAt(epochSeconds(data.createdAt()));

        return refund;
    }

    public Adjustment toAdjustment(
            RazorpayAdjustmentDto data) {

        Adjustment adjustment = new Adjustment();

        adjustment.setAdjustmentId(required(data.id(), "id"));
        adjustment.setSettlementId(data.settlementId());
        adjustment.setAmount(amount(data.amount()));
        adjustment.setType(required(data.type(), "type"));
        adjustment.setDescription(data.description());
        adjustment.setCreatedAt(epochSeconds(data.createdAt()));

        return adjustment;
    }

    private BigDecimal amount(Long value) {

        if (value == null) {
            return BigDecimal.ZERO.setScale(4);
        }

        return BigDecimal.valueOf(value)
                .divide(BigDecimal.valueOf(100));
    }

    private Instant epochSeconds(Long value) {

        if (value == null) {
            return null;
        }

        return Instant.ofEpochSecond(value);
    }

    private String required(
            String value,
            String field) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing required Razorpay field: " + field);
        }

        return value.trim();
    }
}
