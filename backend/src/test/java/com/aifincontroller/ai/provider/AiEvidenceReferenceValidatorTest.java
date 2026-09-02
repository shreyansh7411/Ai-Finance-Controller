package com.aifincontroller.ai.provider;

import com.aifincontroller.ai.dto.AiInvestigationRequest;
import com.aifincontroller.ai.dto.AiInvestigationResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiEvidenceReferenceValidatorTest {

    private final AiEvidenceReferenceValidator validator =
            new AiEvidenceReferenceValidator();

    private AiInvestigationRequest requestWithPayment() {

        AiInvestigationRequest request =
                new AiInvestigationRequest();

        AiInvestigationRequest.PaymentEvidence payment =
                new AiInvestigationRequest.PaymentEvidence();

        payment.setAmount(new BigDecimal("1000"));

        request.setPayment(payment);

        request.setEvidenceIds(
                List.of(
                        "PAYMENT_ID",
                        "PAYMENT_ORDER_ID",
                        "PAYMENT_AMOUNT",
                        "PAYMENT_CURRENCY",
                        "PAYMENT_STATUS"
                )
        );

        return request;
    }

    private AiInvestigationResponse response(
            String reference) {

        AiInvestigationResponse response =
                new AiInvestigationResponse();

        response.setEvidenceReferences(
                List.of(reference)
        );

        return response;
    }

    @Test
    void supportedPaymentReferenceIsAccepted() {

        AiInvestigationRequest request =
                requestWithPayment();

        AiInvestigationResponse response =
                response("PAYMENT_AMOUNT");

        assertDoesNotThrow(() ->
                validator.validate(request, response)
        );
    }

    @Test
    void unsupportedReferenceIsRejected() {

        AiInvestigationRequest request =
                requestWithPayment();

        AiInvestigationResponse response =
                response("MERCHANT_BALANCE");

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(request, response)
        );
    }

    @Test
    void referenceForMissingEvidenceIsRejected() {

        AiInvestigationRequest request =
                requestWithPayment();

        AiInvestigationResponse response =
                response("SETTLEMENT_AMOUNT");

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(request, response)
        );
    }

    @Test
    void emptyReferenceIsRejected() {

        AiInvestigationRequest request =
                requestWithPayment();

        AiInvestigationResponse response =
                response(" ");

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(request, response)
        );
    }

    @Test
    void multipleSupportedReferencesAreAccepted() {

        AiInvestigationRequest request =
                requestWithPayment();

        AiInvestigationResponse response =
                new AiInvestigationResponse();

        response.setEvidenceReferences(
                List.of(
                        "PAYMENT_AMOUNT",
                        "PAYMENT_ORDER_ID",
                        "PAYMENT_STATUS"
                )
        );

        assertDoesNotThrow(() ->
                validator.validate(request, response)
        );
    }

    @Test
    void mixedSupportedAndUnsupportedReferencesAreRejected() {

        AiInvestigationRequest request =
                requestWithPayment();

        AiInvestigationResponse response =
                new AiInvestigationResponse();

        response.setEvidenceReferences(
                List.of(
                        "PAYMENT_AMOUNT",
                        "MERCHANT_BALANCE"
                )
        );

        assertThrows(
                AiProviderException.class,
                () -> validator.validate(request, response)
        );
    }
}
