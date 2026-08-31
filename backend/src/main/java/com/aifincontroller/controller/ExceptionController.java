package com.aifincontroller.controller;

import com.aifincontroller.domain.AuditLog;
import com.aifincontroller.domain.ReconciliationException;
import com.aifincontroller.dto.ExceptionResolutionRequest;
import com.aifincontroller.repository.AuditLogRepository;
import com.aifincontroller.repository.ReconciliationExceptionRepository;
import com.aifincontroller.service.ExceptionResolutionService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reconciliation/exceptions")
public class ExceptionController {

    private final ReconciliationExceptionRepository exceptionRepository;
    private final AuditLogRepository auditLogRepository;
    private final ExceptionResolutionService resolutionService;

    public ExceptionController(
            ReconciliationExceptionRepository exceptionRepository,
            AuditLogRepository auditLogRepository,
            ExceptionResolutionService resolutionService) {

        this.exceptionRepository = exceptionRepository;
        this.auditLogRepository = auditLogRepository;
        this.resolutionService = resolutionService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReconciliationException> getException(
            @PathVariable Long id) {

        return exceptionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ReconciliationException> resolveException(
            @PathVariable Long id,
            @RequestBody ExceptionResolutionRequest request) {

        return ResponseEntity.ok(
                resolutionService.resolveException(id, request)
        );
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<AuditLog>> getAuditHistory(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                auditLogRepository.findByEntityTypeAndEntityId(
                        "RECONCILIATION_EXCEPTION",
                        String.valueOf(id)
                )
        );
    }
}
