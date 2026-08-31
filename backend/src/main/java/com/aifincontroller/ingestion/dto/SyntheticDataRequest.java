package com.aifincontroller.ingestion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class SyntheticDataRequest {

    @Min(1)
    @Max(100_000)
    private int count = 1000;

    private Long seed;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }
}
