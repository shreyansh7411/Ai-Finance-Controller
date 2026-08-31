package com.aifincontroller.ingestion.controller;

import com.aifincontroller.ingestion.service.CsvIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ingestion")
public class CsvIngestionController {

    private final CsvIngestionService ingestionService;

    public CsvIngestionController(CsvIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/payments")
    public ResponseEntity<String> payments(
            @RequestParam("file") MultipartFile file,
            @RequestParam String batchId) {

        int imported = ingestionService.ingestPayments(file, batchId);

        return ResponseEntity.ok(
                "Payments imported: " + imported);
    }

    @PostMapping("/settlements")
    public ResponseEntity<String> settlements(
            @RequestParam("file") MultipartFile file) {

        int imported = ingestionService.ingestSettlements(file);

        return ResponseEntity.ok(
                "Settlements imported: " + imported);
    }

    @PostMapping("/refunds")
    public ResponseEntity<String> refunds(
            @RequestParam("file") MultipartFile file) {

        int imported = ingestionService.ingestRefunds(file);

        return ResponseEntity.ok(
                "Refunds imported: " + imported);
    }

    @PostMapping("/adjustments")
    public ResponseEntity<String> adjustments(
            @RequestParam("file") MultipartFile file) {

        int imported = ingestionService.ingestAdjustments(file);

        return ResponseEntity.ok(
                "Adjustments imported: " + imported);
    }
}
