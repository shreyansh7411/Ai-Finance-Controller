package com.aifincontroller.razorpay;

import com.aifincontroller.config.RazorpayProperties;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class DefaultRazorpayClient implements RazorpayClient {

    private final WebClient webClient;
    private final RazorpayProperties properties;

    public DefaultRazorpayClient(
            WebClient.Builder webClientBuilder,
            RazorpayProperties properties) {

        this.properties = properties;

        this.webClient = webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public Map<String, Object> getPayment(String paymentId) {
        return get("/payments/" + paymentId);
    }

    @Override
    public Map<String, Object> getPayments(
            int count,
            int skip) {

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/payments")
                        .queryParam("count", count)
                        .queryParam("skip", skip)
                        .build())
                .headers(this::applyAuthentication)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @Override
    public Map<String, Object> getPaymentsForOrder(
            String orderId,
            int count,
            int skip) {

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/orders/{orderId}/payments")
                        .queryParam("count", count)
                        .queryParam("skip", skip)
                        .build(orderId))
                .headers(this::applyAuthentication)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @Override
    public Map<String, Object> getSettlement(
            String settlementId) {

        return get("/settlements/" + settlementId);
    }

    @Override
    public Map<String, Object> getSettlements(
            int count,
            int skip) {

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/settlements")
                        .queryParam("count", count)
                        .queryParam("skip", skip)
                        .build())
                .headers(this::applyAuthentication)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @Override
    public Map<String, Object> getSettlementRecon(
            int year,
            int month,
            Integer day,
            int count,
            int skip) {

        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/settlements/recon/combined")
                            .queryParam("year", year)
                            .queryParam("month", month)
                            .queryParam("count", count)
                            .queryParam("skip", skip);

                    if (day != null) {
                        builder.queryParam("day", day);
                    }

                    return builder.build();
                })
                .headers(this::applyAuthentication)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @Override
    public Map<String, Object> getRefund(
            String refundId) {

        return get("/refunds/" + refundId);
    }

    @Override
    public Map<String, Object> getRefunds(
            int count,
            int skip) {

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/refunds")
                        .queryParam("count", count)
                        .queryParam("skip", skip)
                        .build())
                .headers(this::applyAuthentication)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    private Map<String, Object> get(String path) {

        return webClient.get()
                .uri(path)
                .headers(this::applyAuthentication)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    private void applyAuthentication(
            HttpHeaders headers) {

        if (!properties.isEnabled()) {
            throw new IllegalStateException(
                    "Razorpay integration is disabled. "
                            + "Set RAZORPAY_ENABLED=true to enable it.");
        }

        if (properties.getKeyId() == null
                || properties.getKeyId().isBlank()
                || properties.getKeySecret() == null
                || properties.getKeySecret().isBlank()) {

            throw new IllegalStateException(
                    "Razorpay credentials are not configured.");
        }

        headers.setBasicAuth(
                properties.getKeyId(),
                properties.getKeySecret());
    }
}
