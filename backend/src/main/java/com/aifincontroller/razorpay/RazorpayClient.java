package com.aifincontroller.razorpay;

import java.util.Map;

public interface RazorpayClient {

    Map<String, Object> getPayment(String paymentId);

    Map<String, Object> getPayments(int count, int skip);

    Map<String, Object> getPaymentsForOrder(
            String orderId,
            int count,
            int skip);

    Map<String, Object> getSettlement(String settlementId);

    Map<String, Object> getSettlements(
            int count,
            int skip);

    Map<String, Object> getSettlementRecon(
            int year,
            int month,
            Integer day,
            int count,
            int skip);

    Map<String, Object> getRefund(String refundId);

    Map<String, Object> getRefunds(
            int count,
            int skip);
}
