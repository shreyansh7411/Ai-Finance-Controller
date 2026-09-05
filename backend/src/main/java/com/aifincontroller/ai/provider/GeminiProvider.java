package com.aifincontroller.ai.provider;

import com.aifincontroller.ai.dto.AiInvestigationRequest;
import com.aifincontroller.ai.dto.AiInvestigationResponse;
import com.aifincontroller.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini")
public class GeminiProvider implements AiProvider {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final AiProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AiInvestigationPromptBuilder promptBuilder;
    private final AiInvestigationResponseValidator responseValidator;

    @Autowired
    public GeminiProvider(
            AiProperties properties,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            AiInvestigationPromptBuilder promptBuilder,
            AiInvestigationResponseValidator responseValidator) {
        this.properties = properties;
        this.webClient = webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
        this.objectMapper = objectMapper;
        this.promptBuilder = promptBuilder;
        this.responseValidator = responseValidator;
    }

    @Override
    public AiInvestigationResponse investigate(
            AiInvestigationRequest request) {

        if (!properties.isEnabled()) {
            throw new AiProviderException("Gemini provider is disabled");
        }

        if (properties.getApiKey() == null ||
                properties.getApiKey().isBlank()) {
            throw new AiProviderException("Gemini API key is not configured");
        }

        if (request == null) {
            throw new AiProviderException("AI investigation request is null");
        }

        String prompt = promptBuilder.build(request);
        AiProviderException lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return callGemini(prompt, request);
            } catch (AiProviderException exception) {
                lastException = exception;

                if (!isRetryable(exception) || attempt == MAX_ATTEMPTS) {
                    throw exception;
                }

                sleepBeforeRetry(attempt);
            }
        }

        throw new AiProviderException(
                "Gemini investigation failed after retries",
                lastException);
    }

    private AiInvestigationResponse callGemini(
            String prompt,
            AiInvestigationRequest request) {

        try {
            String responseBody = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/" + properties.getModel() + ":generateContent")
                            .queryParam("key", properties.getApiKey())
                            .build())
                    .header("Content-Type", "application/json")
                    .bodyValue(new GeminiRequest(
                            new GeminiContent[] {
                                    new GeminiContent(
                                            "user",
                                            new GeminiPart[] {
                                                    new GeminiPart(prompt)
                                            })
                            }))
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            response -> response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new AiProviderException(
                                                    "Gemini API error: "
                                                            + response.statusCode()
                                                            + " - " + body))))
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(properties.getTimeoutSeconds()));

            if (responseBody == null || responseBody.isBlank()) {
                throw new AiProviderException(
                        "Gemini returned an empty response");
            }

            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode text = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            if (text.isMissingNode() || text.asText().isBlank()) {
                throw new AiProviderException(
                        "Gemini response does not contain generated content");
            }

            String normalizedJson = normalizeJsonResponse(text.asText());

            AiInvestigationResponse response = objectMapper.readValue(
                    normalizedJson,
                    AiInvestigationResponse.class);

            responseValidator.validate(request, response);

            return response;

        } catch (AiProviderException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiProviderException(
                    "Failed to process Gemini investigation response",
                    exception);
        }
    }

    private String normalizeJsonResponse(String text) {

        if (text == null) {
            return null;
        }

        String normalized = text.trim();

        if (normalized.startsWith("```json")) {
            normalized = normalized.substring(7).trim();
        } else if (normalized.startsWith("```")) {
            normalized = normalized.substring(3).trim();
        }

        if (normalized.endsWith("```")) {
            normalized = normalized.substring(
                    0,
                    normalized.length() - 3
            ).trim();
        }

        return normalized;
    }

    private boolean isRetryable(AiProviderException exception) {

        Throwable cause = exception;

        while (cause != null) {

            if (cause instanceof java.net.ConnectException ||
                    cause instanceof java.net.SocketTimeoutException ||
                    cause instanceof java.io.IOException) {
                return true;
            }

            cause = cause.getCause();
        }

        String message = exception.getMessage();

        return message != null &&
                (message.contains("429") ||
                        message.contains("500") ||
                        message.contains("502") ||
                        message.contains("503") ||
                        message.contains("504"));
    }

    private void sleepBeforeRetry(int attempt) {

        try {
            Thread.sleep(RETRY_DELAY_MS * attempt);

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new AiProviderException(
                    "Gemini retry interrupted",
                    exception);
        }
    }

    private record GeminiRequest(
            GeminiContent[] contents) {
    }

    private record GeminiContent(
            String role,
            GeminiPart[] parts) {
    }

    private record GeminiPart(
            String text) {
    }
}
