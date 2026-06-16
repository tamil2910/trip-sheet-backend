package com.example.trip_sheet_backend.services.TripBillingAllocationService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.TripBillingAllocationDtos.TripBillingAllocationCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripBillingAllocationDtos.TripBillingAllocationUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.TripBillingAllocation;
import com.example.trip_sheet_backend.repositories.TripBillingAllocationRepository;
import com.example.trip_sheet_backend.repositories.TripRepository;

@Service
public class TripBillingAllocationServiceImp implements TripBillingAllocationService {

  private final TripBillingAllocationRepository tripBillingAllocationRepository;
  private final TripRepository tripRepository;

  public TripBillingAllocationServiceImp(
      TripBillingAllocationRepository tripBillingAllocationRepository,
      TripRepository tripRepository
  ) {
    this.tripBillingAllocationRepository = tripBillingAllocationRepository;
    this.tripRepository = tripRepository;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TripBillingAllocation createAllocation(TripBillingAllocationCreateRequestDTO body, Tenant tokenTenant, UUID createdBy) {
    validateTenant(tokenTenant);

    Trip trip = findAccessibleTrip(body.getTripId(), tokenTenant);
    validateAmounts(body.getSharePercent(), body.getShareAmount());

    TripBillingAllocation allocation = new TripBillingAllocation();
    allocation.setTrip(trip);
    allocation.setTenant(resolveAllocationTenant(trip));
    applyAllocationFields(allocation, body.getAllocationType(), body.getAllocationKey(), body.getSharePercent(), body.getShareAmount(), body.getStatus());
    if (createdBy != null) {
      allocation.setCreatedBy(createdBy.toString());
      allocation.setUpdatedBy(createdBy.toString());
    }

    return tripBillingAllocationRepository.save(allocation);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TripBillingAllocation> getAllocationsByTrip(UUID tripId, Tenant tokenTenant) {
    validateTenant(tokenTenant);
    findAccessibleTrip(tripId, tokenTenant);
    return tripBillingAllocationRepository.findByTrip_IdAndIsDeletedFalse(tripId);
  }

  @Override
  @Transactional(readOnly = true)
  public TripBillingAllocation getAllocationById(UUID allocationId, Tenant tokenTenant) {
    validateTenant(tokenTenant);
    TripBillingAllocation allocation = tripBillingAllocationRepository.findByIdAndIsDeletedFalse(allocationId)
        .orElseThrow(() -> new RuntimeException("Trip billing allocation not found"));
    validateTripVisibility(allocation.getTrip(), tokenTenant);
    return allocation;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TripBillingAllocation updateAllocation(UUID allocationId, TripBillingAllocationUpdateRequestDTO body, Tenant tokenTenant, UUID updatedBy) {
    validateTenant(tokenTenant);

    TripBillingAllocation existingAllocation = tripBillingAllocationRepository.findByIdAndIsDeletedFalse(allocationId)
        .orElseThrow(() -> new RuntimeException("Trip billing allocation not found"));

    Trip trip = findAccessibleTrip(body.getTripId(), tokenTenant);
    validateAmounts(body.getSharePercent(), body.getShareAmount());

    existingAllocation.setTrip(trip);
    existingAllocation.setTenant(resolveAllocationTenant(trip));
    applyAllocationFields(
        existingAllocation,
        body.getAllocationType(),
        body.getAllocationKey(),
        body.getSharePercent(),
        body.getShareAmount(),
        body.getStatus()
    );
    if (updatedBy != null) {
      existingAllocation.setUpdatedBy(updatedBy.toString());
    }

    return tripBillingAllocationRepository.save(existingAllocation);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteAllocation(UUID allocationId, Tenant tokenTenant, UUID deletedBy) {
    validateTenant(tokenTenant);

    TripBillingAllocation allocation = tripBillingAllocationRepository.findByIdAndIsDeletedFalse(allocationId)
        .orElseThrow(() -> new RuntimeException("Trip billing allocation not found"));
    validateTripVisibility(allocation.getTrip(), tokenTenant);

    allocation.setIsDeleted(true);
    allocation.setDeletedAt(System.currentTimeMillis());
    if (deletedBy != null) {
      allocation.setDeletedBy(deletedBy.toString());
      allocation.setUpdatedBy(deletedBy.toString());
    }
    tripBillingAllocationRepository.save(allocation);
  }

  private void applyAllocationFields(
      TripBillingAllocation allocation,
      TripBillingAllocation.AllocationType allocationType,
      String allocationKey,
      BigDecimal sharePercent,
      BigDecimal shareAmount,
      TripBillingAllocation.AllocationStatus status
  ) {
    allocation.setAllocationType(allocationType);
    allocation.setAllocationKey(allocationKey == null ? null : allocationKey.trim());
    allocation.setSharePercent(sharePercent);
    allocation.setShareAmount(shareAmount);
    allocation.setStatus(status);
  }

  private void validateAmounts(BigDecimal sharePercent, BigDecimal shareAmount) {
    if (sharePercent == null || sharePercent.compareTo(BigDecimal.ZERO) < 0) {
      throw new RuntimeException("sharePercent cannot be negative");
    }
    if (shareAmount == null || shareAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new RuntimeException("shareAmount cannot be negative");
    }
  }

  private Trip findAccessibleTrip(UUID tripId, Tenant tokenTenant) {
    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new RuntimeException("Trip not found"));

    if (Boolean.TRUE.equals(trip.getIsDeleted())) {
      throw new RuntimeException("Trip not found");
    }

    validateTripVisibility(trip, tokenTenant);
    return trip;
  }

  private void validateTripVisibility(Trip trip, Tenant tokenTenant) {
    if (trip == null || tokenTenant == null || tokenTenant.getId() == null) {
      throw new RuntimeException("Trip is not accessible for this tenant");
    }

    UUID tokenTenantId = tokenTenant.getId();
    boolean visible = Objects.equals(tokenTenantId, trip.getTenant() == null ? null : trip.getTenant().getId())
        || Objects.equals(tokenTenantId, trip.getOrganisation() == null ? null : trip.getOrganisation().getId())
        || Objects.equals(tokenTenantId, trip.getVendor() == null ? null : trip.getVendor().getId())
        || Objects.equals(tokenTenantId, trip.getAssignedByVendor() == null ? null : trip.getAssignedByVendor().getId())
        || Objects.equals(tokenTenantId, trip.getPreviousVendor() == null ? null : trip.getPreviousVendor().getId());

    if (!visible) {
      throw new RuntimeException("Trip is not accessible for this tenant");
    }
  }

  private Tenant resolveAllocationTenant(Trip trip) {
    if (trip.getOrganisation() != null) {
      return trip.getOrganisation();
    }
    if (trip.getTenant() != null) {
      return trip.getTenant();
    }
    throw new RuntimeException("Unable to resolve tenant for trip billing allocation");
  }

  private void validateTenant(Tenant tokenTenant) {
    if (tokenTenant == null || tokenTenant.getId() == null) {
      throw new RuntimeException("Tenant not found in token");
    }
  }
}
