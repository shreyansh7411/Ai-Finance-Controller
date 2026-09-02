package com.aifincontroller.service;

import com.aifincontroller.domain.AuditLog;
import com.aifincontroller.domain.ReconciliationException;
import com.aifincontroller.dto.ExceptionResolutionRequest;
import com.aifincontroller.dto.ExceptionStatusUpdateRequest;
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

        ExceptionStatusUpdateRequest statusRequest =
                new ExceptionStatusUpdateRequest();

        statusRequest.setStatus("RESOLVED");
        statusRequest.setActor(request.getActor());
        statusRequest.setDecision(request.getDecision());
        statusRequest.setResolution(request.getResolution());

        return updateStatus(exceptionId, statusRequest);
    }

    @Transactional
    public ReconciliationException updateStatus(
            Long exceptionId,
            ExceptionStatusUpdateRequest request) {

        ReconciliationException exception =
                exceptionRepository.findById(exceptionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Exception not found: " + exceptionId));

        String current = exception.getStatus().toUpperCase();
        String target = request.getStatus().toUpperCase();

        boolean valid =
                ("OPEN".equals(current) &&
                        ("INVESTIGATING".equals(target)
                                || "RESOLVED".equals(target)
                                || "IGNORED".equals(target)))
                || ("INVESTIGATING".equals(current)
                        && "RESOLVED".equals(target));

        if (!valid) {
            throw new IllegalStateException(
                    "Invalid transition: " + current + " -> " + target);
        }

        exception.setStatus(target);

        if ("RESOLVED".equals(target)) {
            exception.setResolution(request.getResolution());
            exception.setResolvedAt(Instant.now());
        }

        ReconciliationException saved =
                exceptionRepository.save(exception);

        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType("RECONCILIATION_EXCEPTION");
        auditLog.setEntityId(String.valueOf(exceptionId));
        auditLog.setAction("STATUS_CHANGE");
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
