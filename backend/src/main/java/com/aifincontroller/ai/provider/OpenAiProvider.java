package com.aifincontroller.ai.provider;

import com.aifincontroller.ai.dto.AiInvestigationRequest;
import com.aifincontroller.ai.dto.AiInvestigationResponse;
import com.aifincontroller.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class OpenAiProvider implements AiProvider {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final AiProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AiInvestigationPromptBuilder promptBuilder;
    private final AiInvestigationResponseValidator responseValidator;

    @Autowired
    public OpenAiProvider(
            AiProperties properties,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            AiInvestigationPromptBuilder promptBuilder,
            AiInvestigationResponseValidator responseValidator) {

        this(
                properties,
                webClientBuilder
                        .baseUrl(properties.getBaseUrl())
                        .build(),
                objectMapper,
                promptBuilder,
                responseValidator
        );
    }

    OpenAiProvider(
            AiProperties properties,
            WebClient webClient,
            ObjectMapper objectMapper,
            AiInvestigationPromptBuilder promptBuilder,
            AiInvestigationResponseValidator responseValidator) {

        this.properties = properties;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.promptBuilder = promptBuilder;
        this.responseValidator = responseValidator;
    }

    @Override
    public AiInvestigationResponse investigate(
            AiInvestigationRequest request) {

        if (!properties.isEnabled()) {
            throw new AiProviderException(
                    "AI provider is disabled"
            );
        }

        if (properties.getApiKey() == null ||
                properties.getApiKey().isBlank()) {

            throw new AiProviderException(
                    "OpenAI API key is not configured"
            );
        }

        if (request == null) {
            throw new AiProviderException(
                    "AI investigation request is null"
            );
        }

        String prompt = promptBuilder.build(request);

        AiProviderException lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return callOpenAi(prompt, request);
            } catch (AiProviderException e) {
                lastException = e;

                if (!isRetryable(e) || attempt == MAX_ATTEMPTS) {
                    throw e;
                }

                sleepBeforeRetry(attempt);
            }
        }

        throw new AiProviderException(
                "OpenAI investigation failed after retries",
                lastException
        );
    }

    private AiInvestigationResponse callOpenAi(
            String prompt,
            AiInvestigationRequest request) {

        try {
            String responseBody = webClient.post()
                    .uri("/chat/completions")
                    .header(
                            "Authorization",
                            "Bearer " + properties.getApiKey()
                    )
                    .header(
                            "Content-Type",
                            "application/json"
                    )
                    .bodyValue(
                            new OpenAiRequest(
                                    properties.getModel(),
                                    prompt
                            )
                    )
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            response -> response.bodyToMono(String.class)
                                    .flatMap(body ->
                                            Mono.error(
                                                    new AiProviderException(
                                                            "OpenAI API error: "
                                                                    + response.statusCode()
                                                                    + " - "
                                                                    + body
                                                    )
                                            )
                                    )
                    )
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));

            if (responseBody == null || responseBody.isBlank()) {
                throw new AiProviderException(
                        "OpenAI returned an empty response"
                );
            }

            JsonNode root =
                    objectMapper.readTree(responseBody);

            JsonNode content =
                    root.path("choices")
                            .path(0)
                            .path("message")
                            .path("content");

            if (content.isMissingNode() ||
                    content.isNull() ||
                    content.asText().isBlank()) {

                throw new AiProviderException(
                        "OpenAI response does not contain message content"
                );
            }

            AiInvestigationResponse response =
                    objectMapper.readValue(
                            content.asText(),
                            AiInvestigationResponse.class
                    );

            responseValidator.validate(
                    request,
                    response
            );

            return response;

        } catch (AiProviderException e) {
            throw e;

        } catch (Exception e) {
            throw new AiProviderException(
                    "Failed to process OpenAI investigation response",
                    e
            );
        }
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

        if (message == null) {
            return false;
        }

        return message.contains("500")
                || message.contains("502")
                || message.contains("503")
                || message.contains("504")
                || message.contains("429");
    }

    private void sleepBeforeRetry(int attempt) {

        try {
            Thread.sleep(RETRY_DELAY_MS * attempt);
        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new AiProviderException(
                    "OpenAI retry interrupted",
                    e
            );
        }
    }

    private record OpenAiRequest(
            String model,
            String prompt
    ) {
    }
}

