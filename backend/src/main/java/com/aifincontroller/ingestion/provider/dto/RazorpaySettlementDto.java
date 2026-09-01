package com.aifincontroller.ingestion.provider.dto;

public record RazorpaySettlementDto(
        String id,
        String paymentId,
        Long amount,
        Long fees,
        Long tax,
        String status,
        String utr,
        Long createdAt) {
}
