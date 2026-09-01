package com.aifincontroller.ingestion.provider.dto;

public record RazorpayRefundDto(
        String id,
        String paymentId,
        Long amount,
        String status,
        Long createdAt) {
}
