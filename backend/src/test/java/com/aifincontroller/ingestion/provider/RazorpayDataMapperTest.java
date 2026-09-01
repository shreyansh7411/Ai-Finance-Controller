package com.aifincontroller.ingestion.provider;

import com.aifincontroller.domain.Adjustment;
import com.aifincontroller.domain.Payment;
import com.aifincontroller.domain.Refund;
import com.aifincontroller.domain.Settlement;
import com.aifincontroller.ingestion.provider.dto.RazorpayAdjustmentDto;
import com.aifincontroller.ingestion.provider.dto.RazorpayPaymentDto;
import com.aifincontroller.ingestion.provider.dto.RazorpayRefundDto;
import com.aifincontroller.ingestion.provider.dto.RazorpaySettlementDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RazorpayDataMapperTest {

        private final RazorpayDataMapper mapper = new RazorpayDataMapper();

        @Test
        void mapsPayment() {

                RazorpayPaymentDto dto = new RazorpayPaymentDto(
                                "pay_test123",
                                "order_test123",
                                15000L,
                                "INR",
                                "captured",
                                1700000000L,
                                1699990000L);

                Payment payment = mapper.toPayment(dto, "batch_test");

                assertEquals("pay_test123", payment.getPaymentId());
                assertEquals("batch_test", payment.getBatchId());
                assertEquals("order_test123", payment.getOrderId());
                assertEquals("150", payment.getAmount().toPlainString());
                assertEquals("INR", payment.getCurrency());
                assertEquals("captured", payment.getStatus());
                assertNotNull(payment.getCapturedAt());
                assertNotNull(payment.getCreatedAt());
        }

        @Test
        void mapsSettlement() {

                RazorpaySettlementDto dto = new RazorpaySettlementDto(
                                "setl_test123",
                                "pay_test123",
                                10000L,
                                200L,
                                36L,
                                "processed",
                                "utr_test123",
                                1700000000L);

                Settlement settlement = mapper.toSettlement(dto);

                assertEquals(
                                "setl_test123",
                                settlement.getSettlementId());

                assertEquals(
                                "pay_test123",
                                settlement.getPaymentId());

                assertEquals(
                                "100",
                                settlement.getAmount().toPlainString());

                assertEquals(
                                "2",
                                settlement.getFees().toPlainString());

                assertEquals(
                                "0.36",
                                settlement.getTax().toPlainString());

                assertEquals(
                                "processed",
                                settlement.getStatus());

                assertEquals(
                                "utr_test123",
                                settlement.getUtr());

                assertNotNull(settlement.getSettledAt());
        }

        @Test
        void mapsRefund() {

                RazorpayRefundDto dto = new RazorpayRefundDto(
                                "rfnd_test123",
                                "pay_test123",
                                5000L,
                                "processed",
                                1700000000L);

                Refund refund = mapper.toRefund(dto);

                assertEquals(
                                "rfnd_test123",
                                refund.getRefundId());

                assertEquals(
                                "pay_test123",
                                refund.getPaymentId());

                assertEquals(
                                "50",
                                refund.getAmount().toPlainString());

                assertEquals(
                                "processed",
                                refund.getStatus());

                assertNotNull(refund.getCreatedAt());
        }

        @Test
        void mapsAdjustment() {

                RazorpayAdjustmentDto dto = new RazorpayAdjustmentDto(
                                "adj_test123",
                                "setl_test123",
                                2500L,
                                "credit",
                                "Test adjustment",
                                1700000000L);

                Adjustment adjustment = mapper.toAdjustment(dto);

                assertEquals(
                                "adj_test123",
                                adjustment.getAdjustmentId());

                assertEquals(
                                "setl_test123",
                                adjustment.getSettlementId());

                assertEquals(
                                "25",
                                adjustment.getAmount().toPlainString());

                assertEquals(
                                "credit",
                                adjustment.getType());

                assertEquals(
                                "Test adjustment",
                                adjustment.getDescription());

                assertNotNull(adjustment.getCreatedAt());
        }

    @Test
    void rejectsPaymentWithoutRequiredId() {

        RazorpayPaymentDto dto =
                new RazorpayPaymentDto(
                        null,
                        "order_test123",
                        1000L,
                        "INR",
                        "captured",
                        null,
                        null);

        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toPayment(dto, "batch_test"));
    }
}