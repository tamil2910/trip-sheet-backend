package com.example.trip_sheet_backend.services.TripChargesService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.TripChargesDtos.TripChargeItemDto;
import com.example.trip_sheet_backend.dtos.TripChargesDtos.TripChargesBulkCreateRequestDto;
import com.example.trip_sheet_backend.dtos.TripChargesDtos.TripChargesUpdateRequestDto;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.TripCharges;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.TripChargesRepository;
import com.example.trip_sheet_backend.repositories.TripRepository;
import com.example.trip_sheet_backend.repositories.DriverRepository;


@Service
public class TripChargeServiceImp implements TripChargeService {

  private final TripChargesRepository tripChargesRepository;
  private final TripRepository tripRepository;
  private final DriverRepository driverRepository;

  public TripChargeServiceImp(TripChargesRepository tripChargesRepository, TripRepository tripRepository, DriverRepository driverRepository) {
    this.tripChargesRepository = tripChargesRepository;
    this.tripRepository = tripRepository;
    this.driverRepository = driverRepository;

  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TripCharges createResource(UUID tenantId, TripCharges payload) {
    if (tenantId == null) {
      throw new RuntimeException("Tenant not found in token");
    }
    if (payload == null) {
      throw new RuntimeException("Trip charge payload is required");
    }

    Tenant tenant = new Tenant();
    tenant.setId(tenantId);
    payload.setTenant(tenant);

    return tripChargesRepository.save(payload);
  }

  @Override
  public TripCharges findByIdResource(UUID tenantId, UUID id) {
    if (tenantId == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    TripCharges tripCharge = tripChargesRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Trip charge not found"));

    if (tripCharge.getTenant() == null || !tenantId.equals(tripCharge.getTenant().getId())) {
      throw new RuntimeException("Trip charge not found");
    }

    return tripCharge;
  }

  @Override
  public Page<TripCharges> getAllResources(UUID tenantId, Pageable pageable) {
    if (tenantId == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    return tripChargesRepository.findAll(pageable);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TripCharges updateResource(UUID tenantId, UUID id, TripCharges payload) {
    TripCharges existingTripCharge = findByIdResource(tenantId, id);

    Trip existTrip = tripRepository.findById(UUID.fromString(payload.getTripId().toString()))
        .orElseThrow(() -> new RuntimeException("Trip not found"));

    if (existTrip.getTenant() != null && !tenantId.equals(existTrip.getTenant().getId())) {
      throw new RuntimeException("Unauthorized to update charge for this trip / Trip is belong to another tenant");
    }

    existingTripCharge.setTripId(payload.getTripId());
    existingTripCharge.setType(payload.getType());
    existingTripCharge.setReceiptImageUrl(payload.getReceiptImageUrl());
    existingTripCharge.setAmount(payload.getAmount());
    existingTripCharge.setDescription(payload.getDescription());

    return tripChargesRepository.save(existingTripCharge);
  }

  @Override
  public void deleteResource(UUID tenantId, UUID id) {
    findByIdResource(tenantId, id);
    tripChargesRepository.deleteById(id);
  }

  @Override
  public Page<TripCharges> searchResources(UUID tenantId, Map<String, Object> filters, Pageable pageable) {
    if (tenantId == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    return tripChargesRepository.findAll(pageable);
  }

  @Override
  public List<TripCharges> findByTripId_Id(UUID tripId) {
    return tripChargesRepository.findByTripId_Id(tripId);
  }

  @Transactional(rollbackFor = Exception.class)
  public TripCharges createTripCharges(UUID tenantId, TripChargesBulkCreateRequestDto payload, UserAccount user) {
    if (tenantId == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    UUID tripId = UUID.fromString(payload.getTripId());

    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new RuntimeException("Trip not found"));

    List<TripChargeItemDto> chargeItems = payload.getCharges();
    if (chargeItems == null || chargeItems.isEmpty()) {
      throw new RuntimeException("At least one charge item is required");
    }

    Tenant tenant = new Tenant();
    if(user != null && "DRIVER".equals(user.getRole().getName())) {
      Optional<Driver> driver = driverRepository.findByAccount_Id(user.getId());
      // .orElseThrow(() -> new RuntimeException("Driver not found for the user"));

      UUID driverId = driver.map(Driver::getId).orElseThrow(() -> new RuntimeException("Driver not found"));

      if (driverId != trip.getDriver().getId()) {
        throw new RuntimeException("Unauthorized Driver to create charges for this trip");
      }

      tenant.setId(trip.getTenant().getId());
    } else {

      if (tenantId != trip.getTenant().getId()) {
        throw new RuntimeException("Unauthorized Vendor/Organisation to create charges for this trip");
      }

      tenant.setId(tenantId);
    }


    List<TripCharges> chargesToSave = new ArrayList<>(chargeItems.size());
    for (TripChargeItemDto chargeItem : chargeItems) {
      TripCharges tripCharge = new TripCharges();
      tripCharge.setTripId(trip);
      tripCharge.setTenant(tenant);
      tripCharge.setType(parseChargeType(chargeItem.getType()));
      tripCharge.setReceiptImageUrl(chargeItem.getReceiptImageUrl());
      tripCharge.setAmount(chargeItem.getAmount());
      tripCharge.setDescription(chargeItem.getDescription());
      tripCharge.setCreatedBy(user != null ? user.getId().toString() : null);
      tripCharge.setUpdatedBy(user != null ? user.getId().toString() : null);
      chargesToSave.add(tripCharge);
    }

    return tripChargesRepository.saveAll(chargesToSave).iterator().next();
  }

  public TripCharges.chargeType parseChargeType(String chargeItemType) {
    if (chargeItemType == null || chargeItemType.isBlank()) {
      throw new RuntimeException("Charge type is required");
    }

    String normalizedType = chargeItemType.trim().toLowerCase();
    String enumName = Character.toUpperCase(normalizedType.charAt(0)) + normalizedType.substring(1);
    return TripCharges.chargeType.valueOf(enumName);
  }

  public List<TripCharges> getTripChargesByTripId(UUID tenantId, UUID tripId) {
    if (tenantId == null) {
      throw new RuntimeException("Tenant not found in token");
    }
    return tripChargesRepository.findByTripId_IdAndTenant_Id(tripId, tenantId); 
  }



}
