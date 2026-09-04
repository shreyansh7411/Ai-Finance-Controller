package com.aifincontroller.service;

import com.aifincontroller.ai.domain.AiInvestigationRecord;
import com.aifincontroller.ai.dto.AiInvestigationRequest;
import com.aifincontroller.ai.dto.AiInvestigationResponse;
import com.aifincontroller.controller.ResourceNotFoundException;
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
         * making another Gemini call.
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
        record.setConclusion(response.getConclusion());
        record.setExplanation(response.getExplanation());
        record.setConfidence(response.getConfidence());
        record.setRecommendedStatus(
                response.getRecommendedStatus()
        );

        try {
            record.setEvidenceReferences(
                    objectMapper.writeValueAsString(
                            response.getEvidenceReferences()
                    )
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to serialize investigation evidence",
                    exception
            );
        }

        investigationRepository.save(record);

        return response;
    }

    @Transactional(readOnly = true)
    public AiInvestigationResponse getInvestigation(Long exceptionId) {

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

        response.setConclusion(record.getConclusion());
        response.setExplanation(record.getExplanation());
        response.setConfidence(record.getConfidence());
        response.setRecommendedStatus(
                record.getRecommendedStatus()
        );

        try {
            List<String> evidenceReferences =
                    record.getEvidenceReferences() == null
                            ? List.of()
                            : objectMapper.readValue(
                                    record.getEvidenceReferences(),
                                    objectMapper.getTypeFactory()
                                            .constructCollectionType(
                                                    List.class,
                                                    String.class
                                            )
                            );

            response.setEvidenceReferences(evidenceReferences);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to deserialize investigation evidence",
                    exception
            );
        }

        return response;
    }
}
