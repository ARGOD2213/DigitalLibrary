package com.digitallibrary.security;

import com.digitallibrary.BaseIntegrationTest;
import com.digitallibrary.entity.AppUser;
import com.digitallibrary.entity.Payment;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentWebhookSecurityIntegrationTest extends BaseIntegrationTest {

    private static final String MOCK_SECRET = "test-mock-webhook-secret";

    private Payment pendingPayment(String transactionId) {
        AppUser user = createUser("payer@example.com", "ROLE_USER");
        Payment payment = new Payment(user, "MOCK", transactionId, new BigDecimal("100.00"), "INR", "PENDING");
        return paymentRepository.save(payment);
    }

    private String sign(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(MOCK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void webhook_WithoutSignature_ShouldReturn401Unauthorized() throws Exception {
        Payment payment = pendingPayment("TXN-NOSIG");

        mockMvc.perform(post("/api/payments/webhook/mock")
                        .content(payment.getTransactionId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhook_WithInvalidSignature_ShouldReturn401AndNotMarkPaymentSuccess() throws Exception {
        Payment payment = pendingPayment("TXN-BADSIG");

        mockMvc.perform(post("/api/payments/webhook/mock")
                        .header("X-Webhook-Signature", "deadbeef")
                        .content(payment.getTransactionId()))
                .andExpect(status().isUnauthorized());

        Payment reloaded = paymentRepository.findByTransactionId(payment.getTransactionId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("PENDING", reloaded.getStatus());
    }

    @Test
    void webhook_ForUnconfiguredGateway_ShouldReturn401Unauthorized() throws Exception {
        Payment payment = pendingPayment("TXN-NOGATEWAY");

        mockMvc.perform(post("/api/payments/webhook/stripe")
                        .header("X-Webhook-Signature", "irrelevant")
                        .content(payment.getTransactionId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhook_WithValidSignature_ShouldMarkPaymentSuccess() throws Exception {
        Payment payment = pendingPayment("TXN-GOODSIG");
        String signature = sign(payment.getTransactionId());

        mockMvc.perform(post("/api/payments/webhook/mock")
                        .header("X-Webhook-Signature", signature)
                        .content(payment.getTransactionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    void webhook_ReplayedAfterSuccess_ShouldStayIdempotent() throws Exception {
        Payment payment = pendingPayment("TXN-REPLAY");
        String signature = sign(payment.getTransactionId());

        mockMvc.perform(post("/api/payments/webhook/mock")
                        .header("X-Webhook-Signature", signature)
                        .content(payment.getTransactionId()))
                .andExpect(status().isOk());

        // Second delivery of the same webhook (gateways retry) must not reprocess side effects.
        mockMvc.perform(post("/api/payments/webhook/mock")
                        .header("X-Webhook-Signature", signature)
                        .content(payment.getTransactionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }
}
