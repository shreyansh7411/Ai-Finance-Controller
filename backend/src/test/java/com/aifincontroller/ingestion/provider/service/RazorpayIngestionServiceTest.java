package com.aifincontroller.ingestion.provider.service;

import com.aifincontroller.domain.Payment;
import com.aifincontroller.ingestion.domain.IngestionBatch;
import com.aifincontroller.ingestion.provider.RazorpayDataMapper;
import com.aifincontroller.ingestion.provider.dto.ProviderIngestionResult;
import com.aifincontroller.ingestion.service.IngestionBatchService;
import com.aifincontroller.repository.PaymentRepository;
import com.aifincontroller.repository.RefundRepository;
import com.aifincontroller.repository.SettlementRepository;
import com.aifincontroller.razorpay.RazorpayClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RazorpayIngestionServiceTest {

    @Test
    void importsNewPayments() {

        RazorpayClient client = mock(RazorpayClient.class);
        RazorpayDataMapper mapper = new RazorpayDataMapper();
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        SettlementRepository settlementRepository =
                mock(SettlementRepository.class);
        RefundRepository refundRepository =
                mock(RefundRepository.class);
        IngestionBatchService batchService =
                mock(IngestionBatchService.class);

        IngestionBatch batch = new IngestionBatch();
        batch.setBatchId("batch_test");

        when(batchService.createBatch(
                "PAYMENT",
                "razorpay-api"))
                .thenReturn(batch);

        when(client.getPayments(100, 0))
                .thenReturn(Map.of(
                        "items",
                        List.of(Map.of(
                                "id", "pay_test123",
                                "order_id", "order_test123",
                                "amount", 10000,
                                "currency", "INR",
                                "status", "captured"))));

        when(paymentRepository.existsByPaymentId("pay_test123"))
                .thenReturn(false);

        RazorpayIngestionService service =
                new RazorpayIngestionService(
                        client,
                        mapper,
                        paymentRepository,
                        settlementRepository,
                        refundRepository,
                        batchService);

        ProviderIngestionResult result =
                service.ingestPayments(100, 0);

        assertEquals("RAZORPAY", result.getProvider());
        assertEquals("PAYMENT", result.getEntityType());
        assertEquals(1, result.getFetchedRows());
        assertEquals(1, result.getImportedRows());
        assertEquals(0, result.getSkippedRows());
        assertEquals(0, result.getFailedRows());

        verify(paymentRepository)
                .save(any(Payment.class));

        verify(batchService).completeBatch(
                batch,
                1,
                1,
                0,
                0);
    }

    @Test
    void skipsExistingPayment() {

        RazorpayClient client = mock(RazorpayClient.class);
        RazorpayDataMapper mapper = new RazorpayDataMapper();
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        SettlementRepository settlementRepository =
                mock(SettlementRepository.class);
        RefundRepository refundRepository =
                mock(RefundRepository.class);
        IngestionBatchService batchService =
                mock(IngestionBatchService.class);

        IngestionBatch batch = new IngestionBatch();
        batch.setBatchId("batch_test");

        when(batchService.createBatch(
                "PAYMENT",
                "razorpay-api"))
                .thenReturn(batch);

        when(client.getPayments(100, 0))
                .thenReturn(Map.of(
                        "items",
                        List.of(Map.of(
                                "id", "pay_existing",
                                "order_id", "order_test",
                                "amount", 10000,
                                "currency", "INR",
                                "status", "captured"))));

        when(paymentRepository.existsByPaymentId("pay_existing"))
                .thenReturn(true);

        RazorpayIngestionService service =
                new RazorpayIngestionService(
                        client,
                        mapper,
                        paymentRepository,
                        settlementRepository,
                        refundRepository,
                        batchService);

        ProviderIngestionResult result =
                service.ingestPayments(100, 0);

        assertEquals(1, result.getFetchedRows());
        assertEquals(0, result.getImportedRows());
        assertEquals(1, result.getSkippedRows());
        assertEquals(0, result.getFailedRows());

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(batchService).completeBatch(
                batch,
                1,
                0,
                1,
                0);
    }

    @Test
    void marksInvalidPaymentAsFailed() {

        RazorpayClient client = mock(RazorpayClient.class);
        RazorpayDataMapper mapper = new RazorpayDataMapper();
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        SettlementRepository settlementRepository =
                mock(SettlementRepository.class);
        RefundRepository refundRepository =
                mock(RefundRepository.class);
        IngestionBatchService batchService =
                mock(IngestionBatchService.class);

        IngestionBatch batch = new IngestionBatch();
        batch.setBatchId("batch_test");

        when(batchService.createBatch(
                "PAYMENT",
                "razorpay-api"))
                .thenReturn(batch);

        when(client.getPayments(100, 0))
                .thenReturn(Map.of(
                        "items",
                        List.of(Map.of(
                                "order_id", "order_test",
                                "amount", 10000,
                                "currency", "INR",
                                "status", "captured"))));

        RazorpayIngestionService service =
                new RazorpayIngestionService(
                        client,
                        mapper,
                        paymentRepository,
                        settlementRepository,
                        refundRepository,
                        batchService);

        ProviderIngestionResult result =
                service.ingestPayments(100, 0);

        assertEquals(1, result.getFetchedRows());
        assertEquals(0, result.getImportedRows());
        assertEquals(0, result.getSkippedRows());
        assertEquals(1, result.getFailedRows());

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(batchService).completeBatch(
                batch,
                1,
                0,
                0,
                1);
    }

    @Test
    void marksBatchFailedWhenRazorpayCallFails() {

        RazorpayClient client = mock(RazorpayClient.class);
        RazorpayDataMapper mapper = new RazorpayDataMapper();
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        SettlementRepository settlementRepository =
                mock(SettlementRepository.class);
        RefundRepository refundRepository =
                mock(RefundRepository.class);
        IngestionBatchService batchService =
                mock(IngestionBatchService.class);

        IngestionBatch batch = new IngestionBatch();
        batch.setBatchId("batch_test");

        when(batchService.createBatch(
                "PAYMENT",
                "razorpay-api"))
                .thenReturn(batch);

        when(client.getPayments(100, 0))
                .thenThrow(new RuntimeException("API unavailable"));

        RazorpayIngestionService service =
                new RazorpayIngestionService(
                        client,
                        mapper,
                        paymentRepository,
                        settlementRepository,
                        refundRepository,
                        batchService);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.ingestPayments(100, 0));

        verify(batchService).failBatch(
                batch,
                0,
                0,
                0,
                0);
    }
}
