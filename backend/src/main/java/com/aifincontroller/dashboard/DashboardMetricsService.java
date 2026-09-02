package com.aifincontroller.dashboard;

import com.aifincontroller.repository.DecisionRecordRepository;
import com.aifincontroller.repository.ReconciliationExceptionRepository;
import com.aifincontroller.repository.ReconciliationResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardMetricsService {

    private final ReconciliationResultRepository resultRepository;
    private final ReconciliationExceptionRepository exceptionRepository;
    private final DecisionRecordRepository decisionRepository;

    public DashboardMetricsService(
            ReconciliationResultRepository resultRepository,
            ReconciliationExceptionRepository exceptionRepository,
            DecisionRecordRepository decisionRepository) {

        this.resultRepository = resultRepository;
        this.exceptionRepository = exceptionRepository;
        this.decisionRepository = decisionRepository;
    }

    @Transactional(readOnly = true)
    public DashboardMetricsResponse getMetrics() {

        long totalRecords = resultRepository.count();

        long matchedRecords =
                resultRepository.countByStatus("MATCHED");

        long exceptionRecords =
                resultRepository.countByStatus("EXCEPTION");

        long aiResolvedRecords =
                decisionRepository.countByOutcome("AUTO_RESOLVE");

        long unresolvedRecords =
                exceptionRepository.countByStatusNot("RESOLVED");

        BigDecimal matchRate =
                percentage(matchedRecords, totalRecords);

        BigDecimal resolutionRate =
                percentage(aiResolvedRecords, exceptionRecords);

        Map<String, Long> exceptionBreakdown =
                new LinkedHashMap<>();

        List<Object[]> groupedExceptions =
                exceptionRepository.countByCategory();

        for (Object[] row : groupedExceptions) {
            String category = (String) row[0];
            Long count = (Long) row[1];

            exceptionBreakdown.put(category, count);
        }

        return new DashboardMetricsResponse(
                totalRecords,
                matchedRecords,
                exceptionRecords,
                aiResolvedRecords,
                unresolvedRecords,
                matchRate,
                resolutionRate,
                exceptionBreakdown
        );
    }

    private BigDecimal percentage(long numerator, long denominator) {

        if (denominator == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(denominator),
                        2,
                        RoundingMode.HALF_UP
                );
    }
}
