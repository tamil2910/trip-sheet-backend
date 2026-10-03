package com.example.trip_sheet_backend.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class WhatsAppWebhookControllerTest {

    private static final String VERIFY_TOKEN = "test-verify-token";
    private static final String APP_SECRET = "test-meta-app-secret";

    private WhatsAppWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new WhatsAppWebhookController(VERIFY_TOKEN, APP_SECRET);
    }

    @Test
    void returnsChallengeForValidSubscriptionVerification() {
        var response = controller.verifyWebhook("subscribe", VERIFY_TOKEN, "challenge-value");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("challenge-value", response.getBody());
    }

    @Test
    void rejectsInvalidSubscriptionVerification() {
        var response = controller.verifyWebhook("subscribe", "incorrect-token", "challenge-value");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void acceptsPayloadSignedWithMetaAppSecret() throws Exception {
        byte[] payload = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);

        var response = controller.receiveWebhook(payload, signatureFor(payload));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void rejectsUnsignedPayload() {
        var response = controller.receiveWebhook("{}".getBytes(StandardCharsets.UTF_8), null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    private String signatureFor(byte[] payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload));
    }
}