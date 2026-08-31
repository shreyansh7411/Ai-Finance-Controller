package com.aifincontroller.controller;

import com.aifincontroller.dto.SyntheticGenerationRequest;
import com.aifincontroller.dto.SyntheticGenerationResponse;
import com.aifincontroller.service.SyntheticDataGeneratorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/synthetic")
public class SyntheticDataController {

    private final SyntheticDataGeneratorService generatorService;

    public SyntheticDataController(
            SyntheticDataGeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    @PostMapping("/generate")
    public ResponseEntity<SyntheticGenerationResponse> generate(
            @Valid @RequestBody SyntheticGenerationRequest request) {

        return ResponseEntity.ok(
                generatorService.generate(request.getCount()));
    }
}