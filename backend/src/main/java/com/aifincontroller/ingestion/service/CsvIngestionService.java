package com.aifincontroller.ingestion.service;

import com.aifincontroller.domain.Adjustment;
import com.aifincontroller.domain.Payment;
import com.aifincontroller.domain.Refund;
import com.aifincontroller.domain.Settlement;
import com.aifincontroller.repository.AdjustmentRepository;
import com.aifincontroller.repository.PaymentRepository;
import com.aifincontroller.repository.RefundRepository;
import com.aifincontroller.repository.SettlementRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CsvIngestionService {

    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final RefundRepository refundRepository;
    private final AdjustmentRepository adjustmentRepository;

    public CsvIngestionService(
            PaymentRepository paymentRepository,
            SettlementRepository settlementRepository,
            RefundRepository refundRepository,
            AdjustmentRepository adjustmentRepository) {

        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
        this.refundRepository = refundRepository;
        this.adjustmentRepository = adjustmentRepository;
    }

    public int ingestPayments(MultipartFile file, String batchId) {
        int imported = 0;

        for (CSVRecord record : parse(file)) {
            String paymentId = value(record, "payment_id");

            if (paymentId.isBlank() || paymentRepository.existsByPaymentId(paymentId)) {
                continue;
            }

            Payment payment = new Payment();
            payment.setPaymentId(paymentId);
            payment.setBatchId(batchId);
            payment.setOrderId(value(record, "order_id"));
            payment.setAmount(decimal(record, "amount"));
            payment.setCurrency(value(record, "currency"));
            payment.setStatus(value(record, "status"));
            payment.setCapturedAt(instant(record, "captured_at"));
            payment.setCreatedAt(instant(record, "created_at"));

            paymentRepository.save(payment);
            imported++;
        }

        return imported;
    }

    public int ingestSettlements(MultipartFile file) {
        int imported = 0;

        for (CSVRecord record : parse(file)) {
            String settlementId = value(record, "settlement_id");
            String paymentId = value(record, "payment_id");

            if (settlementId.isBlank()) {
                continue;
            }

            if (settlementRepository
                    .findBySettlementIdAndPaymentId(settlementId, paymentId)
                    .isPresent()) {
                continue;
            }

            Settlement settlement = new Settlement();
            settlement.setSettlementId(settlementId);
            settlement.setPaymentId(emptyToNull(paymentId));
            settlement.setAmount(decimal(record, "amount"));
            settlement.setFees(decimal(record, "fees"));
            settlement.setTax(decimal(record, "tax"));
            settlement.setStatus(value(record, "status"));
            settlement.setUtr(emptyToNull(value(record, "utr")));
            settlement.setSettledAt(instant(record, "settled_at"));

            settlementRepository.save(settlement);
            imported++;
        }

        return imported;
    }

    public int ingestRefunds(MultipartFile file) {
        int imported = 0;

        for (CSVRecord record : parse(file)) {
            String refundId = value(record, "refund_id");

            if (refundId.isBlank() || refundRepository.existsByRefundId(refundId)) {
                continue;
            }

            Refund refund = new Refund();
            refund.setRefundId(refundId);
            refund.setPaymentId(value(record, "payment_id"));
            refund.setAmount(decimal(record, "amount"));
            refund.setStatus(value(record, "status"));
            refund.setCreatedAt(instant(record, "created_at"));

            refundRepository.save(refund);
            imported++;
        }

        return imported;
    }

    public int ingestAdjustments(MultipartFile file) {
        int imported = 0;

        for (CSVRecord record : parse(file)) {
            String adjustmentId = value(record, "adjustment_id");

            if (adjustmentId.isBlank()
                    || adjustmentRepository.existsByAdjustmentId(adjustmentId)) {
                continue;
            }

            Adjustment adjustment = new Adjustment();
            adjustment.setAdjustmentId(adjustmentId);
            adjustment.setSettlementId(
                    emptyToNull(value(record, "settlement_id")));
            adjustment.setAmount(decimal(record, "amount"));
            adjustment.setType(value(record, "type"));
            adjustment.setDescription(
                    emptyToNull(value(record, "description")));
            adjustment.setCreatedAt(instant(record, "created_at"));

            adjustmentRepository.save(adjustment);
            imported++;
        }

        return imported;
    }

    private List<CSVRecord> parse(MultipartFile file) {
        try {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .get();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            file.getInputStream(),
                            StandardCharsets.UTF_8));
                 CSVParser parser = format.parse(reader)) {

                return parser.getRecords();
            }

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to parse CSV file: " + e.getMessage(), e);
        }
    }

    private String value(CSVRecord record, String column) {
        if (!record.isMapped(column)) {
            throw new IllegalArgumentException(
                    "Missing required CSV column: " + column);
        }

        return record.get(column).trim();
    }

    private BigDecimal decimal(CSVRecord record, String column) {
        String value = value(record, column);

        if (value.isBlank()) {
            return BigDecimal.ZERO.setScale(4);
        }

        return new BigDecimal(value);
    }

    private Instant instant(CSVRecord record, String column) {
        String value = value(record, column);

        if (value.isBlank()) {
            return null;
        }

        return Instant.parse(value);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
