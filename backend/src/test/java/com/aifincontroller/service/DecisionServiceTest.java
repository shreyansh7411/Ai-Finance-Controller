package com.aifincontroller.service;

import com.aifincontroller.ai.dto.AiInvestigationResponse;
import com.aifincontroller.domain.AuditLog;
import com.aifincontroller.domain.DecisionRecord;
import com.aifincontroller.domain.ReconciliationException;
import com.aifincontroller.repository.AuditLogRepository;
import com.aifincontroller.repository.DecisionRecordRepository;
import com.aifincontroller.repository.ReconciliationExceptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class DecisionServiceTest {

    private DecisionRecordRepository decisionRepository;
    private ReconciliationExceptionRepository exceptionRepository;
    private AuditLogRepository auditLogRepository;
    private DecisionEngine decisionEngine;

    private DecisionService decisionService;

    @BeforeEach
    void setUp() {
        decisionRepository = mock(DecisionRecordRepository.class);
        exceptionRepository = mock(ReconciliationExceptionRepository.class);
        auditLogRepository = mock(AuditLogRepository.class);
        decisionEngine = mock(DecisionEngine.class);

        decisionService = new DecisionService(
                decisionRepository,
                exceptionRepository,
                auditLogRepository,
                decisionEngine,
                new ObjectMapper()
        );
    }

    @Test
    void shouldReturnExistingDecisionWithoutReprocessing() {

        Long exceptionId = 1L;

        DecisionRecord existing = new DecisionRecord();
        existing.setExceptionId(exceptionId);
        existing.setOutcome("AUTO_RESOLVE");

        when(decisionRepository.findByExceptionId(exceptionId))
                .thenReturn(Optional.of(existing));

        DecisionRecord result =
                decisionService.processDecision(
                        exceptionId,
                        new AiInvestigationResponse()
                );

        assertSame(existing, result);

        verify(decisionRepository)
                .findByExceptionId(exceptionId);

        verifyNoInteractions(
                exceptionRepository,
                auditLogRepository,
                decisionEngine
        );
    }

    @Test
    void shouldRejectNullExceptionId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> decisionService.processDecision(
                        null,
                        new AiInvestigationResponse()
                )
        );

        verifyNoInteractions(
                decisionRepository,
                exceptionRepository,
                auditLogRepository,
                decisionEngine
        );
    }

    @Test
    void shouldRejectMissingException() {

        Long exceptionId = 1L;

        when(decisionRepository.findByExceptionId(exceptionId))
                .thenReturn(Optional.empty());

        when(exceptionRepository.findById(exceptionId))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> decisionService.processDecision(
                        exceptionId,
                        new AiInvestigationResponse()
                )
        );

        verifyNoInteractions(
                auditLogRepository,
                decisionEngine
        );
    }

    @Test
    void shouldAutoResolveAndCreateAuditLog() {

        Long exceptionId = 10L;

        ReconciliationException exception =
                new ReconciliationException();

        exception.setId(exceptionId);

        when(decisionRepository.findByExceptionId(exceptionId))
                .thenReturn(Optional.empty());

        when(exceptionRepository.findById(exceptionId))
                .thenReturn(Optional.of(exception));

        DecisionResult decision =
                new DecisionResult();

        decision.setOutcome(
                DecisionOutcome.AUTO_RESOLVE
        );

        decision.setConfidence(
                new BigDecimal("0.95")
        );

        decision.setReason(
                "High-confidence AI investigation recommended resolution."
        );

        when(decisionEngine.decideWithReason(any()))
                .thenReturn(decision);

        when(decisionRepository.save(any(DecisionRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AiInvestigationResponse investigation =
                new AiInvestigationResponse();

        investigation.setEvidenceReferences(
                List.of(
                        "payment:PAY-1001",
                        "settlement:SET-2001"
                )
        );

        DecisionRecord result =
                decisionService.processDecision(
                        exceptionId,
                        investigation
                );

        assertEquals(
                DecisionOutcome.AUTO_RESOLVE.name(),
                result.getOutcome()
        );

        assertEquals(
                DecisionOutcome.AUTO_RESOLVE.name(),
                result.getStatus()
        );

        assertEquals(
                "[\"payment:PAY-1001\",\"settlement:SET-2001\"]",
                result.getEvidenceReferences()
        );

        assertEquals(
                "RESOLVED",
                exception.getStatus()
        );

        assertNotNull(
                exception.getResolvedAt()
        );

        verify(exceptionRepository)
                .save(exception);

        verify(auditLogRepository)
                .save(argThat(audit ->
                        "DECISION".equals(audit.getEntityType())
                                && String.valueOf(exceptionId)
                                .equals(audit.getEntityId())
                                && "DECISION_PROCESSED"
                                .equals(audit.getAction())
                                && "SYSTEM".equals(audit.getActor())
                                && ("AUTO_RESOLVE: "
                                + decision.getReason())
                                .equals(audit.getDecision())
                                && ("exception:"
                                + exceptionId
                                + ";investigation:"
                                + result.getEvidenceReferences())
                                .equals(audit.getEvidenceReference())
                ));
    }

    @Test
    void shouldCreateAuditLogForHumanReview() {

        Long exceptionId = 20L;

        ReconciliationException exception =
                new ReconciliationException();

        exception.setId(exceptionId);

        when(decisionRepository.findByExceptionId(exceptionId))
                .thenReturn(Optional.empty());

        when(exceptionRepository.findById(exceptionId))
                .thenReturn(Optional.of(exception));

        DecisionResult decision =
                new DecisionResult();

        decision.setOutcome(
                DecisionOutcome.HUMAN_REVIEW
        );

        decision.setConfidence(
                new BigDecimal("0.40")
        );

        decision.setReason(
                "AI confidence is below the review threshold; exception must be escalated to human review."
        );

        when(decisionEngine.decideWithReason(any()))
                .thenReturn(decision);

        when(decisionRepository.save(any(DecisionRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        decisionService.processDecision(
                exceptionId,
                new AiInvestigationResponse()
        );

        verify(auditLogRepository)
                .save(argThat(audit ->
                        "DECISION".equals(audit.getEntityType())
                                && String.valueOf(exceptionId)
                                .equals(audit.getEntityId())
                                && "DECISION_PROCESSED"
                                .equals(audit.getAction())
                                && "SYSTEM".equals(audit.getActor())
                ));

        verify(exceptionRepository, never())
                .save(any());
    }

    @Test
    void shouldNotCreateDuplicateAuditLogWhenDecisionAlreadyExists() {

        Long exceptionId = 30L;

        DecisionRecord existing = new DecisionRecord();
        existing.setExceptionId(exceptionId);
        existing.setOutcome("HUMAN_REVIEW");

        when(decisionRepository.findByExceptionId(exceptionId))
                .thenReturn(Optional.of(existing));

        decisionService.processDecision(
                exceptionId,
                new AiInvestigationResponse()
        );

        verifyNoInteractions(auditLogRepository);
    }
}
