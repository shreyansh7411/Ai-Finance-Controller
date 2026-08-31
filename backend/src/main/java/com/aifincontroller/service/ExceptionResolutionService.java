package com.aifincontroller.service;

import com.aifincontroller.domain.AuditLog;
import com.aifincontroller.domain.ReconciliationException;
import com.aifincontroller.dto.ExceptionResolutionRequest;
import com.aifincontroller.repository.AuditLogRepository;
import com.aifincontroller.repository.ReconciliationExceptionRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExceptionResolutionService {

    private final ReconciliationExceptionRepository exceptionRepository;
    private final AuditLogRepository auditLogRepository;

    public ExceptionResolutionService(
            ReconciliationExceptionRepository exceptionRepository,
            AuditLogRepository auditLogRepository) {

        this.exceptionRepository = exceptionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public ReconciliationException resolveException(
            Long exceptionId,
            ExceptionResolutionRequest request) {

        ReconciliationException exception =
                exceptionRepository.findById(exceptionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Exception not found: " + exceptionId));

        if ("RESOLVED".equalsIgnoreCase(exception.getStatus())) {
            return exception;
        }

        exception.setStatus("RESOLVED");
        exception.setResolution(request.getResolution());
        exception.setResolvedAt(Instant.now());

        ReconciliationException saved =
                exceptionRepository.save(exception);

        AuditLog auditLog = new AuditLog();

        auditLog.setEntityType("RECONCILIATION_EXCEPTION");
        auditLog.setEntityId(String.valueOf(exceptionId));
        auditLog.setAction("RESOLVE_EXCEPTION");
        auditLog.setActor(
                request.getActor() == null ||
                request.getActor().isBlank()
                        ? "SYSTEM"
                        : request.getActor()
        );
        auditLog.setDecision(request.getDecision());

        auditLogRepository.save(auditLog);

        return saved;
    }
}
