package com.example.trip_sheet_backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmailService {

    private final RestTemplate restTemplate;
    private final String sendGridApiKey;
    private final String fromEmail;
    private static final String SENDGRID_API_URL = "https://api.sendgrid.com/v3/mail/send";

    public EmailService(
            @Value("${sendgrid.api.key:}") String sendGridApiKey,
            @Value("${sendgrid.from.email:}") String fromEmail) {
        this.restTemplate = new RestTemplate();
        this.sendGridApiKey = sendGridApiKey;
        this.fromEmail = fromEmail;
    }

    public void sendTenantOnboardingEmail(String toEmail, String tenantName, String username, String rawPassword) {
        if (toEmail == null || toEmail.isBlank()) {
            throw new RuntimeException("Recipient email is required");
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            throw new RuntimeException("SendGrid sender email is not configured. Set SENDGRID_FROM_EMAIL or sendgrid.from.email");
        }

        if (sendGridApiKey == null || sendGridApiKey.isBlank()) {
            throw new RuntimeException("SendGrid API key is not configured. Set SENDGRID_API_KEY or email_config_key");
        }

        try {
            Map<String, Object> emailPayload = buildEmailPayload(toEmail, tenantName, username, rawPassword);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + sendGridApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailPayload, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(SENDGRID_API_URL, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                        "SendGrid rejected the message. Status=" + response.getStatusCodeValue()
                                + ", Body=" + response.getBody()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Error sending onboarding email: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildEmailPayload(String toEmail, String tenantName, String username, String rawPassword) {
        Map<String, Object> payload = new HashMap<>();

        Map<String, String> from = new HashMap<>();
        from.put("email", fromEmail);
        payload.put("from", from);

        List<Map<String, Object>> personalizations = new ArrayList<>();
        Map<String, Object> personalization = new HashMap<>();
        
        List<Map<String, String>> to = new ArrayList<>();
        Map<String, String> recipient = new HashMap<>();
        recipient.put("email", toEmail);
        to.add(recipient);
        personalization.put("to", to);
        personalizations.add(personalization);
        
        payload.put("personalizations", personalizations);
        payload.put("subject", "Your Trip Sheet account is ready");

        List<Map<String, String>> content = new ArrayList<>();
        Map<String, String> textContent = new HashMap<>();
        textContent.put("type", "text/plain");
        textContent.put("value", buildOnboardingBody(tenantName, username, rawPassword));
        content.add(textContent);
        
        payload.put("content", content);

        return payload;
    }

    private String buildOnboardingBody(String tenantName, String username, String rawPassword) {
        StringBuilder body = new StringBuilder();
        body.append("Hello ").append(tenantName == null ? "User" : tenantName).append(",\n\n");
        body.append("Your tenant account has been created successfully.\n");
        body.append("Use the below credentials to login and complete your profile setup:\n\n");
        body.append("Username: ").append(username).append("\n");
        body.append("Temporary Password: ").append(rawPassword).append("\n\n");
        body.append("Login URL: http://localhost:4200/login\n\n");
        body.append("Please change your password immediately after first login.\n\n");
        body.append("Thanks,\nTrip Sheet Team");
        return body.toString();
    }

    public void sendDriverPasswordEmail(String toEmail, String driverName, String username, String rawPassword) {
        if (toEmail == null || toEmail.isBlank()) {
            throw new RuntimeException("Recipient email is required");
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            throw new RuntimeException("SendGrid sender email is not configured. Set SENDGRID_FROM_EMAIL or sendgrid.from.email");
        }

        if (sendGridApiKey == null || sendGridApiKey.isBlank()) {
            throw new RuntimeException("SendGrid API key is not configured. Set SENDGRID_API_KEY or email_config_key");
        }

        try {
            Map<String, Object> emailPayload = buildDriverPasswordPayload(toEmail, driverName, username, rawPassword);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + sendGridApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailPayload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(SENDGRID_API_URL, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                        "SendGrid rejected the message. Status=" + response.getStatusCodeValue()
                                + ", Body=" + response.getBody()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Error sending driver password email: " + e.getMessage(), e);
        }
    }

    public void sendTripFeedbackEmail(String toEmail, String passengerName, String feedbackLink) {
        if (toEmail == null || toEmail.isBlank()) {
            throw new RuntimeException("Recipient email is required");
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            throw new RuntimeException("SendGrid sender email is not configured. Set SENDGRID_FROM_EMAIL or sendgrid.from.email");
        }

        if (sendGridApiKey == null || sendGridApiKey.isBlank()) {
            throw new RuntimeException("SendGrid API key is not configured. Set SENDGRID_API_KEY or email_config_key");
        }

        try {
            Map<String, Object> emailPayload = buildTripFeedbackPayload(toEmail, passengerName, feedbackLink);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + sendGridApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailPayload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(SENDGRID_API_URL, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                        "SendGrid rejected the message. Status=" + response.getStatusCodeValue()
                                + ", Body=" + response.getBody()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Error sending trip feedback email: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildTripFeedbackPayload(String toEmail, String passengerName, String feedbackLink) {
        Map<String, Object> payload = new HashMap<>();

        Map<String, String> from = new HashMap<>();
        from.put("email", fromEmail);
        payload.put("from", from);

        List<Map<String, Object>> personalizations = new ArrayList<>();
        Map<String, Object> personalization = new HashMap<>();

        List<Map<String, String>> to = new ArrayList<>();
        Map<String, String> recipient = new HashMap<>();
        recipient.put("email", toEmail);
        to.add(recipient);
        personalization.put("to", to);
        personalizations.add(personalization);

        payload.put("personalizations", personalizations);
        payload.put("subject", "We would like your trip feedback");

        List<Map<String, String>> content = new ArrayList<>();
        Map<String, String> textContent = new HashMap<>();
        textContent.put("type", "text/plain");
        textContent.put("value", buildTripFeedbackBody(passengerName, feedbackLink));
        content.add(textContent);

        payload.put("content", content);

        return payload;
    }

    private String buildTripFeedbackBody(String passengerName, String feedbackLink) {
        StringBuilder body = new StringBuilder();
        body.append("Hello ").append(passengerName == null ? "Guest" : passengerName).append(",\n\n");
        body.append("Your trip has been completed and we would like your feedback.\n");
        body.append("Please use the secure link below to rate your experience:\n\n");
        body.append(feedbackLink).append("\n\n");
        body.append("Thanks,\nTrip Sheet Team");
        return body.toString();
    }

    private Map<String, Object> buildDriverPasswordPayload(String toEmail, String driverName, String username, String rawPassword) {
        Map<String, Object> payload = new HashMap<>();

        Map<String, String> from = new HashMap<>();
        from.put("email", fromEmail);
        payload.put("from", from);

        List<Map<String, Object>> personalizations = new ArrayList<>();
        Map<String, Object> personalization = new HashMap<>();

        List<Map<String, String>> to = new ArrayList<>();
        Map<String, String> recipient = new HashMap<>();
        recipient.put("email", toEmail);
        to.add(recipient);
        personalization.put("to", to);
        personalizations.add(personalization);

        payload.put("personalizations", personalizations);
        payload.put("subject", "Your Trip Sheet driver password");

        List<Map<String, String>> content = new ArrayList<>();
        Map<String, String> textContent = new HashMap<>();
        textContent.put("type", "text/plain");
        textContent.put("value", buildDriverPasswordBody(driverName, username, rawPassword));
        content.add(textContent);

        payload.put("content", content);

        return payload;
    }

    private String buildDriverPasswordBody(String driverName, String username, String rawPassword) {
        StringBuilder body = new StringBuilder();
        body.append("Hello ").append(driverName == null ? "Driver" : driverName).append(",\n\n");
        body.append("Your Trip Sheet password has been set successfully.\n");
        body.append("Use the credentials below to login:\n\n");
        body.append("Username: ").append(username).append("\n");
        body.append("Password: ").append(rawPassword).append("\n\n");
        body.append("Login URL: http://localhost:4200/login\n\n");
        body.append("Please change your password after login if needed.\n\n");
        body.append("Thanks,\nTrip Sheet Team");
        return body.toString();
    }

    public void sendPasswordResetOTP(String toEmail, String otpCode) {
        if (toEmail == null || toEmail.isBlank()) {
            throw new RuntimeException("Recipient email is required");
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            throw new RuntimeException("SendGrid sender email is not configured. Set SENDGRID_FROM_EMAIL or sendgrid.from.email");
        }

        if (sendGridApiKey == null || sendGridApiKey.isBlank()) {
            throw new RuntimeException("SendGrid API key is not configured. Set SENDGRID_API_KEY or email_config_key");
        }

        try {
            Map<String, Object> emailPayload = buildPasswordResetPayload(toEmail, otpCode);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + sendGridApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailPayload, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(SENDGRID_API_URL, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                        "SendGrid rejected the message. Status=" + response.getStatusCodeValue()
                                + ", Body=" + response.getBody()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Error sending password reset email: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildPasswordResetPayload(String toEmail, String otpCode) {
        Map<String, Object> payload = new HashMap<>();

        Map<String, String> from = new HashMap<>();
        from.put("email", fromEmail);
        payload.put("from", from);

        List<Map<String, Object>> personalizations = new ArrayList<>();
        Map<String, Object> personalization = new HashMap<>();
        
        List<Map<String, String>> to = new ArrayList<>();
        Map<String, String> recipient = new HashMap<>();
        recipient.put("email", toEmail);
        to.add(recipient);
        personalization.put("to", to);
        personalizations.add(personalization);
        
        payload.put("personalizations", personalizations);
        payload.put("subject", "Password Reset OTP - Trip Sheet");

        List<Map<String, String>> content = new ArrayList<>();
        Map<String, String> textContent = new HashMap<>();
        textContent.put("type", "text/plain");
        textContent.put("value", buildPasswordResetBody(otpCode));
        content.add(textContent);
        
        payload.put("content", content);

        return payload;
    }

    private String buildPasswordResetBody(String otpCode) {
        StringBuilder body = new StringBuilder();
        body.append("Hello,\n\n");
        body.append("You requested a password reset for your Trip Sheet account.\n");
        body.append("Use the following OTP to verify your identity and reset your password:\n\n");
        body.append("OTP Code: ").append(otpCode).append("\n\n");
        body.append("This OTP is valid for 15 minutes.\n");
        body.append("If you didn't request a password reset, please ignore this email.\n\n");
        body.append("Thanks,\nTrip Sheet Team");
        return body.toString();
    }
}
