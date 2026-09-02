package com.aifincontroller.ai.provider;

import com.aifincontroller.ai.dto.AiInvestigationRequest;
import com.aifincontroller.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiProviderTest {

    @Test
    void shouldRejectWhenAiProviderIsDisabled() {

        AiProperties properties = new AiProperties();
        properties.setEnabled(false);

        OpenAiProvider provider = createProvider(properties);

        assertThrows(
                AiProviderException.class,
                () -> provider.investigate(
                        new AiInvestigationRequest()
                )
        );
    }

    @Test
    void shouldRejectWhenApiKeyIsMissing() {

        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiKey("");

        OpenAiProvider provider = createProvider(properties);

        assertThrows(
                AiProviderException.class,
                () -> provider.investigate(
                        new AiInvestigationRequest()
                )
        );
    }

    @Test
    void shouldRejectNullInvestigationRequest() {

        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");

        OpenAiProvider provider = createProvider(properties);

        assertThrows(
                AiProviderException.class,
                () -> provider.investigate(null)
        );
    }

    @Test
    void shouldRetryTransientProviderFailure() {

        AtomicInteger attempts = new AtomicInteger();

        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    attempts.incrementAndGet();

                    return reactor.core.publisher.Mono.error(
                            new java.net.ConnectException(
                                    "Connection failed"
                            )
                    );
                })
                .build();

        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");

        OpenAiProvider provider = createProvider(
                properties,
                webClient
        );

        assertThrows(
                AiProviderException.class,
                () -> provider.investigate(
                        new AiInvestigationRequest()
                )
        );

        assertEquals(3, attempts.get());
    }

    @Test
    void shouldRetryRateLimitFailure() {

        AtomicInteger attempts = new AtomicInteger();

        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    attempts.incrementAndGet();

                    return reactor.core.publisher.Mono.error(
                            new AiProviderException(
                                    "OpenAI API error: 429 - Too Many Requests"
                            )
                    );
                })
                .build();

        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");

        OpenAiProvider provider = createProvider(
                properties,
                webClient
        );

        assertThrows(
                AiProviderException.class,
                () -> provider.investigate(
                        new AiInvestigationRequest()
                )
        );

        assertEquals(3, attempts.get());
    }

    @Test
    void shouldRetryServerError() {

        AtomicInteger attempts = new AtomicInteger();

        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    attempts.incrementAndGet();

                    return reactor.core.publisher.Mono.error(
                            new AiProviderException(
                                    "OpenAI API error: 500 - Internal Server Error"
                            )
                    );
                })
                .build();

        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");

        OpenAiProvider provider = createProvider(
                properties,
                webClient
        );

        assertThrows(
                AiProviderException.class,
                () -> provider.investigate(
                        new AiInvestigationRequest()
                )
        );

        assertEquals(3, attempts.get());
    }

    @Test
    void shouldNotRetryPermanentProviderFailure() {

        AtomicInteger attempts = new AtomicInteger();

        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    attempts.incrementAndGet();

                    return reactor.core.publisher.Mono.error(
                            new AiProviderException(
                                    "Permanent provider failure"
                            )
                    );
                })
                .build();

        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");

        OpenAiProvider provider = createProvider(
                properties,
                webClient
        );

        assertThrows(
                AiProviderException.class,
                () -> provider.investigate(
                        new AiInvestigationRequest()
                )
        );

        assertEquals(1, attempts.get());
    }

    private OpenAiProvider createProvider(
            AiProperties properties) {

        return createProvider(
                properties,
                WebClient.builder().build()
        );
    }

    private OpenAiProvider createProvider(
            AiProperties properties,
            WebClient webClient) {

        ObjectMapper objectMapper = new ObjectMapper();

        AiInvestigationPromptBuilder promptBuilder =
                new AiInvestigationPromptBuilder(objectMapper);

        AiConfidenceValidator confidenceValidator =
                new AiConfidenceValidator();

        AiRecommendationValidator recommendationValidator =
                new AiRecommendationValidator();

        AiInvestigationResponseValidator responseValidator =
                new AiInvestigationResponseValidator(
                        confidenceValidator,
                        recommendationValidator,
                        new AiEvidenceReferenceValidator(),
                        new AiInvestigationConsistencyValidator()
                );

        return new OpenAiProvider(
                properties,
                webClient,
                objectMapper,
                promptBuilder,
                responseValidator
        );
    }
}
