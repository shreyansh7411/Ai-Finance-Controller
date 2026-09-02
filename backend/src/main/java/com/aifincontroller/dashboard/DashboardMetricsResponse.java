package com.aifincontroller.dashboard;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardMetricsResponse(
        long totalRecords,
        long matchedRecords,
        long exceptionRecords,
        long aiResolvedRecords,
        long unresolvedRecords,
        BigDecimal matchRate,
        BigDecimal resolutionRate,
        Map<String, Long> exceptionBreakdown) {
}
