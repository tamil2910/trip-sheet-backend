package com.example.trip_sheet_backend.services.TripService;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.trip_sheet_backend.dtos.TripDtos.TripRealtimeEventDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripResponseDTO;
import com.example.trip_sheet_backend.mappers.TripResponseMapper;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;

@Service
public class TripRealtimePublisher {

  private final SimpMessagingTemplate messagingTemplate;

  public TripRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  public void publishCreated(Trip trip) {
    publishAfterCommit(trip, TripRealtimeEventDTO.TripRealtimeEventType.CREATED, true);
  }

  public void publishUpdated(Trip trip) {
    publishAfterCommit(trip, TripRealtimeEventDTO.TripRealtimeEventType.UPDATED, true);
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
