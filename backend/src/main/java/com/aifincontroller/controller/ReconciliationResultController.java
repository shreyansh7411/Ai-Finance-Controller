package com.aifincontroller.controller;

import com.aifincontroller.domain.ReconciliationException;
import com.aifincontroller.domain.ReconciliationResult;
import com.aifincontroller.dto.ReconciliationExceptionResponse;
import com.aifincontroller.dto.ReconciliationSummaryResponse;
import com.aifincontroller.repository.ReconciliationExceptionRepository;
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
    private final ReconciliationExceptionRepository exceptionRepository;

    public ReconciliationResultController(
            ReconciliationResultRepository reconciliationResultRepository,
            ReconciliationExceptionRepository exceptionRepository) {

        this.reconciliationResultRepository = reconciliationResultRepository;
        this.exceptionRepository = exceptionRepository;
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
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String matchType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity) {

        List<ReconciliationResult> results =
                batchId == null
                        ? reconciliationResultRepository.findAll()
                        : reconciliationResultRepository.findByBatchId(batchId);

        List<ReconciliationResult> exceptionResults =
                results.stream()
                        .filter(result ->
                                "EXCEPTION".equalsIgnoreCase(result.getStatus()))
                        .filter(result ->
                                matchType == null ||
                                matchType.equalsIgnoreCase(result.getMatchType()))
                        .toList();

        List<Long> resultIds =
                exceptionResults.stream()
                        .map(ReconciliationResult::getId)
                        .toList();

        Map<Long, ReconciliationException> exceptionsByResultId =
                resultIds.isEmpty()
                        ? Map.of()
                        : exceptionRepository
                                .findByReconciliationResultIdIn(resultIds)
                                .stream()
                                .collect(Collectors.toMap(
                                        ReconciliationException::getReconciliationResultId,
                                        Function.identity(),
                                        (first, second) -> first
                                ));

        List<ReconciliationExceptionResponse> exceptions =
                exceptionResults.stream()
                        .map(result ->
                                exceptionsByResultId.get(result.getId()) == null
                                        ? null
                                        : new ReconciliationExceptionResponse(
                                                exceptionsByResultId
                                                        .get(result.getId())
                                                        .getId(),
                                                result.getBatchId(),
                                                result.getPaymentReference(),
                                                result.getMatchType(),
                                                exceptionsByResultId
                                                        .get(result.getId())
                                                        .getCategory(),
                                                exceptionsByResultId
                                                        .get(result.getId())
                                                        .getSeverity(),
                                                exceptionsByResultId
                                                        .get(result.getId())
                                                        .getStatus(),
                                                result.getExpectedAmount(),
                                                result.getActualAmount(),
                                                result.getDifference(),
                                                result.getConfidenceScore()
                                        )
                        )
                        .filter(java.util.Objects::nonNull)
                        .filter(exception ->
                                status == null ||
                                status.equalsIgnoreCase(exception.getStatus()))
                        .filter(exception ->
                                category == null ||
                                category.equalsIgnoreCase(exception.getCategory()))
                        .filter(exception ->
                                severity == null ||
                                severity.equalsIgnoreCase(exception.getSeverity()))
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
