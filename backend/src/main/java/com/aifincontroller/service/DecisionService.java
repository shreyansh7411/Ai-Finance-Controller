package com.aifincontroller.service;

import com.aifincontroller.ai.dto.AiInvestigationResponse;
import com.aifincontroller.domain.AuditLog;
import com.aifincontroller.domain.DecisionRecord;
import com.aifincontroller.domain.ReconciliationException;
import com.aifincontroller.repository.AuditLogRepository;
import com.aifincontroller.repository.DecisionRecordRepository;
import com.aifincontroller.repository.ReconciliationExceptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class DecisionService {

    private final DecisionRecordRepository decisionRepository;
    private final ReconciliationExceptionRepository exceptionRepository;
    private final AuditLogRepository auditLogRepository;
    private final DecisionEngine decisionEngine;

    public DecisionService(
            DecisionRecordRepository decisionRepository,
            ReconciliationExceptionRepository exceptionRepository,
            AuditLogRepository auditLogRepository,
            DecisionEngine decisionEngine) {

        this.decisionRepository = decisionRepository;
        this.exceptionRepository = exceptionRepository;
        this.auditLogRepository = auditLogRepository;
        this.decisionEngine = decisionEngine;
    }

    @Transactional
    public DecisionRecord processDecision(
            Long exceptionId,
            AiInvestigationResponse investigation) {

        if (exceptionId == null) {
            throw new IllegalArgumentException(
                    "Exception ID is required"
            );
        }

        DecisionRecord existing =
                decisionRepository.findByExceptionId(exceptionId)
                        .orElse(null);

        if (existing != null) {
            return existing;
        }

        ReconciliationException exception =
                exceptionRepository.findById(exceptionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Exception not found: "
                                                + exceptionId
                                ));

        DecisionResult decision =
                decisionEngine.decideWithReason(
                        investigation
                );

        DecisionRecord record =
                new DecisionRecord();

        record.setExceptionId(exceptionId);
        record.setOutcome(
                decision.getOutcome().name()
        );
        record.setConfidence(
                decision.getConfidence()
        );
        record.setReason(
                decision.getReason()
        );
        record.setStatus(
                decision.getOutcome().name()
        );

        DecisionRecord saved =
                decisionRepository.save(record);

        if (DecisionOutcome.AUTO_RESOLVE
                .name()
                .equals(decision.getOutcome().name())) {

            exception.setStatus("RESOLVED");
            exception.setResolution(
                    decision.getReason()
            );
            exception.setResolvedAt(
                    Instant.now()
            );

            exceptionRepository.save(exception);
        }

        AuditLog auditLog = new AuditLog();

        auditLog.setEntityType(
                "DECISION"
        );

        auditLog.setEntityId(
                String.valueOf(exceptionId)
        );

        auditLog.setAction(
                "DECISION_PROCESSED"
        );

        auditLog.setActor(
                "SYSTEM"
        );

        auditLog.setDecision(
                decision.getOutcome().name()
                        + ": "
                        + decision.getReason()
        );

        auditLog.setEvidenceReference(
                "exception:"
                        + exceptionId
        );

        auditLogRepository.save(auditLog);

        return saved;
    }
}
