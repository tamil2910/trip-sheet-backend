package com.example.trip_sheet_backend.services.TripChargesService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
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
public class TripChargeService {

  private final TripChargesRepository tripChargesRepository;
  private final TripRepository tripRepository;
  private final DriverRepository driverRepository;

  public TripChargeService(TripChargesRepository tripChargesRepository, TripRepository tripRepository, DriverRepository driverRepository) {
    this.tripChargesRepository = tripChargesRepository;
    this.tripRepository = tripRepository;
    this.driverRepository = driverRepository;

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
      tripCharge.setTrip(trip);
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
    return tripChargesRepository.findByTrip_IdAndTenant_Id(tripId, tenantId); 
  }

  @Transactional(rollbackFor = Exception.class) 
  public TripCharges updateTripCharge(UUID tenantId, UUID id, TripChargesUpdateRequestDto payload, UserAccount user) {
    if (tenantId == null) {
      throw new RuntimeException("No Tenant Found for this user");
    }

    TripCharges existingTripCharge = tripChargesRepository.findById(id).orElseThrow(
      () -> new RuntimeException("TripCharge resource can not be found!"));

    // if(payload.getAmount() != null) {
    //   existingTripCharge.setAmount(payload.getAmount());
    // }

    // if(payload.getDescription() != null) {
    //   existingTripCharge.setDescription(payload.getDescription());
    // }

    // if(payload.getReceiptImageUrl() != null) {
    //   existingTripCharge.setReceiptImageUrl(payload.getReceiptImageUrl());
    // }

    // if (payload.getType() != null) {
    //   existingTripCharge.setType(parseChargeType(payload.getType()));
    // }

    ModelMapper mapper = new ModelMapper();
    mapper.getConfiguration().setSkipNullEnabled(true); // only maps non-null fields from payload to existingTripCharge
    mapper.map(payload, existingTripCharge);

    // existingTripCharge.setTrip(trip);

    return tripChargesRepository.saveAndFlush(existingTripCharge);
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteTripCharge(UUID tenantId, UUID id) {
    if (tenantId == null) { 
       throw new RuntimeException("No Tenant Found for this user");
    }

    tripChargesRepository.findById(id).orElseThrow(() -> new RuntimeException("TripCharge resource can not be found!"));

    tripChargesRepository.deleteById(id);
  }

}
