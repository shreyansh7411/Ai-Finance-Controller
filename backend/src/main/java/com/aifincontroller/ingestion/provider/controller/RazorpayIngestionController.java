package com.aifincontroller.ingestion.provider.controller;

import com.aifincontroller.ingestion.provider.dto.ProviderIngestionResult;
import com.aifincontroller.ingestion.provider.service.RazorpayIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ingestion/razorpay")
public class RazorpayIngestionController {

    private final RazorpayIngestionService ingestionService;

    public RazorpayIngestionController(
            RazorpayIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/payments")
    public ResponseEntity<ProviderIngestionResult> payments(
            @RequestParam(defaultValue = "100") int count,
            @RequestParam(defaultValue = "0") int skip) {

        return ResponseEntity.ok(
                ingestionService.ingestPayments(count, skip));
    }

    @PostMapping("/settlements")
    public ResponseEntity<ProviderIngestionResult> settlements(
            @RequestParam(defaultValue = "100") int count,
            @RequestParam(defaultValue = "0") int skip) {

        return ResponseEntity.ok(
                ingestionService.ingestSettlements(count, skip));
    }

    @PostMapping("/refunds")
    public ResponseEntity<ProviderIngestionResult> refunds(
            @RequestParam(defaultValue = "100") int count,
            @RequestParam(defaultValue = "0") int skip) {

        return ResponseEntity.ok(
                ingestionService.ingestRefunds(count, skip));
    }
}
