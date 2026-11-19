package com.example.trip_sheet_backend.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
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
            return estimateWithoutGoogleRoutes(orderedPickups, destination);
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

    /** Returns passenger stop indexes in a low-backtracking order and the estimated legs for that order. */
    public RoutePlan optimizeStops(List<PassengerGeocodingService.GeoPoint> stops,
            PassengerGeocodingService.GeoPoint office, boolean homeToOffice) {
        if (stops == null || stops.isEmpty()) {
            throw new IllegalArgumentException("At least one passenger stop is required for route optimization");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return optimizeWithoutGoogleRoutes(stops, office, homeToOffice);
        }
        RoutePlan best = null;
        for (int fixedStop = 0; fixedStop < stops.size(); fixedStop++) {
            RoutePlan candidate = computeOptimizedCandidate(stops, office, homeToOffice, fixedStop);
            if (best == null || candidate.durationMinutes() < best.durationMinutes()) best = candidate;
        }
        return best;
    }

    private RoutePlan computeOptimizedCandidate(List<PassengerGeocodingService.GeoPoint> stops,
            PassengerGeocodingService.GeoPoint office, boolean homeToOffice, int fixedStop) {
        List<Integer> intermediateIndexes = new ArrayList<>();
        for (int index = 0; index < stops.size(); index++) {
            if (index != fixedStop) intermediateIndexes.add(index);
        }

        PassengerGeocodingService.GeoPoint origin = homeToOffice ? stops.get(fixedStop) : office;
        PassengerGeocodingService.GeoPoint destination = homeToOffice ? office : stops.get(fixedStop);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("origin", waypoint(origin));
        request.put("destination", waypoint(destination));
        if (!intermediateIndexes.isEmpty()) {
            request.put("intermediates", intermediateIndexes.stream().map(index -> waypoint(stops.get(index))).toList());
            request.put("optimizeWaypointOrder", true);
        }
        request.put("travelMode", "DRIVE");
        request.put("routingPreference", "TRAFFIC_AWARE");

        JsonNode response = client.post()
                .uri("/directions/v2:computeRoutes")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", "routes.distanceMeters,routes.duration,routes.legs.distanceMeters,routes.legs.duration,routes.optimizedIntermediateWaypointIndex")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);
        JsonNode route = response == null ? null : response.path("routes").path(0);
        if (route == null || route.isMissingNode()) {
            throw new IllegalArgumentException("Google Routes API returned no optimized route for this passenger group");
        }

        List<Integer> optimizedIntermediatePositions = new ArrayList<>();
        JsonNode optimized = route.path("optimizedIntermediateWaypointIndex");
        if (optimized.isArray() && optimized.size() == intermediateIndexes.size()) {
            for (JsonNode position : optimized) {
                int inputPosition = position.asInt();
                if (inputPosition < 0 || inputPosition >= intermediateIndexes.size()) {
                    throw new IllegalArgumentException("Google Routes API returned an invalid waypoint order");
                }
                optimizedIntermediatePositions.add(inputPosition);
            }
        } else {
            for (int index = 0; index < intermediateIndexes.size(); index++) optimizedIntermediatePositions.add(index);
        }

        List<Integer> orderedStopIndexes = new ArrayList<>();
        if (homeToOffice) orderedStopIndexes.add(fixedStop);
        for (Integer position : optimizedIntermediatePositions) orderedStopIndexes.add(intermediateIndexes.get(position));
        if (!homeToOffice) orderedStopIndexes.add(fixedStop);

        List<RouteLeg> legs = new ArrayList<>();
        JsonNode legsNode = route.path("legs");
        if (legsNode.isArray()) {
            for (JsonNode leg : legsNode) {
                legs.add(new RouteLeg(leg.path("distanceMeters").asDouble() / 1000.0,
                        secondsToMinutes(leg.path("duration").asText())));
            }
        }
        return new RoutePlan(orderedStopIndexes, legs,
                secondsToMinutes(route.path("duration").asText()));
    }

    private RoutePlan optimizeWithoutGoogleRoutes(List<PassengerGeocodingService.GeoPoint> stops,
            PassengerGeocodingService.GeoPoint office, boolean homeToOffice) {
        List<Integer> bestOrder = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        if (homeToOffice) {
            for (int start = 0; start < stops.size(); start++) {
                List<Integer> order = greedyStopOrder(stops, stops.get(start), start);
                double distance = pathDistance(stops, office, order, true);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestOrder = order;
                }
            }
        } else {
            bestOrder = greedyStopOrder(stops, office, -1);
        }

        List<RouteLeg> legs = new ArrayList<>();
        PassengerGeocodingService.GeoPoint current = homeToOffice
                ? stops.get(bestOrder.getFirst()) : office;
        for (int position = homeToOffice ? 1 : 0; position < bestOrder.size(); position++) {
            PassengerGeocodingService.GeoPoint next = stops.get(bestOrder.get(position));
            double km = straightLineDistanceKm(current, next) * 1.3;
            legs.add(new RouteLeg(km, estimatedMinutes(km)));
            current = next;
        }
        double finalKm = straightLineDistanceKm(current, homeToOffice ? office : stops.get(bestOrder.getLast())) * 1.3;
        // Office-to-home's final stop is already included in the loop; home-to-office ends at the office.
        if (homeToOffice) {
            legs.add(new RouteLeg(finalKm, estimatedMinutes(finalKm)));
        }
        return new RoutePlan(bestOrder, legs, legs.stream().mapToLong(RouteLeg::durationMinutes).sum());
    }

    private List<Integer> greedyStopOrder(List<PassengerGeocodingService.GeoPoint> stops,
            PassengerGeocodingService.GeoPoint initialPoint, int excludedIndex) {
        List<Integer> remaining = new ArrayList<>();
        for (int index = 0; index < stops.size(); index++) if (index != excludedIndex) remaining.add(index);
        List<Integer> order = new ArrayList<>();
        if (excludedIndex >= 0) order.add(excludedIndex);
        PassengerGeocodingService.GeoPoint current = initialPoint;
        while (!remaining.isEmpty()) {
            PassengerGeocodingService.GeoPoint currentPoint = current;
            int nextIndex = remaining.stream().min(Comparator.comparingDouble(
                    index -> straightLineDistanceKm(currentPoint, stops.get(index)))).orElseThrow();
            order.add(nextIndex);
            current = stops.get(nextIndex);
            remaining.remove(Integer.valueOf(nextIndex));
        }
        return order;
    }

    private double pathDistance(List<PassengerGeocodingService.GeoPoint> stops,
            PassengerGeocodingService.GeoPoint office, List<Integer> order, boolean returnToOffice) {
        double distance = 0;
        for (int index = 1; index < order.size(); index++) {
            distance += straightLineDistanceKm(stops.get(order.get(index - 1)), stops.get(order.get(index)));
        }
        if (returnToOffice && !order.isEmpty()) distance += straightLineDistanceKm(stops.get(order.getLast()), office);
        return distance;
    }

    private RouteEstimate estimateWithoutGoogleRoutes(
            List<PassengerGeocodingService.GeoPoint> orderedPickups,
            PassengerGeocodingService.GeoPoint destination) {
        List<RouteLeg> legs = new ArrayList<>();
        double totalKm = 0;
        for (int index = 0; index < orderedPickups.size() - 1; index++) {
            PassengerGeocodingService.GeoPoint from = orderedPickups.get(index);
            PassengerGeocodingService.GeoPoint to = orderedPickups.get(index + 1);
            double distanceKm = straightLineDistanceKm(from, to) * 1.3;
            legs.add(new RouteLeg(distanceKm, estimatedMinutes(distanceKm)));
            totalKm += distanceKm;
        }
        double finalLegKm = straightLineDistanceKm(orderedPickups.get(orderedPickups.size() - 1), destination) * 1.3;
        legs.add(new RouteLeg(finalLegKm, estimatedMinutes(finalLegKm)));
        totalKm += finalLegKm;
        long totalMinutes = legs.stream().mapToLong(RouteLeg::durationMinutes).sum();
        return new RouteEstimate(totalKm, totalMinutes, legs);
    }

    private double straightLineDistanceKm(
            PassengerGeocodingService.GeoPoint from,
            PassengerGeocodingService.GeoPoint to) {
        double earthRadiusKm = 6371.0;
        double latitudeDelta = Math.toRadians(to.latitude() - from.latitude());
        double longitudeDelta = Math.toRadians(to.longitude() - from.longitude());
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(from.latitude())) * Math.cos(Math.toRadians(to.latitude()))
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private long estimatedMinutes(double distanceKm) {
        return Math.round(distanceKm / 30.0 * 60.0);
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

    public record RoutePlan(List<Integer> orderedStopIndexes, List<RouteLeg> legs, long durationMinutes) {
    }
}
