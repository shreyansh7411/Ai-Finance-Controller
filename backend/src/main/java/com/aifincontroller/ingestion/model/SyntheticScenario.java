package com.aifincontroller.ingestion.model;

public enum SyntheticScenario {
    EXACT_MATCH,
    FEE_DIFFERENCE,
    TAX_DIFFERENCE,
    REFUND,
    ADJUSTMENT,
    TIMING_DIFFERENCE,
    MISSING_SETTLEMENT,
    DUPLICATE,
    UNEXPLAINED_MISMATCH
}
