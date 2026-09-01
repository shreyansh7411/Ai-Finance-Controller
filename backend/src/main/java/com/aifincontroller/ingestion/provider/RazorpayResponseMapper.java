package com.aifincontroller.ingestion.provider;

import com.aifincontroller.ingestion.provider.dto.RazorpayAdjustmentDto;
import com.aifincontroller.ingestion.provider.dto.RazorpayPaymentDto;
import com.aifincontroller.ingestion.provider.dto.RazorpayRefundDto;
import com.aifincontroller.ingestion.provider.dto.RazorpaySettlementDto;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RazorpayResponseMapper {

    public RazorpayPaymentDto toPayment(Map<String, Object> data) {
        return new RazorpayPaymentDto(
                string(data, "id"),
                string(data, "order_id"),
                longValue(data, "amount"),
                string(data, "currency"),
                string(data, "status"),
                longValue(data, "captured_at"),
                longValue(data, "created_at"));
    }

    public RazorpaySettlementDto toSettlement(Map<String, Object> data) {
        return new RazorpaySettlementDto(
                string(data, "id"),
                string(data, "payment_id"),
                longValue(data, "amount"),
                longValue(data, "fees"),
                longValue(data, "tax"),
                string(data, "status"),
                string(data, "utr"),
                longValue(data, "created_at"));
    }

    public RazorpayRefundDto toRefund(Map<String, Object> data) {
        return new RazorpayRefundDto(
                string(data, "id"),
                string(data, "payment_id"),
                longValue(data, "amount"),
                string(data, "status"),
                longValue(data, "created_at"));
    }

    public RazorpayAdjustmentDto toAdjustment(
            Map<String, Object> data) {

        return new RazorpayAdjustmentDto(
                string(data, "id"),
                string(data, "settlement_id"),
                longValue(data, "amount"),
                string(data, "type"),
                string(data, "description"),
                longValue(data, "created_at"));
    }

    private String string(
            Map<String, Object> data,
            String field) {

        Object value = data.get(field);

        if (value == null) {
            return null;
        }

        String result = value.toString().trim();

        return result.isBlank() ? null : result;
    }

    private Long longValue(
            Map<String, Object> data,
            String field) {

        Object value = data.get(field);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(value.toString());
    }
}
