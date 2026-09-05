package com.aifincontroller.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ai_investigations")
public class AiInvestigationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exception_id", nullable = false, unique = true)
    private Long exceptionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conclusion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "what_happened", columnDefinition = "TEXT")
    private String whatHappened;

    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "financial_impact", columnDefinition = "TEXT")
    private String financialImpact;

    @Column(name = "supporting_evidence", columnDefinition = "TEXT")
    private String supportingEvidence;

    @Column(name = "alternative_explanations", columnDefinition = "TEXT")
    private String alternativeExplanations;

    @Column(name = "missing_evidence", columnDefinition = "TEXT")
    private String missingEvidence;

    @Column(name = "confidence_reasoning", columnDefinition = "TEXT")
    private String confidenceReasoning;

    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(name = "evidence_references", columnDefinition = "TEXT")
    private String evidenceReferences;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "recommended_status", length = 50)
    private String recommendedStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExceptionId() {
        return exceptionId;
    }

    public void setExceptionId(Long exceptionId) {
        this.exceptionId = exceptionId;
    }

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

    public String getSupportingEvidence() {
        return supportingEvidence;
    }

    public void setSupportingEvidence(String supportingEvidence) {
        this.supportingEvidence = supportingEvidence;
    }

    public String getAlternativeExplanations() {
        return alternativeExplanations;
    }

    public void setAlternativeExplanations(String alternativeExplanations) {
        this.alternativeExplanations = alternativeExplanations;
    }

    public String getMissingEvidence() {
        return missingEvidence;
    }

    public void setMissingEvidence(String missingEvidence) {
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

    public String getEvidenceReferences() {
        return evidenceReferences;
    }

    public void setEvidenceReferences(String evidenceReferences) {
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
