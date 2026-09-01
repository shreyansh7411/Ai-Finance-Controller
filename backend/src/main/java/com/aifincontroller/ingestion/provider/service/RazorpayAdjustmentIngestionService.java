package com.aifincontroller.ingestion.provider.service;

import com.aifincontroller.domain.Adjustment;
import com.aifincontroller.ingestion.domain.IngestionBatch;
import com.aifincontroller.ingestion.provider.RazorpayDataMapper;
import com.aifincontroller.ingestion.provider.dto.ProviderIngestionResult;
import com.aifincontroller.ingestion.provider.dto.RazorpayAdjustmentDto;
import com.aifincontroller.ingestion.service.IngestionBatchService;
import com.aifincontroller.repository.AdjustmentRepository;
import com.aifincontroller.razorpay.RazorpayClient;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RazorpayAdjustmentIngestionService {

    private static final String PROVIDER = "RAZORPAY";
    private static final String ENTITY_TYPE = "ADJUSTMENT";

    private final RazorpayClient razorpayClient;
    private final RazorpayDataMapper dataMapper;
    private final AdjustmentRepository adjustmentRepository;
    private final IngestionBatchService ingestionBatchService;

    public RazorpayAdjustmentIngestionService(
            RazorpayClient razorpayClient,
            RazorpayDataMapper dataMapper,
            AdjustmentRepository adjustmentRepository,
            IngestionBatchService ingestionBatchService) {

        this.razorpayClient = razorpayClient;
        this.dataMapper = dataMapper;
        this.adjustmentRepository = adjustmentRepository;
        this.ingestionBatchService = ingestionBatchService;
    }

    @Transactional
    public ProviderIngestionResult ingestAdjustments(
            int year,
            int month,
            Integer day,
            int count,
            int skip) {

        IngestionBatch batch = ingestionBatchService.createBatch(
                ENTITY_TYPE,
                "razorpay-api");

        long imported = 0;
        long skipped = 0;
        long failed = 0;

        try {
            List<Map<String, Object>> items = extractItems(
                    razorpayClient.getSettlementRecon(
                            year,
                            month,
                            day,
                            count,
                            skip));

            for (Map<String, Object> item : items) {
                try {
                    RazorpayAdjustmentDto dto = new RazorpayAdjustmentDto(
                            requiredString(item, "id"),
                            requiredString(item, "settlement_id"),
                            longValue(item, "amount"),
                            requiredString(item, "type"),
                            string(item, "description"),
                            longValue(item, "created_at"));

                    if (adjustmentRepository
                            .existsByAdjustmentId(dto.id())) {
                        skipped++;
                        continue;
                    }

                    Adjustment adjustment = dataMapper.toAdjustment(dto);

                    adjustmentRepository.save(adjustment);
                    imported++;

                } catch (Exception e) {
                    failed++;
                }
            }

            ingestionBatchService.completeBatch(
                    batch,
                    items.size(),
                    imported,
                    skipped,
                    failed);

            return new ProviderIngestionResult(
                    PROVIDER,
                    ENTITY_TYPE,
                    items.size(),
                    imported,
                    skipped,
                    failed);

        } catch (Exception e) {

            ingestionBatchService.failBatch(
                    batch,
                    0,
                    imported,
                    skipped,
                    failed);

            throw new IllegalArgumentException(
                    "Razorpay adjustment ingestion failed for batch "
                            + batch.getBatchId()
                            + ": " + e.getMessage(),
                    e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItems(
            Map<String, Object> response) {

        Object items = response.get("items");

        if (items == null) {
            return List.of();
        }

        if (!(items instanceof List<?>)) {
            throw new IllegalArgumentException(
                    "Invalid Razorpay response: items is not a list.");
        }

        return (List<Map<String, Object>>) items;
    }

    private String requiredString(
            Map<String, Object> data,
            String field) {

        String value = string(data, field);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing required Razorpay field: " + field);
        }

        return value;
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
