package com.aifincontroller.controller;

import com.aifincontroller.domain.ReconciliationResult;
import com.aifincontroller.service.ReconciliationService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(
            ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/run")
    public ResponseEntity<List<ReconciliationResult>> runReconciliation(
            @RequestParam String batchId) {

        return ResponseEntity.ok(
                reconciliationService.reconcileBatch(batchId));
    }
}