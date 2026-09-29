package com.example.trip_sheet_backend.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class PassengerRouteEstimateService {

    private final RestClient client;
    private final String apiKey;

    public PassengerRouteEstimateService(
            @Value("${passenger-routing.routes-url:https://routes.googleapis.com}") String routesUrl,
            @Value("${passenger-routing.api-key:}") String apiKey) {
        this.client = RestClient.builder().baseUrl(routesUrl).build();
        this.apiKey = apiKey;
    }

    public RouteEstimate estimate(List<PassengerGeocodingService.GeoPoint> orderedPickups,
            PassengerGeocodingService.GeoPoint destination) {
        if (orderedPickups == null || orderedPickups.isEmpty()) {
            throw new IllegalArgumentException("At least one passenger pickup is required for route estimation");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Google Maps API key is required when isEstimatedHrKm is true");
        }

        PassengerGeocodingService.GeoPoint origin = orderedPickups.get(0);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("origin", waypoint(origin));
        request.put("destination", waypoint(destination));
        if (orderedPickups.size() > 1) {
            List<Map<String, Object>> intermediateStops = new ArrayList<>();
            for (int index = 1; index < orderedPickups.size(); index++) {
                intermediateStops.add(waypoint(orderedPickups.get(index)));
            }
            request.put("intermediates", intermediateStops);
        }
        request.put("travelMode", "DRIVE");
        request.put("routingPreference", "TRAFFIC_AWARE");

        JsonNode response = client.post()
                .uri("/directions/v2:computeRoutes")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", "routes.distanceMeters,routes.duration,routes.legs.distanceMeters,routes.legs.duration")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        JsonNode route = response == null ? null : response.path("routes").path(0);
        if (route == null || route.isMissingNode()) {
            throw new IllegalArgumentException("Google Routes API returned no route for this passenger group");
        }

        List<RouteLeg> legs = new ArrayList<>();
        JsonNode legsNode = route.path("legs");
        if (legsNode.isArray()) {
            for (JsonNode leg : legsNode) {
                legs.add(new RouteLeg(
                        leg.path("distanceMeters").asDouble() / 1000.0,
                        secondsToMinutes(leg.path("duration").asText())));
            }
        }
        return new RouteEstimate(
                route.path("distanceMeters").asDouble() / 1000.0,
                secondsToMinutes(route.path("duration").asText()),
                legs);
    }

    private Map<String, Object> waypoint(PassengerGeocodingService.GeoPoint point) {
        Map<String, Object> latLng = Map.of("latitude", point.latitude(), "longitude", point.longitude());
        return Map.of("location", Map.of("latLng", latLng));
    }

    private long secondsToMinutes(String duration) {
        if (duration == null || !duration.endsWith("s")) {
            return 0;
        }
        return Math.round(Double.parseDouble(duration.substring(0, duration.length() - 1)) / 60.0);
    }

    public record RouteLeg(double distanceKm, long durationMinutes) {
    }

    public record RouteEstimate(double distanceKm, long durationMinutes, List<RouteLeg> legs) {
    }
}