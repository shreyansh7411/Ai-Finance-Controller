package com.aifincontroller.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aifincontroller.domain.Payment;
import com.aifincontroller.ingestion.domain.IngestionBatch;
import com.aifincontroller.ingestion.dto.IngestionError;
import com.aifincontroller.ingestion.dto.IngestionResult;
import com.aifincontroller.ingestion.service.CsvIngestionService;
import com.aifincontroller.ingestion.service.IngestionBatchService;
import com.aifincontroller.ingestion.validation.CsvRowValidator;
import com.aifincontroller.repository.AdjustmentRepository;
import com.aifincontroller.repository.PaymentRepository;
import com.aifincontroller.repository.RefundRepository;
import com.aifincontroller.repository.SettlementRepository;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class CsvIngestionServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private AdjustmentRepository adjustmentRepository;

    @Mock
    private CsvRowValidator csvRowValidator;

    @Mock
    private IngestionBatchService ingestionBatchService;

    private CsvIngestionService service;

    @BeforeEach
    void setUp() {
        service = new CsvIngestionService(
                paymentRepository,
                settlementRepository,
                refundRepository,
                adjustmentRepository,
                csvRowValidator,
                ingestionBatchService);
    }

    @Test
    void validPaymentRowsAreImported() {
        IngestionBatch batch = new IngestionBatch();
        batch.setBatchId("batch_test");
        batch.setEntityType("PAYMENT");

        when(ingestionBatchService.createBatch(
                eq("PAYMENT"), eq("payments.csv")))
                .thenReturn(batch);

        when(csvRowValidator.validate(eq("PAYMENT"), any()))
                .thenReturn(Collections.emptyList());

        when(paymentRepository.existsByPaymentId("pay_1"))
                .thenReturn(false);

        when(ingestionBatchService.completeBatch(
                eq(batch), eq(1L), eq(1L), eq(0L), eq(0L)))
                .thenAnswer(invocation -> {
                    batch.setTotalRows(1);
                    batch.setImportedRows(1);
                    batch.setSkippedRows(0);
                    batch.setFailedRows(0);
                    return batch;
                });

        MockMultipartFile file = csv(
                "payments.csv",
                "payment_id,order_id,amount,currency,status,created_at,captured_at\n"
                        + "pay_1,order_1,100.5000,INR,captured,"
                        + "2026-08-31T06:00:00Z,2026-08-31T06:01:00Z");

        IngestionResult result = service.ingestPayments(file);

        assertThat(result.getBatchId()).isEqualTo("batch_test");
        assertThat(result.getEntityType()).isEqualTo("PAYMENT");
        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getImportedRows()).isEqualTo(1);
        assertThat(result.getSkippedRows()).isZero();
        assertThat(result.getFailedRows()).isZero();
        assertThat(result.getErrors()).isEmpty();

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void duplicatePaymentIsSkipped() {
        IngestionBatch batch = new IngestionBatch();
        batch.setBatchId("batch_test");
        batch.setEntityType("PAYMENT");

        when(ingestionBatchService.createBatch(
                eq("PAYMENT"), eq("payments.csv")))
                .thenReturn(batch);

        when(csvRowValidator.validate(eq("PAYMENT"), any()))
                .thenReturn(Collections.emptyList());

        when(paymentRepository.existsByPaymentId("pay_1"))
                .thenReturn(true);

        when(ingestionBatchService.completeBatch(
                eq(batch), eq(1L), eq(0L), eq(1L), eq(0L)))
                .thenAnswer(invocation -> {
                    batch.setTotalRows(1);
                    batch.setImportedRows(0);
                    batch.setSkippedRows(1);
                    batch.setFailedRows(0);
                    return batch;
                });

        MockMultipartFile file = csv(
                "payments.csv",
                "payment_id,order_id,amount,currency,status,created_at\n"
                        + "pay_1,order_1,100.5000,INR,captured,2026-08-31T06:00:00Z");

        IngestionResult result = service.ingestPayments(file);

        assertThat(result.getImportedRows()).isZero();
        assertThat(result.getSkippedRows()).isEqualTo(1);
        assertThat(result.getFailedRows()).isZero();

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void invalidRowsAreCountedAsFailures() {
        IngestionBatch batch = new IngestionBatch();
        batch.setBatchId("batch_test");
        batch.setEntityType("PAYMENT");

        when(ingestionBatchService.createBatch(
                eq("PAYMENT"), eq("payments.csv")))
                .thenReturn(batch);

        when(csvRowValidator.validate(eq("PAYMENT"), any()))
                .thenReturn(List.of(
                        new IngestionError(
                                1,
                                "amount",
                                "Invalid decimal value")));

        when(ingestionBatchService.completeBatch(
                eq(batch), eq(1L), eq(0L), eq(0L), eq(1L)))
                .thenAnswer(invocation -> {
                    batch.setTotalRows(1);
                    batch.setImportedRows(0);
                    batch.setSkippedRows(0);
                    batch.setFailedRows(1);
                    return batch;
                });

        MockMultipartFile file = csv(
                "payments.csv",
                "payment_id,order_id,amount,currency,status,created_at\n"
                        + "pay_1,order_1,bad,INR,captured,2026-08-31T06:00:00Z");

        IngestionResult result = service.ingestPayments(file);

        assertThat(result.getFailedRows()).isEqualTo(1);
        assertThat(result.getImportedRows()).isZero();
        assertThat(result.getErrors()).hasSize(1);

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    private MockMultipartFile csv(String filename, String content) {
        return new MockMultipartFile(
                "file",
                filename,
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
