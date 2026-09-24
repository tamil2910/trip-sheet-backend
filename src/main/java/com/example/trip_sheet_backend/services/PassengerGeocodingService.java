package com.example.trip_sheet_backend.services;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class PassengerGeocodingService {

    private final RestClient client;
    private final ConcurrentMap<String, GeoPoint> cache = new ConcurrentHashMap<>();
    private final Object requestLock = new Object();
    private final long minimumRequestIntervalMillis;
    private final String apiKey;
    private final boolean googleProvider;
    private long lastRequestAt;

    public PassengerGeocodingService(
            @Value("${passenger-routing.geocoding-url:https://nominatim.openstreetmap.org}") String geocodingUrl,
            @Value("${passenger-routing.user-agent:trip-sheet-backend}") String userAgent,
            @Value("${passenger-routing.minimum-request-interval-millis:1100}") long minimumRequestIntervalMillis,
            @Value("${passenger-routing.api-key:}") String apiKey) {
        this.client = RestClient.builder()
                .baseUrl(geocodingUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();
        this.minimumRequestIntervalMillis = minimumRequestIntervalMillis;
        this.apiKey = apiKey;
        this.googleProvider = geocodingUrl.contains("googleapis.com");
    }

    public GeoPoint geocode(String query) {
        String cacheKey = query.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        GeoPoint cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        for (String candidate : searchCandidates(query)) {
            JsonNode response;
            try {
                waitForProviderSlot();
            response = client.get()
                .uri(uriBuilder -> buildGeocodingUri(uriBuilder, candidate))
                        .retrieve()
                .body(JsonNode.class);
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().value() == 429) {
                    throw new IllegalArgumentException(
                            "Geocoding provider rate limit reached. Send latitude/longitude in the passenger payload "
                                    + "or configure a provider that supports bulk geocoding.");
                }
                throw exception;
            }

            validateGoogleResponse(response, candidate);
            GeoPoint point = extractPoint(response);
            if (point != null) {
                cache.putIfAbsent(cacheKey, point);
                return point;
            }
        }

        throw new IllegalArgumentException("Unable to find geographic location for: " + query);
    }

    private void validateGoogleResponse(JsonNode response, String candidate) {
        if (!googleProvider || response == null) {
            return;
        }
        String status = response.path("status").asText();
        if ("OK".equals(status) || "ZERO_RESULTS".equals(status)) {
            return;
        }
        String providerMessage = response.path("error_message").asText("");
        throw new IllegalArgumentException(
                "Google geocoding failed for '" + candidate + "': " + status
                        + (providerMessage.isBlank() ? "" : " - " + providerMessage));
    }

    private java.net.URI buildGeocodingUri(
            org.springframework.web.util.UriBuilder uriBuilder,
            String candidate) {
        if (googleProvider) {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalArgumentException("Google geocoding API key is not configured");
            }
            return uriBuilder
                    .queryParam("address", candidate)
                    .queryParam("key", apiKey)
                    .build();
        }
        return uriBuilder
                .path("/search")
                .queryParam("q", candidate)
                .queryParam("format", "jsonv2")
                .queryParam("limit", 1)
                .build();
    }

    private GeoPoint extractPoint(JsonNode response) {
        if (response == null) {
            return null;
        }
        if (googleProvider) {
            JsonNode location = response.path("results").path(0).path("geometry").path("location");
            return location.has("lat") && location.has("lng")
                    ? new GeoPoint(location.path("lat").asDouble(), location.path("lng").asDouble())
                    : null;
        }
        JsonNode result = response.isArray() && !response.isEmpty() ? response.path(0) : null;
        return result != null && result.has("lat") && result.has("lon")
                ? new GeoPoint(result.path("lat").asDouble(), result.path("lon").asDouble())
                : null;
    }

    private void waitForProviderSlot() {
        synchronized (requestLock) {
            long waitMillis = minimumRequestIntervalMillis - (System.currentTimeMillis() - lastRequestAt);
            if (waitMillis > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(waitMillis);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for geocoding provider", exception);
                }
            }
            lastRequestAt = System.currentTimeMillis();
        }
    }

    private List<String> searchCandidates(String query) {
        String normalized = query.trim().replaceAll("\\s+", " ");
        String[] parts = normalized.split("\\s*,\\s*");
        List<String> candidates = new ArrayList<>();
        candidates.add(normalized);

        if (parts.length >= 2) {
            String city = parts[parts.length - 1];
            String location = parts[parts.length - 2];
            candidates.add(location + ", " + city);
            candidates.add(location + " bus stop, " + city);
            candidates.add(location + " bus stand, " + city);
            candidates.add(location + " bus station, " + city);
        }

        if (parts.length >= 1) {
            candidates.add(parts[parts.length - 1]);
        }

        return candidates.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    public record GeoPoint(double latitude, double longitude) {
    }
}