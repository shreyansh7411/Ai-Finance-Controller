package com.aifincontroller.service;

import com.aifincontroller.ai.domain.AiInvestigationRecord;
import com.aifincontroller.ai.dto.AiInvestigationRequest;
import com.aifincontroller.ai.dto.AiInvestigationResponse;
import com.aifincontroller.ai.provider.AiProvider;
import com.aifincontroller.ai.repository.AiInvestigationRecordRepository;
import com.aifincontroller.controller.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AiInvestigationService {

    private final AiInvestigationEvidenceService evidenceService;
    private final AiProvider aiProvider;
    private final AiInvestigationRecordRepository investigationRepository;
    private final ObjectMapper objectMapper;

    public AiInvestigationService(
            AiInvestigationEvidenceService evidenceService,
            AiProvider aiProvider,
            AiInvestigationRecordRepository investigationRepository,
            ObjectMapper objectMapper) {

        this.evidenceService = evidenceService;
        this.aiProvider = aiProvider;
        this.investigationRepository = investigationRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiInvestigationResponse investigate(Long exceptionId) {

        if (exceptionId == null) {
            throw new IllegalArgumentException(
                    "Exception ID is required"
            );
        }

        /*
         * Investigation is persisted per exception.
         * If one already exists, return the stored result rather than
         * making another AI provider call.
         */
        AiInvestigationRecord existing =
                investigationRepository.findByExceptionId(exceptionId)
                        .orElse(null);

        if (existing != null) {
            return toResponse(existing);
        }

        AiInvestigationRequest request =
                evidenceService.buildRequest(exceptionId);

        AiInvestigationResponse response =
                aiProvider.investigate(request);

        AiInvestigationRecord record =
                new AiInvestigationRecord();

        record.setExceptionId(exceptionId);

        record.setConclusion(
                response.getConclusion());

        record.setExplanation(
                response.getExplanation());

        record.setWhatHappened(
                response.getWhatHappened());

        record.setRootCause(
                response.getRootCause());

        record.setFinancialImpact(
                response.getFinancialImpact());

        record.setConfidenceReasoning(
                response.getConfidenceReasoning());

        record.setRecommendedAction(
                response.getRecommendedAction());

        record.setConfidence(
                response.getConfidence());

        record.setRecommendedStatus(
                response.getRecommendedStatus());

        try {
            record.setEvidenceReferences(
                    serialize(
                            response.getEvidenceReferences()));

            record.setSupportingEvidence(
                    serialize(
                            response.getSupportingEvidence()));

            record.setAlternativeExplanations(
                    serialize(
                            response.getAlternativeExplanations()));

            record.setMissingEvidence(
                    serialize(
                            response.getMissingEvidence()));

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to serialize investigation details",
                    exception
            );
        }

        investigationRepository.save(record);

        return response;
    }

    @Transactional(readOnly = true)
    public AiInvestigationResponse getInvestigation(
            Long exceptionId) {

        if (exceptionId == null) {
            throw new IllegalArgumentException(
                    "Exception ID is required"
            );
        }

        AiInvestigationRecord record =
                investigationRepository.findByExceptionId(exceptionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Investigation not found for exception: "
                                                + exceptionId
                                )
                        );

        return toResponse(record);
    }

    private AiInvestigationResponse toResponse(
            AiInvestigationRecord record) {

        AiInvestigationResponse response =
                new AiInvestigationResponse();

        response.setConclusion(
                record.getConclusion());

        response.setExplanation(
                record.getExplanation());

        response.setWhatHappened(
                record.getWhatHappened());

        response.setRootCause(
                record.getRootCause());

        response.setFinancialImpact(
                record.getFinancialImpact());

        response.setConfidenceReasoning(
                record.getConfidenceReasoning());

        response.setRecommendedAction(
                record.getRecommendedAction());

        response.setConfidence(
                record.getConfidence());

        response.setRecommendedStatus(
                record.getRecommendedStatus());

        try {
            response.setEvidenceReferences(
                    deserializeList(
                            record.getEvidenceReferences()));

            response.setSupportingEvidence(
                    deserializeList(
                            record.getSupportingEvidence()));

            response.setAlternativeExplanations(
                    deserializeList(
                            record.getAlternativeExplanations()));

            response.setMissingEvidence(
                    deserializeList(
                            record.getMissingEvidence()));

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to deserialize investigation details",
                    exception
            );
        }

        return response;
    }

    private String serialize(
            List<String> values)
            throws JsonProcessingException {

        return objectMapper.writeValueAsString(
                values == null
                        ? List.of()
                        : values
        );
    }

    private List<String> deserializeList(
            String json)
            throws JsonProcessingException {

        if (json == null || json.isBlank()) {
            return List.of();
        }

        return objectMapper.readValue(
                json,
                objectMapper.getTypeFactory()
                        .constructCollectionType(
                                List.class,
                                String.class
                        )
        );
    }
}
