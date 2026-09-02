package com.aifincontroller.ai.dto;

import java.math.BigDecimal;
import java.util.List;

public class AiInvestigationResponse {

    private String conclusion;
    private String explanation;
    private List<String> evidenceReferences;
    private BigDecimal confidence;
    private String recommendedStatus;

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<String> getEvidenceReferences() {
        return evidenceReferences;
    }

    public void setEvidenceReferences(List<String> evidenceReferences) {
        this.evidenceReferences = evidenceReferences;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getRecommendedStatus() {
        return recommendedStatus;
    }

    public void setRecommendedStatus(String recommendedStatus) {
        this.recommendedStatus = recommendedStatus;
    }
}
