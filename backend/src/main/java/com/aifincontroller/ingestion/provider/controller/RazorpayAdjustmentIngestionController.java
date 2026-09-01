package com.aifincontroller.ingestion.provider.controller;

import com.aifincontroller.ingestion.provider.dto.ProviderIngestionResult;
import com.aifincontroller.ingestion.provider.service.RazorpayAdjustmentIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ingestion/razorpay")
public class RazorpayAdjustmentIngestionController {

    private final RazorpayAdjustmentIngestionService ingestionService;

    public RazorpayAdjustmentIngestionController(
            RazorpayAdjustmentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/adjustments")
    public ResponseEntity<ProviderIngestionResult> adjustments(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Integer day,
            @RequestParam(defaultValue = "100") int count,
            @RequestParam(defaultValue = "0") int skip) {

        return ResponseEntity.ok(
                ingestionService.ingestAdjustments(
                        year,
                        month,
                        day,
                        count,
                        skip));
    }
}
