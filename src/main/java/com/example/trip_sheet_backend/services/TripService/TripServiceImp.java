package com.example.trip_sheet_backend.services.TripService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripStopRequestDTO;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.TripStop;
// import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.Vehicle;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.repositories.DriverRepository;
import com.example.trip_sheet_backend.repositories.DutyTypeRepository;
import com.example.trip_sheet_backend.repositories.PeopleTenantRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.TripRepository;
import com.example.trip_sheet_backend.repositories.VehicleRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeRepository;

@Service
public class TripServiceImp extends BaseServiceImp<Trip, UUID> implements TripService {
  private final TripRepository repository;
  private final TenantRepository tenantRepository;

    private final DutyTypeRepository dutyTypeRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final PeopleTenantRepository peopleTenantRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;


    private final ModelMapper mapper;

  public TripServiceImp(TripRepository repository, TenantRepository tenantRepository, 
    DutyTypeRepository dutyTypeRepository, VehicleTypeRepository vehicleTypeRepository, 
    PeopleTenantRepository peopleTenantRepository, ModelMapper mapper, DriverRepository driverRepository,
      VehicleRepository vehicleRepository) {
    super(repository);
    this.repository = repository;
    this.tenantRepository = tenantRepository;
    this.dutyTypeRepository = dutyTypeRepository;
    this.vehicleTypeRepository = vehicleTypeRepository;
    this.mapper = mapper;
    this.peopleTenantRepository = peopleTenantRepository;
    this.driverRepository = driverRepository;
    this.vehicleRepository = vehicleRepository;
  }

@Override
@Transactional(rollbackFor = Exception.class)
public Trip createTrip(TripCreateRequestDTO createTripDto, Tenant tenant, UUID createdBy) {
  System.out.println("---- DEBUG TRIP CREATE ----");
  Tenant organisation = tenantRepository.findById(
          UUID.fromString(createTripDto.getOrganisationId()))
      .orElseThrow(() -> new RuntimeException("Invalid organisation"));

  DutyType dutyType = dutyTypeRepository.findById(
          UUID.fromString(createTripDto.getDutyTypeId()))
      .orElseThrow(() -> new RuntimeException("Invalid duty type"));

  VehicleType vehicleType = vehicleTypeRepository.findById(
          UUID.fromString(createTripDto.getVehicleTypeId()))
      .orElseThrow(() -> new RuntimeException("Invalid vehicle type"));

  Driver driver = null;
  if (createTripDto.getDriverId() != null && !createTripDto.getDriverId().isBlank()) {
    driver = driverRepository.findById(UUID.fromString(createTripDto.getDriverId()))
        .orElseThrow(() -> new RuntimeException("Invalid driver"));
  }

  Vehicle vehicle = null;
  if (createTripDto.getVehicleId() != null && !createTripDto.getVehicleId().isBlank()) {
    vehicle = vehicleRepository.findById(UUID.fromString(createTripDto.getVehicleId()))
        .orElseThrow(() -> new RuntimeException("Invalid vehicle"));
  }

  // ✅ Create Trip manually (avoid mapper poisoning)
  Trip trip = new Trip();
  trip.setTripCode(createTripDto.getTripCode());
  trip.setTripType(createTripDto.getTripType());
  trip.setOrganisation(organisation);
  trip.setTenant(tenant);
  trip.setDutyType(dutyType);
  trip.setVehicleType(vehicleType);
  trip.setDriver(driver);
  trip.setVehicle(vehicle);

  if (createTripDto.getParentTripId() != null && !createTripDto.getParentTripId().isBlank()) {
    Trip parentTrip = repository.findById(UUID.fromString(createTripDto.getParentTripId()))
        .orElseThrow(() -> new RuntimeException("Invalid parent trip"));
    trip.setParentTrip(parentTrip);
  }

  trip.setCreatedBy(createdBy.toString());
  trip.setTripStatus(Trip.TripStatus.CREATED);
  trip.setNotes(createTripDto.getNotes());
  trip.setPickupTime(createTripDto.getPickupTime());
  trip.setStartDate(createTripDto.getStartDate());
  trip.setEndDate(createTripDto.getEndDate());
  trip.setStartOtp((long) ThreadLocalRandom.current().nextInt(1000, 10000));
  trip.setEndOtp((long) ThreadLocalRandom.current().nextInt(1000, 10000));
  trip.setTripStatus(Trip.TripStatus.CREATED);
  // Vendor auto-assign
  if ("VENDOR".equals(tenant.getTenantType().toString())) {
    trip.setVendor(tenant);
  }

  // Vendor override if provided
  if (createTripDto.getVendorId() != null) {
    Tenant vendor = tenantRepository.findById(UUID.fromString(createTripDto.getVendorId()))
        .orElseThrow(() -> new RuntimeException("Invalid vendor"));

    trip.setVendor(vendor);
  }

  // ✅ Passengers
  if (createTripDto.getPassengerIds() != null && !createTripDto.getPassengerIds().isEmpty()) {

    List<UUID> ids = createTripDto.getPassengerIds()
        .stream()
        .map(UUID::fromString)
        .toList();

    List<PeopleTenant> people = peopleTenantRepository.findAllById(ids);

    if (people.size() != ids.size()) {
      throw new RuntimeException("One or more passengers not found");
    }

    // Optional safety: ensure name exists
    people.forEach(p -> {
      if (p.getName() == null) {
        throw new RuntimeException("Passenger name cannot be null: " + p.getId());
      }
    });

    trip.setPassengers(people);
  }

  // ✅ Booker
  if (createTripDto.getBookerId() != null) {
    PeopleTenant booker = peopleTenantRepository.findById(
        UUID.fromString(createTripDto.getBookerId()))
        .orElseThrow(() -> new RuntimeException("Invalid booker"));

    if (booker.getName() == null) {
      throw new RuntimeException("Booker name cannot be null");
    }

    trip.setBooker(booker);
  }

  // ✅ Stops
  trip.setStops(new ArrayList<>());

  if (createTripDto.getStops() != null) {
    for (TripStopRequestDTO stopDto : createTripDto.getStops()) {

      if (stopDto.getAddressText() == null) {
        throw new RuntimeException("Stop addressText cannot be null");
      }

      TripStop stop = mapper.map(stopDto, TripStop.class);
      stop.setTrip(trip);
      trip.getStops().add(stop);
    }
  }

  System.out.println("===== DEBUG NULL CHECK =====");

  System.out.println("Organisation name = " + organisation.getTenantName());
  System.out.println("DutyType name = " + dutyType.getName());
  System.out.println("VehicleType name = " + vehicleType.getDefaultName());

  if (trip.getBooker() != null) {
    System.out.println("Booker name = " + trip.getBooker().getName());
  }

  if (trip.getPassengers() != null) {
    trip.getPassengers().forEach(p ->
        System.out.println("Passenger " + p.getId() + " name=" + p.getName())
    );
  }

  if (trip.getStops() != null) {
    trip.getStops().forEach(s ->
        System.out.println("Stop address=" + s.getAddressText())
    );
  }


  try {
    return repository.save(trip);
  } catch (Exception e) {
    e.printStackTrace();
    throw e;
  }


  // return repository.save(trip);
}


}
