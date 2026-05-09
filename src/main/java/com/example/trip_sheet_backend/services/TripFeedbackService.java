package com.example.trip_sheet_backend.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.TripDtos.TripFeedbackRequestDTO;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.TripFeedback;
import com.example.trip_sheet_backend.repositories.PeopleTenantRepository;
import com.example.trip_sheet_backend.repositories.TripFeedbackRepository;
import com.example.trip_sheet_backend.repositories.TripRepository;
import com.example.trip_sheet_backend.security.JwtTokenUtil;

@Service
public class TripFeedbackService {

    private static final Logger log = LoggerFactory.getLogger(TripFeedbackService.class);

    private final TripRepository tripRepository;
    private final PeopleTenantRepository peopleTenantRepository;
    private final TripFeedbackRepository tripFeedbackRepository;
    private final EmailService emailService;
    private final JwtTokenUtil jwtTokenUtil;
    private final String feedbackLinkBaseUrl;

    public TripFeedbackService(
            TripRepository tripRepository,
            PeopleTenantRepository peopleTenantRepository,
            TripFeedbackRepository tripFeedbackRepository,
            EmailService emailService,
            JwtTokenUtil jwtTokenUtil,
            @Value("${app.feedback.link-base:http://localhost:4200/feedback}") String feedbackLinkBaseUrl) {
        this.tripRepository = tripRepository;
        this.peopleTenantRepository = peopleTenantRepository;
        this.tripFeedbackRepository = tripFeedbackRepository;
        this.emailService = emailService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.feedbackLinkBaseUrl = feedbackLinkBaseUrl;
    }

    public void sendFeedbackRequestsForTrip(Trip trip) {
        if (trip == null) {
            return;
        }

        List<PeopleTenant> passengers = trip.getPassengers();
        if (passengers == null || passengers.isEmpty()) {
            return;
        }

        UUID executedVendorId = trip.getVendor() != null ? trip.getVendor().getId() : null;
        UUID originVendorId = resolveOriginVendorId(trip);
        UUID organisationId = trip.getOrganisation() != null ? trip.getOrganisation().getId() : null;

        for (PeopleTenant passenger : passengers) {
            if (passenger == null || passenger.getId() == null) {
                continue;
            }

            if (passenger.getEmail() == null || passenger.getEmail().isBlank()) {
                log.warn("Skipping feedback email for passenger {} because email is missing", passenger.getId());
                continue;
            }

            try {
                String feedbackToken = jwtTokenUtil.generateTripFeedbackToken(
                        trip.getId(),
                        passenger.getId(),
                        executedVendorId,
                        originVendorId,
                        organisationId);
                String feedbackLink = buildFeedbackLink(feedbackToken);
                emailService.sendTripFeedbackEmail(passenger.getEmail(), passenger.getName(), feedbackLink);
            } catch (RuntimeException ex) {
                log.warn("Failed to send feedback email for trip {} passenger {}: {}", trip.getId(), passenger.getId(), ex.getMessage());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Optional<TripFeedback> submitFeedback(String feedbackToken, TripFeedbackRequestDTO request) {
        if (feedbackToken == null || feedbackToken.isBlank()) {
            throw new RuntimeException("Feedback token is required");
        }

        if (!jwtTokenUtil.validateToken(feedbackToken)) {
            throw new RuntimeException("Invalid or expired feedback token");
        }

        UUID tripId = parseUuid(jwtTokenUtil.getTripIdFromToken(feedbackToken), "trip_id");
        UUID passengerId = parseUuid(jwtTokenUtil.getPassengerIdFromToken(feedbackToken), "passenger_id");
        UUID executedVendorId = parseUuid(jwtTokenUtil.getExecutedVendorIdFromToken(feedbackToken), "executed_vendor_id");
        UUID originVendorId = parseUuid(jwtTokenUtil.getOriginVendorIdFromToken(feedbackToken), "origin_vendor_id");
        UUID organisationId = parseUuid(jwtTokenUtil.getOrganisationIdFromToken(feedbackToken), "organisation_id");

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        PeopleTenant passenger = peopleTenantRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found"));

        if (trip.getPassengers() == null || trip.getPassengers().stream().noneMatch(item -> passengerId.equals(item.getId()))) {
            throw new RuntimeException("Passenger does not belong to this trip");
        }

        UUID actualExecutedVendorId = trip.getVendor() != null ? trip.getVendor().getId() : null;
        UUID actualOriginVendorId = resolveOriginVendorId(trip);
        UUID actualOrganisationId = trip.getOrganisation() != null ? trip.getOrganisation().getId() : null;

        if (!Objects.equals(executedVendorId, actualExecutedVendorId)
                || !Objects.equals(originVendorId, actualOriginVendorId)
                || !Objects.equals(organisationId, actualOrganisationId)) {
            throw new RuntimeException("Feedback token does not match this trip");
        }

        if (tripFeedbackRepository.existsByTrip_IdAndPassenger_Id(tripId, passengerId)) {
            return Optional.empty();
        }

        TripFeedback feedback = new TripFeedback();
        feedback.setTrip(trip);
        feedback.setPassenger(passenger);
        feedback.setDriverBehaviourRating(request.getDriverBehaviourRating());
        feedback.setDriverCleanlinessRating(request.getDriverCleanlinessRating());
        feedback.setVehicleConditionRating(request.getVehicleConditionRating());
        feedback.setVehicleCleanlinessRating(request.getVehicleCleanlinessRating());
        feedback.setOverallExperienceRating(request.getOverallExperienceRating());
        feedback.setExecutedVendorId(executedVendorId);
        feedback.setOriginVendorId(originVendorId);
        feedback.setOrganisationId(organisationId);

        return Optional.of(tripFeedbackRepository.save(feedback));
    }

    private String buildFeedbackLink(String feedbackToken) {
        String baseUrl = feedbackLinkBaseUrl == null || feedbackLinkBaseUrl.isBlank()
                ? "http://localhost:4200/feedback"
                : feedbackLinkBaseUrl;

        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "token=" + URLEncoder.encode(feedbackToken, StandardCharsets.UTF_8);
    }

    private UUID resolveOriginVendorId(Trip trip) {
        if (trip == null) {
            return null;
        }

        if (trip.getTenant() != null && trip.getTenant().getTenantType() == com.example.trip_sheet_backend.models.Tenant.TenantType.VENDOR) {
            return trip.getTenant().getId();
        }

        return trip.getAssignedByVendor() != null ? trip.getAssignedByVendor().getId() : null;
    }

    private UUID parseUuid(String value, String claimName) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(claimName + " is missing from feedback token");
        }

        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            throw new RuntimeException("Invalid " + claimName + " in feedback token");
        }
    }
}