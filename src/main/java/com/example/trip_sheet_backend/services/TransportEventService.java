package com.example.trip_sheet_backend.services;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.EventDtos.EventCreateRequest;
import com.example.trip_sheet_backend.dtos.EventDtos.EventResponse;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.TransportEvent;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.repositories.TransportEventRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeRepository;

@Service
public class TransportEventService {
  private final TransportEventRepository eventRepository;
  private final VehicleTypeRepository vehicleTypeRepository;

  public TransportEventService(TransportEventRepository eventRepository, VehicleTypeRepository vehicleTypeRepository) {
    this.eventRepository = eventRepository;
    this.vehicleTypeRepository = vehicleTypeRepository;
  }

  @Transactional
  public EventResponse create(EventCreateRequest request, Tenant tenant, UUID createdBy) {
    if (tenant == null || tenant.getId() == null) throw new IllegalArgumentException("Tenant not found in token");
    validateSchedule(request);
    List<VehicleType> vehicleTypes = resolveVehicleTypes(request.getVehicleTypeIds());

    TransportEvent event = new TransportEvent();
    event.setName(request.getName().trim());
    event.setVenue(request.getVenue().trim());
    event.setCity(trimToNull(request.getCity()));
    event.setDescription(trimToNull(request.getDescription()));
    validateVenueCoordinates(request.getVenueLatitude(), request.getVenueLongitude());
    event.setVenueLatitude(request.getVenueLatitude());
    event.setVenueLongitude(request.getVenueLongitude());
    event.setStartsAt(request.getStartsAt());
    event.setEndsAt(request.getEndsAt());
    event.setLinkValidUntil(request.getLinkValidUntil());
    event.setRequestAudience(request.getRequestAudience());
    event.setTenant(tenant);
    event.setVehicleTypes(vehicleTypes);
    event.setCreatedBy(createdBy == null ? null : createdBy.toString());
    event.setUpdatedBy(createdBy == null ? null : createdBy.toString());
    return EventResponse.from(eventRepository.save(event));
  }

  @Transactional(readOnly = true)
  public Page<EventResponse> list(Tenant tenant, Pageable pageable) {
    if (tenant == null || tenant.getId() == null) throw new IllegalArgumentException("Tenant not found in token");
    List<TransportEvent> all = eventRepository.findAllByTenant_IdAndIsDeletedFalseOrderByStartsAtAsc(tenant.getId());
    int page = pageable == null ? 0 : pageable.getPageNumber();
    int size = pageable == null || pageable.isUnpaged() ? Math.max(all.size(), 1) : pageable.getPageSize();
    int from = Math.min(page * size, all.size());
    int to = Math.min(from + size, all.size());
    return new PageImpl<>(all.subList(from, to).stream().map(EventResponse::from).toList(),
        pageable == null ? Pageable.unpaged() : pageable, all.size());
  }

  @Transactional
  public EventResponse updateVehicleTypes(UUID id, List<UUID> vehicleTypeIds, Tenant tenant, UUID updatedBy) {
    if (tenant == null || tenant.getId() == null) throw new IllegalArgumentException("Tenant not found in token");
    TransportEvent event = eventRepository.findByIdAndTenant_IdAndIsDeletedFalse(id, tenant.getId())
        .orElseThrow(() -> new IllegalArgumentException("Event not found"));
    event.setVehicleTypes(resolveVehicleTypes(vehicleTypeIds));
    event.setUpdatedBy(updatedBy == null ? null : updatedBy.toString());
    return EventResponse.from(eventRepository.save(event));
  }

  @Transactional(readOnly = true)
  public EventResponse get(UUID id, Tenant tenant) {
    if (tenant == null || tenant.getId() == null) throw new IllegalArgumentException("Tenant not found in token");
    return eventRepository.findByIdAndTenant_IdAndIsDeletedFalse(id, tenant.getId())
        .map(EventResponse::from)
        .orElseThrow(() -> new IllegalArgumentException("Event not found"));
  }

  @Transactional(readOnly = true)
  public List<EventResponse.VehicleTypeOption> vehicleTypeOptions() {
    return vehicleTypeRepository.findAll().stream()
        .filter(v -> !Boolean.TRUE.equals(v.getIsDeleted()))
        .map(EventResponse.VehicleTypeOption::from)
        .toList();
  }

  private void validateSchedule(EventCreateRequest request) {
    if (request.getStartsAt() == null || request.getEndsAt() == null || request.getLinkValidUntil() == null) {
      throw new IllegalArgumentException("Event start, end, and link validity are required");
    }
    long now = Instant.now().toEpochMilli();
    if (request.getStartsAt() >= request.getEndsAt()) throw new IllegalArgumentException("Event end must be after its start");
    if (request.getLinkValidUntil() <= now) throw new IllegalArgumentException("Link validity must be in the future");
    if (request.getLinkValidUntil() < request.getStartsAt()) throw new IllegalArgumentException("Link must remain valid through the event start");
    if (request.getRequestAudience() == null) throw new IllegalArgumentException("Request audience is required");
  }

  private void validateVenueCoordinates(Double latitude, Double longitude) {
    if ((latitude == null) != (longitude == null)) {
      throw new IllegalArgumentException("Venue latitude and longitude must be provided together");
    }
    if (latitude != null && (latitude < -90 || latitude > 90)) {
      throw new IllegalArgumentException("Venue latitude must be between -90 and 90");
    }
    if (longitude != null && (longitude < -180 || longitude > 180)) {
      throw new IllegalArgumentException("Venue longitude must be between -180 and 180");
    }
  }

  private List<VehicleType> resolveVehicleTypes(List<UUID> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    if (new HashSet<>(ids).size() != ids.size()) throw new IllegalArgumentException("Duplicate vehicle type ids are not allowed");
    List<VehicleType> result = vehicleTypeRepository.findAllById(ids);
    Set<UUID> found = new HashSet<>();
    result.forEach(type -> found.add(type.getId()));
    if (found.size() != ids.size()) throw new IllegalArgumentException("One or more vehicle types were not found");
    if (result.stream().anyMatch(type -> Boolean.TRUE.equals(type.getIsDeleted()))) {
      throw new IllegalArgumentException("Deleted vehicle types cannot be selected");
    }
    return result;
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}



