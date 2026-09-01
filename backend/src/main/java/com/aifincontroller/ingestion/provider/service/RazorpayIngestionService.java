package com.aifincontroller.ingestion.provider.service;

import com.aifincontroller.domain.Payment;
import com.aifincontroller.domain.Refund;
import com.aifincontroller.domain.Settlement;
import com.aifincontroller.ingestion.domain.IngestionBatch;
import com.aifincontroller.ingestion.provider.RazorpayDataMapper;
import com.aifincontroller.ingestion.provider.dto.ProviderIngestionResult;
import com.aifincontroller.ingestion.provider.dto.RazorpayPaymentDto;
import com.aifincontroller.ingestion.provider.dto.RazorpayRefundDto;
import com.aifincontroller.ingestion.provider.dto.RazorpaySettlementDto;
import com.aifincontroller.ingestion.service.IngestionBatchService;
import com.aifincontroller.repository.PaymentRepository;
import com.aifincontroller.repository.RefundRepository;
import com.aifincontroller.repository.SettlementRepository;
import com.aifincontroller.razorpay.RazorpayClient;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RazorpayIngestionService {

    private static final String PROVIDER = "RAZORPAY";
    private static final String PAYMENT = "PAYMENT";
    private static final String SETTLEMENT = "SETTLEMENT";
    private static final String REFUND = "REFUND";

    private final RazorpayClient razorpayClient;
    private final RazorpayDataMapper dataMapper;
    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final RefundRepository refundRepository;
    private final IngestionBatchService ingestionBatchService;

    public RazorpayIngestionService(
            RazorpayClient razorpayClient,
            RazorpayDataMapper dataMapper,
            PaymentRepository paymentRepository,
            SettlementRepository settlementRepository,
            RefundRepository refundRepository,
            IngestionBatchService ingestionBatchService) {

        this.razorpayClient = razorpayClient;
        this.dataMapper = dataMapper;
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
        this.refundRepository = refundRepository;
        this.ingestionBatchService = ingestionBatchService;
    }

    @Transactional
    public ProviderIngestionResult ingestPayments(
            int count,
            int skip) {

        IngestionBatch batch = ingestionBatchService.createBatch(
                PAYMENT,
                "razorpay-api");

        long imported = 0;
        long skipped = 0;
        long failed = 0;

        try {
            List<Map<String, Object>> items = extractItems(razorpayClient.getPayments(count, skip));

            for (Map<String, Object> item : items) {
                try {
                    RazorpayPaymentDto dto = new RazorpayPaymentDto(
                            requiredString(item, "id"),
                            requiredString(item, "order_id"),
                            longValue(item, "amount"),
                            requiredString(item, "currency"),
                            requiredString(item, "status"),
                            longValue(item, "captured_at"),
                            longValue(item, "created_at"));

                    if (paymentRepository.existsByPaymentId(dto.id())) {
                        skipped++;
                        continue;
                    }

                    Payment payment = dataMapper.toPayment(
                            dto,
                            batch.getBatchId());

                    paymentRepository.save(payment);
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
                    PAYMENT,
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
                    "Razorpay payment ingestion failed for batch "
                            + batch.getBatchId()
                            + ": "
                            + e.getMessage(),
                    e);
        }
    }

    @Transactional
    public ProviderIngestionResult ingestSettlements(
            int count,
            int skip) {

        IngestionBatch batch = ingestionBatchService.createBatch(
                SETTLEMENT,
                "razorpay-api");

        long imported = 0;
        long skipped = 0;
        long failed = 0;

        try {
            List<Map<String, Object>> items = extractItems(razorpayClient.getSettlements(count, skip));

            for (Map<String, Object> item : items) {
                try {
                    RazorpaySettlementDto dto = new RazorpaySettlementDto(
                            requiredString(item, "id"),
                            string(item, "payment_id"),
                            longValue(item, "amount"),
                            longValue(item, "fees"),
                            longValue(item, "tax"),
                            string(item, "status"),
                            string(item, "utr"),
                            longValue(item, "created_at"));

                    if (settlementRepository
                            .findBySettlementIdAndPaymentId(
                                    dto.id(),
                                    dto.paymentId())
                            .isPresent()) {
                        skipped++;
                        continue;
                    }

                    Settlement settlement = dataMapper.toSettlement(dto);

                    settlementRepository.save(settlement);
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
                    SETTLEMENT,
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
                    "Razorpay settlement ingestion failed for batch "
                            + batch.getBatchId()
                            + ": "
                            + e.getMessage(),
                    e);
        }
    }

    @Transactional
    public ProviderIngestionResult ingestRefunds(
            int count,
            int skip) {

        IngestionBatch batch = ingestionBatchService.createBatch(
                REFUND,
                "razorpay-api");

        long imported = 0;
        long skipped = 0;
        long failed = 0;

        try {
            List<Map<String, Object>> items = extractItems(razorpayClient.getRefunds(count, skip));

            for (Map<String, Object> item : items) {
                try {
                    RazorpayRefundDto dto = new RazorpayRefundDto(
                            requiredString(item, "id"),
                            requiredString(item, "payment_id"),
                            longValue(item, "amount"),
                            requiredString(item, "status"),
                            longValue(item, "created_at"));

                    if (refundRepository.existsByRefundId(dto.id())) {
                        skipped++;
                        continue;
                    }

                    Refund refund = dataMapper.toRefund(dto);

                    refundRepository.save(refund);
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
                    REFUND,
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
                    "Razorpay refund ingestion failed for batch "
                            + batch.getBatchId()
                            + ": "
                            + e.getMessage(),
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
