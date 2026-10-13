package com.example.trip_sheet_backend.services.TripService;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.trip_sheet_backend.dtos.TripDtos.TripLocationRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripLocationResponseDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.TripLocationUpdate;
import com.example.trip_sheet_backend.repositories.TripLocationUpdateRepository;
import com.example.trip_sheet_backend.repositories.TripRepository;
import com.example.trip_sheet_backend.security.JwtTokenUtil;

@Service
public class TripLocationService {
  private final TripRepository tripRepository;
  private final TripLocationUpdateRepository locationRepository;
  private final JwtTokenUtil jwtTokenUtil;
  private final SimpMessagingTemplate messagingTemplate;

  public TripLocationService(TripRepository tripRepository, TripLocationUpdateRepository locationRepository,
      JwtTokenUtil jwtTokenUtil, SimpMessagingTemplate messagingTemplate) {
    this.tripRepository = tripRepository;
    this.locationRepository = locationRepository;
    this.jwtTokenUtil = jwtTokenUtil;
    this.messagingTemplate = messagingTemplate;
  }

  @Transactional
  public TripLocationResponseDTO record(UUID tripId, String trackingToken, TripLocationRequestDTO request) {
    if (trackingToken == null || trackingToken.isBlank()
        || !"trip_tracking".equals(jwtTokenUtil.getTokenType(trackingToken))) {
      throw new IllegalArgumentException("A valid trip tracking token is required");
    }
    if (!tripId.toString().equals(jwtTokenUtil.getTripIdFromToken(trackingToken))) {
      throw new IllegalArgumentException("Tracking token does not match this trip");
    }

    UUID driverId = UUID.fromString(jwtTokenUtil.getTripTrackingDriverIdFromToken(trackingToken));
    Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new IllegalArgumentException("Trip not found"));
    if (Boolean.TRUE.equals(trip.getIsDeleted()) || trip.getDriver() == null
        || !driverId.equals(trip.getDriver().getId())) {
      throw new IllegalArgumentException("Tracking token is no longer valid for this trip");
    }
    if (trip.getTripStatus() != Trip.TripStatus.DISPATCHED
        && trip.getTripStatus() != Trip.TripStatus.ARRIVED
        && trip.getTripStatus() != Trip.TripStatus.STARTED) {
      throw new IllegalArgumentException("Location updates are only accepted while a trip is active");
    }

    long receivedAt = Instant.now().toEpochMilli();
    TripLocationUpdate update = new TripLocationUpdate();
    update.setTrip(trip);
    update.setLatitude(request.getLatitude());
    update.setLongitude(request.getLongitude());
    update.setRecordedAt(request.getRecordedAt());
    update.setAccuracyMeters(request.getAccuracyMeters());
    update.setSpeedMetersPerSecond(request.getSpeedMetersPerSecond());
    update.setReceivedAt(receivedAt);
    TripLocationUpdate saved = locationRepository.save(update);
    TripLocationResponseDTO payload = TripLocationResponseDTO.from(saved);

    Runnable publish = () -> {
      for (UUID tenantId : audienceTenantIds(trip)) {
        messagingTemplate.convertAndSend("/topic/trips/" + tenantId + "/location/" + tripId, payload);
      }
    };
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override public void afterCommit() { publish.run(); }
      });
    } else {
      publish.run();
    }
    return payload;
  }

  @Transactional(readOnly = true)
  public TripLocationResponseDTO latest(UUID tripId, UUID tenantId) {
    Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new IllegalArgumentException("Trip not found"));
    if (Boolean.TRUE.equals(trip.getIsDeleted()) || !visibleToTenant(trip, tenantId)) {
      throw new IllegalArgumentException("Trip not found or not accessible");
    }
    return locationRepository.findFirstByTrip_IdOrderByRecordedAtDesc(tripId)
        .map(TripLocationResponseDTO::from).orElse(null);
  }

  private boolean visibleToTenant(Trip trip, UUID tenantId) {
    if (tenantId == null) return true;
    return matches(trip.getTenant(), tenantId) || matches(trip.getOrganisation(), tenantId)
        || matches(trip.getVendor(), tenantId) || matches(trip.getAssignedByVendor(), tenantId)
        || matches(trip.getPreviousVendor(), tenantId) || matches(trip.getClientVendor(), tenantId);
  }

  private boolean matches(Tenant tenant, UUID id) {
    return tenant != null && id.equals(tenant.getId());
  }

  private Set<UUID> audienceTenantIds(Trip trip) {
    Set<UUID> ids = new LinkedHashSet<>();
    add(ids, trip.getTenant()); add(ids, trip.getOrganisation()); add(ids, trip.getVendor());
    add(ids, trip.getAssignedByVendor()); add(ids, trip.getPreviousVendor()); add(ids, trip.getClientVendor());
    return ids;
  }

  private void add(Set<UUID> ids, Tenant tenant) {
    if (tenant != null && tenant.getId() != null) ids.add(tenant.getId());
  }
}
