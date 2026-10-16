package com.example.trip_sheet_backend.controllers;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Receives and reports RazorpayX payout lifecycle notifications. */
@RestController
@RequestMapping("/webhooks/razorpay")
public class RazorpayWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final String webhookSecret;
    private final ObjectMapper objectMapper;

    public RazorpayWebhookController(
            @Value("${razorpay.webhook.secret:}") String webhookSecret,
            ObjectMapper objectMapper) {
        this.webhookSecret = webhookSecret;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receiveWebhook(
            @RequestBody byte[] payload,
            @RequestHeader(name = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(name = "X-Razorpay-Event-Id", required = false) String eventId) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!isValidSignature(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText("");
            if (event.startsWith("payout.")) {
                JsonNode payout = root.path("payload").path("payout").path("entity");
                logger.info("Razorpay payout webhook received: eventId={}, event={}, payoutId={}, status={}",
                        safe(eventId), event, safe(payout.path("id").asText(null)),
                        safe(payout.path("status").asText(null)));
            } else {
                logger.debug("Razorpay webhook received: eventId={}, event={}", safe(eventId), safe(event));
            }
        } catch (Exception ex) {
            // A signed but malformed payload should be retried and investigated, not acknowledged.
            logger.warn("Unable to parse signed Razorpay webhook payload", ex);
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isValidSignature(byte[] payload, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(payload);
            byte[] supplied = HexFormat.of().parseHex(signature);
            return MessageDigest.isEqual(expected, supplied);
        } catch (IllegalArgumentException ex) {
            return false;
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to verify Razorpay webhook signature", ex);
        }
    }
}
