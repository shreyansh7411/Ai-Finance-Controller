package com.aifincontroller.controller;

import com.aifincontroller.dto.ReconciliationExceptionResponse;
import java.math.BigDecimal;
import com.aifincontroller.domain.ReconciliationResult;
import com.aifincontroller.dto.ReconciliationSummaryResponse;
import com.aifincontroller.repository.ReconciliationResultRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reconciliation")
public class ReconciliationResultController {

    private final ReconciliationResultRepository reconciliationResultRepository;

    public ReconciliationResultController(
            ReconciliationResultRepository reconciliationResultRepository) {

        this.reconciliationResultRepository =
                reconciliationResultRepository;
    }

    @GetMapping("/results")
    public ResponseEntity<List<ReconciliationResult>> getResults(
            @RequestParam String batchId) {

        return ResponseEntity.ok(
                reconciliationResultRepository.findByBatchId(batchId)
        );
    }

    @GetMapping("/exceptions")
public ResponseEntity<List<ReconciliationExceptionResponse>> getExceptions(
        @RequestParam String batchId,
        @RequestParam(required = false) String matchType,
        @RequestParam(required = false) String status) {

    List<ReconciliationResult> results =
            reconciliationResultRepository.findByBatchId(batchId);

    List<ReconciliationExceptionResponse> exceptions =
            results.stream()
                    .filter(result -> "EXCEPTION".equals(result.getStatus()))
                    .filter(result ->
                            matchType == null ||
                            matchType.equalsIgnoreCase(result.getMatchType()))
                    .filter(result ->
                            status == null ||
                            status.equalsIgnoreCase(result.getStatus()))
                    .map(result -> new ReconciliationExceptionResponse(
                            result.getId(),
                            result.getBatchId(),
                            result.getPaymentReference(),
                            result.getMatchType(),
                            result.getStatus(),
                            result.getExpectedAmount(),
                            result.getActualAmount(),
                            result.getDifference(),
                            result.getConfidenceScore()
                    ))
                    .toList();

    return ResponseEntity.ok(exceptions);
}

    @GetMapping("/summary")
    public ResponseEntity<ReconciliationSummaryResponse> getSummary(
            @RequestParam String batchId) {

        List<ReconciliationResult> results =
                reconciliationResultRepository.findByBatchId(batchId);

        int totalResults = results.size();

        int matched = (int) results.stream()
                .filter(result -> "MATCHED".equals(result.getStatus()))
                .count();

        int exceptions = (int) results.stream()
                .filter(result -> "EXCEPTION".equals(result.getStatus()))
                .count();

        Map<String, Long> matchTypes = results.stream()
                .map(ReconciliationResult::getMatchType)
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));

        return ResponseEntity.ok(
                new ReconciliationSummaryResponse(
                        totalResults,
                        matched,
                        exceptions,
                        matchTypes
                )
        );
    }
}
