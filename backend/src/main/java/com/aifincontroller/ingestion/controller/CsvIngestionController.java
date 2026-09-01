package com.aifincontroller.ingestion.controller;

import com.aifincontroller.ingestion.dto.IngestionResult;
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
    public ResponseEntity<IngestionResult> payments(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(
                ingestionService.ingestPayments(file));
    }

    @PostMapping("/settlements")
    public ResponseEntity<IngestionResult> settlements(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(
                ingestionService.ingestSettlements(file));
    }

    @PostMapping("/refunds")
    public ResponseEntity<IngestionResult> refunds(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(
                ingestionService.ingestRefunds(file));
    }

    @PostMapping("/adjustments")
    public ResponseEntity<IngestionResult> adjustments(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(
                ingestionService.ingestAdjustments(file));
    }
}
