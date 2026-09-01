package com.aifincontroller.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aifincontroller.ingestion.domain.IngestionBatch;
import com.aifincontroller.ingestion.domain.IngestionBatchStatus;
import com.aifincontroller.ingestion.repository.IngestionBatchRepository;
import com.aifincontroller.ingestion.service.IngestionBatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IngestionBatchServiceTest {

    @Mock
    private IngestionBatchRepository batchRepository;

    private IngestionBatchService service;

    @BeforeEach
    void setUp() {
        service = new IngestionBatchService(batchRepository);
    }

    @Test
    void createBatchInitializesProcessingBatch() {
        when(batchRepository.save(any(IngestionBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IngestionBatch batch =
                service.createBatch("PAYMENT", "payments.csv");

        assertThat(batch.getBatchId()).startsWith("batch_");
        assertThat(batch.getEntityType()).isEqualTo("PAYMENT");
        assertThat(batch.getFilename()).isEqualTo("payments.csv");
        assertThat(batch.getStatus()).isEqualTo(IngestionBatchStatus.PROCESSING);
        assertThat(batch.getTotalRows()).isZero();
        assertThat(batch.getImportedRows()).isZero();
        assertThat(batch.getSkippedRows()).isZero();
        assertThat(batch.getFailedRows()).isZero();
        assertThat(batch.getStartedAt()).isNotNull();

        verify(batchRepository).save(batch);
    }

    @Test
    void completeBatchWithoutFailuresIsMarkedCompleted() {
        IngestionBatch batch = new IngestionBatch();

        when(batchRepository.save(any(IngestionBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IngestionBatch completed =
                service.completeBatch(batch, 10, 8, 2, 0);

        assertThat(completed.getTotalRows()).isEqualTo(10);
        assertThat(completed.getImportedRows()).isEqualTo(8);
        assertThat(completed.getSkippedRows()).isEqualTo(2);
        assertThat(completed.getFailedRows()).isZero();
        assertThat(completed.getStatus())
                .isEqualTo(IngestionBatchStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
    }

    @Test
    void completeBatchWithFailuresIsMarkedCompletedWithErrors() {
        IngestionBatch batch = new IngestionBatch();

        when(batchRepository.save(any(IngestionBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IngestionBatch completed =
                service.completeBatch(batch, 10, 7, 1, 2);

        assertThat(completed.getStatus())
                .isEqualTo(IngestionBatchStatus.COMPLETED_WITH_ERRORS);
        assertThat(completed.getFailedRows()).isEqualTo(2);
        assertThat(completed.getCompletedAt()).isNotNull();
    }

    @Test
    void failBatchIsMarkedFailed() {
        IngestionBatch batch = new IngestionBatch();

        when(batchRepository.save(any(IngestionBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IngestionBatch failed =
                service.failBatch(batch, 10, 4, 1, 5);

        assertThat(failed.getTotalRows()).isEqualTo(10);
        assertThat(failed.getImportedRows()).isEqualTo(4);
        assertThat(failed.getSkippedRows()).isEqualTo(1);
        assertThat(failed.getFailedRows()).isEqualTo(5);
        assertThat(failed.getStatus())
                .isEqualTo(IngestionBatchStatus.FAILED);
        assertThat(failed.getCompletedAt()).isNotNull();
    }
}
