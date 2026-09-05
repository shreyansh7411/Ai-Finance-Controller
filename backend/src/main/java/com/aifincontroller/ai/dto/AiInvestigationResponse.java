package com.aifincontroller.ai.dto;

import java.math.BigDecimal;
import java.util.List;

public class AiInvestigationResponse {

    private String conclusion;
    private String explanation;

    private String whatHappened;
    private String rootCause;
    private String financialImpact;

    private List<String> supportingEvidence;
    private List<String> alternativeExplanations;
    private List<String> missingEvidence;

    private String confidenceReasoning;
    private String recommendedAction;

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

    public String getWhatHappened() {
        return whatHappened;
    }

    public void setWhatHappened(String whatHappened) {
        this.whatHappened = whatHappened;
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getFinancialImpact() {
        return financialImpact;
    }

    public void setFinancialImpact(String financialImpact) {
        this.financialImpact = financialImpact;
    }

    public List<String> getSupportingEvidence() {
        return supportingEvidence;
    }

    public void setSupportingEvidence(List<String> supportingEvidence) {
        this.supportingEvidence = supportingEvidence;
    }

    public List<String> getAlternativeExplanations() {
        return alternativeExplanations;
    }

    public void setAlternativeExplanations(
            List<String> alternativeExplanations) {
        this.alternativeExplanations = alternativeExplanations;
    }

    public List<String> getMissingEvidence() {
        return missingEvidence;
    }

    public void setMissingEvidence(List<String> missingEvidence) {
        this.missingEvidence = missingEvidence;
    }

    public String getConfidenceReasoning() {
        return confidenceReasoning;
    }

    public void setConfidenceReasoning(String confidenceReasoning) {
        this.confidenceReasoning = confidenceReasoning;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
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
