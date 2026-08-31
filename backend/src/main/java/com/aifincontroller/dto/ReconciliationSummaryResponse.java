package com.aifincontroller.dto;

import java.util.Map;

public class ReconciliationSummaryResponse {

    private int totalResults;
    private int matched;
    private int exceptions;
    private Map<String, Long> matchTypes;

    public ReconciliationSummaryResponse(
            int totalResults,
            int matched,
            int exceptions,
            Map<String, Long> matchTypes) {

        this.totalResults = totalResults;
        this.matched = matched;
        this.exceptions = exceptions;
        this.matchTypes = matchTypes;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public int getMatched() {
        return matched;
    }

    public int getExceptions() {
        return exceptions;
    }

    public Map<String, Long> getMatchTypes() {
        return matchTypes;
    }
}
