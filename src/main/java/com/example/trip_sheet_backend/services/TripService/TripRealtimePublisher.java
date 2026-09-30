package com.example.trip_sheet_backend.services.TripService;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.trip_sheet_backend.dtos.TripDtos.TripRealtimeEventDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripResponseDTO;
import com.example.trip_sheet_backend.mappers.TripResponseMapper;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.repositories.TripRepository;

@Service
public class TripRealtimePublisher {

  private static final Logger log = LoggerFactory.getLogger(TripRealtimePublisher.class);

  private final SimpMessagingTemplate messagingTemplate;
  private final TripRepository tripRepository;
  private final RedisTemplate<String, String> redisTemplate;

  public TripRealtimePublisher(SimpMessagingTemplate messagingTemplate, TripRepository tripRepository,
      RedisTemplate<String, String> redisTemplate) {
    this.messagingTemplate = messagingTemplate;
    this.tripRepository = tripRepository;
    this.redisTemplate = redisTemplate;
  }

  public void publishCreated(Trip trip) {
    publishAfterCommit(trip, TripRealtimeEventDTO.TripRealtimeEventType.CREATED, true);
  }

  public void publishUpdated(Trip trip) {
    publishAfterCommit(trip, TripRealtimeEventDTO.TripRealtimeEventType.UPDATED, true);
  }

  @Transactional(readOnly = true)
  public void publishUpdatedByTripId(UUID tripId) {
    if (tripId == null) {
      return;
    }

    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new RuntimeException("Trip not found for realtime publication"));
    // This method owns the read-only transaction, so publish while the lazy
    // trip relations are still attached to the persistence context.
    publishNow(trip, TripRealtimeEventDTO.TripRealtimeEventType.UPDATED, true);
  }

  public void publishDeleted(Trip trip) {
    publishAfterCommit(trip, TripRealtimeEventDTO.TripRealtimeEventType.DELETED, false);
  }

  private void publishAfterCommit(
      Trip trip,
      TripRealtimeEventDTO.TripRealtimeEventType eventType,
      boolean includeTripBody
  ) {
    Runnable publishAction = () -> publishNow(trip, eventType, includeTripBody);

    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          publishAction.run();
        }
      });
      return;
    }

    publishAction.run();
  }

  private void publishNow(
      Trip trip,
      TripRealtimeEventDTO.TripRealtimeEventType eventType,
      boolean includeTripBody
  ) {
    if (trip == null || trip.getId() == null) {
      return;
    }

    invalidateTripSearchCaches(trip);

    TripResponseDTO tripBody = includeTripBody ? TripResponseMapper.toDTO(trip) : null;
    TripRealtimeEventDTO payload = new TripRealtimeEventDTO(
        eventType,
        trip.getId().toString(),
        tripBody,
        Instant.now().toEpochMilli());

    for (UUID tenantId : resolveAudienceTenantIds(trip)) {
      messagingTemplate.convertAndSend("/topic/trips/" + tenantId, payload);
    }
  }

  private void invalidateTripSearchCaches(Trip trip) {
    for (UUID tenantId : resolveAudienceTenantIds(trip)) {
      try {
        redisTemplate.opsForValue().increment("trip-list:version:" + tenantId);
      } catch (DataAccessException ex) {
        log.warn("Unable to invalidate trip search cache for tenant {}", tenantId, ex);
      }
    }
  }

  private Set<UUID> resolveAudienceTenantIds(Trip trip) {
    Set<UUID> tenantIds = new LinkedHashSet<>();
    addTenantId(tenantIds, trip.getTenant());
    addTenantId(tenantIds, trip.getOrganisation());
    addTenantId(tenantIds, trip.getVendor());
    addTenantId(tenantIds, trip.getAssignedByVendor());
    addTenantId(tenantIds, trip.getPreviousVendor());
    return tenantIds;
  }

  private void addTenantId(Set<UUID> tenantIds, Tenant tenant) {
    if (tenant != null && tenant.getId() != null) {
      tenantIds.add(tenant.getId());
    }
  }
}
