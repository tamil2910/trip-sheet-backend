package com.example.trip_sheet_backend.services.TripService;

import java.util.ArrayList;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripStopRequestDTO;
import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.TripStop;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.repositories.DutyTypeRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.TripRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeRepository;

@Service
public class TripServiceImp extends BaseServiceImp<Trip, UUID> implements TripService {
  private final TripRepository repository;
  private final TenantRepository tenantRepository;

    private final DutyTypeRepository dutyTypeRepository;
    private final VehicleTypeRepository vehicleTypeRepository;

    private final ModelMapper mapper;

  public TripServiceImp(TripRepository repository, TenantRepository tenantRepository, 
    DutyTypeRepository dutyTypeRepository, VehicleTypeRepository vehicleTypeRepository, ModelMapper mapper) {
    super(repository);
    this.repository = repository;
    this.tenantRepository = tenantRepository;
    this.dutyTypeRepository = dutyTypeRepository;
    this.vehicleTypeRepository = vehicleTypeRepository;
    this.mapper = mapper;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Trip createTrip(TripCreateRequestDTO createTripDto, Tenant tenant, UserAccount user, UUID createdBy) {

    Tenant organisation = tenantRepository.findById(
                UUID.fromString(createTripDto.getOrganisationId()))
            .orElseThrow(() -> new RuntimeException("Invalid organisation"));

    DutyType dutyType = dutyTypeRepository.findById(
          UUID.fromString(createTripDto.getDutyTypeId()))
      .orElseThrow(() -> new RuntimeException("Invalid duty type"));

    VehicleType vehicleType = vehicleTypeRepository.findById(
              UUID.fromString(createTripDto.getVehicleTypeId()))
          .orElseThrow(() -> new RuntimeException("Invalid vehicle type"));

    
      // Create Trip
    Trip trip = mapper.map(createTripDto, Trip.class);
    trip.setDutyType(dutyType);
    trip.setVehicleType(vehicleType);
    trip.setOrganisation(organisation);
    trip.setTenant(tenant);
    trip.setCreatedBy(createdBy.toString());
    trip.setTripStatus(Trip.TripStatus.CREATED);

    if ("VENDOR".equals(tenant.getTenantType().toString())) {
      trip.setVendor(tenant);
    }

    // if vendor_id is provided by organisation/corporate side, fetch vendor from db
    if(createTripDto.getVendorId() != null) {
      trip.setVendor(tenantRepository.findById(UUID.fromString(createTripDto.getVendorId()))
      .orElseThrow(() -> new RuntimeException("Invalid vendor")));
    }

    trip.setStops(new ArrayList<>());

    for (TripStopRequestDTO stopDto : createTripDto.getStops()) {
      TripStop stop = new TripStop();
      stop = mapper.map(stopDto, TripStop.class);
      stop.setTrip(trip); // VERY IMPORTANT

      trip.getStops().add(stop);
    }    

     return repository.save(trip);
  }



}
