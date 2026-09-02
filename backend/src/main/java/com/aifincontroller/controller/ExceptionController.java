package com.aifincontroller.controller;

import com.aifincontroller.ai.dto.AiInvestigationResponse;
import com.aifincontroller.service.AiInvestigationService;
import com.aifincontroller.domain.AuditLog;
import com.aifincontroller.domain.ReconciliationException;
import com.aifincontroller.dto.ExceptionResolutionRequest;
import com.aifincontroller.dto.ExceptionStatusUpdateRequest;
import com.aifincontroller.repository.AuditLogRepository;
import com.aifincontroller.repository.ReconciliationExceptionRepository;
import com.aifincontroller.service.ExceptionResolutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reconciliation/exceptions")
public class ExceptionController {

    private final ReconciliationExceptionRepository exceptionRepository;
    private final AuditLogRepository auditLogRepository;
    private final ExceptionResolutionService resolutionService;
    private final AiInvestigationService aiInvestigationService;

    public ExceptionController(
            ReconciliationExceptionRepository exceptionRepository,
            AuditLogRepository auditLogRepository,
            ExceptionResolutionService resolutionService,
            AiInvestigationService aiInvestigationService) {

        this.exceptionRepository = exceptionRepository;
        this.auditLogRepository = auditLogRepository;
        this.resolutionService = resolutionService;
        this.aiInvestigationService = aiInvestigationService;
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

    @PutMapping("/{id}/status")
    public ResponseEntity<ReconciliationException> updateStatus(
            @PathVariable Long id,
            @RequestBody ExceptionStatusUpdateRequest request) {

        return ResponseEntity.ok(
                resolutionService.updateStatus(id, request)
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

    @PostMapping("/{id}/investigate")
    public ResponseEntity<AiInvestigationResponse> investigateException(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                aiInvestigationService.investigate(id)
        );
    }
}
