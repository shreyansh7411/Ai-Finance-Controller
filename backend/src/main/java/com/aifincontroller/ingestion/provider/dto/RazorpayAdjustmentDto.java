package com.aifincontroller.ingestion.provider.dto;

public record RazorpayAdjustmentDto(
        String id,
        String settlementId,
        Long amount,
        String type,
        String description,
        Long createdAt) {
}
