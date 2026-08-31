package com.aifincontroller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class SyntheticGenerationRequest {

    @Min(1)
    @Max(10000)
    private int count = 1000;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
