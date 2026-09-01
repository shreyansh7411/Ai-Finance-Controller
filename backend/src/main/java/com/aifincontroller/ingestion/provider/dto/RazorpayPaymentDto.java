package com.aifincontroller.ingestion.provider.dto;

public record RazorpayPaymentDto(
        String id,
        String orderId,
        Long amount,
        String currency,
        String status,
        Long capturedAt,
        Long createdAt) {
}
