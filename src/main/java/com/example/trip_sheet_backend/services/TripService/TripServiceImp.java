package com.example.trip_sheet_backend.services.TripService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

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
  trip.setRecurrenceInterval(createTripDto.getRecurrenceInterval());
  trip.setDaysOfWeek(createTripDto.getDaysOfWeek());
  trip.setRecurrenceFrequency(createTripDto.getRecurrenceFrequency());
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

  if (trip.getTripType() == Trip.TripType.MULTI_DAY) {
    return createMultiDayTrips(trip);
  }

  if (trip.getTripType() == Trip.TripType.RECURRING) {
    return createRecurringTrips(trip);
  }

  return repository.save(trip);
}

@Override
@Transactional(rollbackFor = Exception.class)
public List<Trip> createBulkTrips(List<TripCreateRequestDTO> createTripDtos, Tenant tenant, UUID createdBy) {
  if (createTripDtos == null || createTripDtos.isEmpty()) {
    throw new RuntimeException("Trip list cannot be empty for bulk create");
  }

  List<Trip> createdTrips = new ArrayList<>();
  Trip firstCreatedTrip = null;

  for (TripCreateRequestDTO dto : createTripDtos) {
    Trip createdTrip = createTrip(dto, tenant, createdBy);

    if (firstCreatedTrip == null) {
      firstCreatedTrip = createdTrip;
      createdTrips.add(createdTrip);
      continue;
    }

    createdTrip.setParentTrip(firstCreatedTrip);
    Trip updatedTrip = repository.save(createdTrip);
    createdTrips.add(updatedTrip);
  }

  return createdTrips;
}

@Override
public Page<Trip> searchResourcesWithGlobalSearch(UUID tenantId, Map<String, Object> filters, String globalSearch, Pageable pageable) {
  return super.searchResourcesWithGlobalSearch(tenantId, filters, globalSearch, pageable);
}

private Trip createMultiDayTrips(Trip templateTrip) {
  if (templateTrip.getStartDate() == null || templateTrip.getEndDate() == null) {
    throw new RuntimeException("startDate and endDate are required for MULTI_DAY trip");
  }

  LocalDate start = Instant.ofEpochSecond(templateTrip.getStartDate())
      .atZone(ZoneOffset.UTC)
      .toLocalDate();
  LocalDate end = Instant.ofEpochSecond(templateTrip.getEndDate())
      .atZone(ZoneOffset.UTC)
      .toLocalDate();

  if (end.isBefore(start)) {
    throw new RuntimeException("endDate must be greater than or equal to startDate for MULTI_DAY trip");
  }

  Trip firstTrip = null;
  Trip seriesParentTrip = null;

  LocalDate current = start;
  while (!current.isAfter(end)) {
    Trip dailyTrip = cloneTripTemplate(templateTrip);
    long tripDateTimeEpoch = buildTripDateTimeEpoch(current, templateTrip.getPickupTime());

    dailyTrip.setStartDate(tripDateTimeEpoch);
    dailyTrip.setEndDate(tripDateTimeEpoch);
    dailyTrip.setPickupTime(tripDateTimeEpoch);
    dailyTrip.setParentTrip(seriesParentTrip);
    dailyTrip.setTripStatus(Trip.TripStatus.CREATED);
    dailyTrip.setStartOtp((long) ThreadLocalRandom.current().nextInt(1000, 10000));
    dailyTrip.setEndOtp((long) ThreadLocalRandom.current().nextInt(1000, 10000));

    Trip savedTrip = repository.save(dailyTrip);
    if (firstTrip == null) {
      firstTrip = savedTrip;
      seriesParentTrip = savedTrip;
    }
    current = current.plusDays(1);
  }

  return firstTrip;
}

private Trip createRecurringTrips(Trip templateTrip) {
  if (templateTrip.getStartDate() == null || templateTrip.getEndDate() == null) {
    throw new RuntimeException("startDate and endDate are required for RECURRING trip");
  }
  if (templateTrip.getRecurrenceFrequency() == null) {
    throw new RuntimeException("recurrenceFrequency is required for RECURRING trip");
  }
  if (templateTrip.getRecurrenceInterval() == null || templateTrip.getRecurrenceInterval() < 1) {
    throw new RuntimeException("recurrenceInterval must be at least 1 for RECURRING trip");
  }

  LocalDate start = Instant.ofEpochSecond(templateTrip.getStartDate())
      .atZone(ZoneOffset.UTC)
      .toLocalDate();
  LocalDate end = Instant.ofEpochSecond(templateTrip.getEndDate())
      .atZone(ZoneOffset.UTC)
      .toLocalDate();

  if (end.isBefore(start)) {
    throw new RuntimeException("endDate must be greater than or equal to startDate for RECURRING trip");
  }

  Set<DayOfWeekValue> allowedDays = parseAllowedDays(templateTrip.getDaysOfWeek(), templateTrip.getRecurrenceFrequency());

  Trip firstTrip = null;
  Trip seriesParentTrip = null;

  if (templateTrip.getRecurrenceFrequency() == Trip.RecurrenceFrequency.MONTHLY) {
    LocalDate current = start;
    while (!current.isAfter(end)) {
      Trip recurringTrip = cloneTripTemplate(templateTrip);
      long tripDateTimeEpoch = buildTripDateTimeEpoch(current, templateTrip.getPickupTime());

      recurringTrip.setStartDate(tripDateTimeEpoch);
      recurringTrip.setEndDate(tripDateTimeEpoch);
      recurringTrip.setPickupTime(tripDateTimeEpoch);
      recurringTrip.setParentTrip(seriesParentTrip);
      recurringTrip.setTripStatus(Trip.TripStatus.CREATED);
      recurringTrip.setStartOtp((long) ThreadLocalRandom.current().nextInt(1000, 10000));
      recurringTrip.setEndOtp((long) ThreadLocalRandom.current().nextInt(1000, 10000));

      Trip savedTrip = repository.save(recurringTrip);
      if (firstTrip == null) {
        firstTrip = savedTrip;
        seriesParentTrip = savedTrip;
      }
      current = current.plusMonths(templateTrip.getRecurrenceInterval());
    }
    return firstTrip;
  }

  LocalDate cursor = start;
  while (!cursor.isAfter(end)) {
    long weeksFromStart = ChronoUnit.WEEKS.between(start, cursor);
    boolean matchesInterval = weeksFromStart % templateTrip.getRecurrenceInterval() == 0;

    if (matchesInterval && allowedDays.contains(DayOfWeekValue.from(cursor.getDayOfWeek()))) {
      Trip recurringTrip = cloneTripTemplate(templateTrip);
      long tripDateTimeEpoch = buildTripDateTimeEpoch(cursor, templateTrip.getPickupTime());

      recurringTrip.setStartDate(tripDateTimeEpoch);
      recurringTrip.setEndDate(tripDateTimeEpoch);
      recurringTrip.setPickupTime(tripDateTimeEpoch);
      recurringTrip.setParentTrip(seriesParentTrip);
      recurringTrip.setTripStatus(Trip.TripStatus.CREATED);
      recurringTrip.setStartOtp((long) ThreadLocalRandom.current().nextInt(1000, 10000));
      recurringTrip.setEndOtp((long) ThreadLocalRandom.current().nextInt(1000, 10000));

      Trip savedTrip = repository.save(recurringTrip);
      if (firstTrip == null) {
        firstTrip = savedTrip;
        seriesParentTrip = savedTrip;
      }
    }

    cursor = cursor.plusDays(1);
  }

  return firstTrip;
}

private Trip cloneTripTemplate(Trip source) {
  Trip clone = new Trip();
  clone.setTripCode(source.getTripCode());
  clone.setTripType(source.getTripType());
  clone.setRecurrenceInterval(source.getRecurrenceInterval());
  clone.setDaysOfWeek(source.getDaysOfWeek());
  clone.setRecurrenceFrequency(source.getRecurrenceFrequency());
  clone.setOrganisation(source.getOrganisation());
  clone.setTenant(source.getTenant());
  clone.setVendor(source.getVendor());
  clone.setAssignedByVendor(source.getAssignedByVendor());
  clone.setPreviousVendor(source.getPreviousVendor());
  clone.setNotes(source.getNotes());
  clone.setDriver(source.getDriver());
  clone.setVehicle(source.getVehicle());
  clone.setDutyType(source.getDutyType());
  clone.setVehicleType(source.getVehicleType());
  clone.setPassengers(source.getPassengers() == null ? null : new ArrayList<>(source.getPassengers()));
  clone.setBooker(source.getBooker());
  clone.setPickupTime(source.getPickupTime());
  clone.setStartDate(source.getStartDate());
  clone.setEndDate(source.getEndDate());
  clone.setCreatedBy(source.getCreatedBy());
  clone.setTripStatus(Trip.TripStatus.CREATED);

  clone.setStops(new ArrayList<>());
  if (source.getStops() != null) {
    for (TripStop stop : source.getStops()) {
      TripStop copiedStop = new TripStop();
      copiedStop.setSequenceNumber(stop.getSequenceNumber());
      copiedStop.setStopType(stop.getStopType());
      copiedStop.setAddressText(stop.getAddressText());
      copiedStop.setFormattedAddress(stop.getFormattedAddress());
      copiedStop.setLatitude(stop.getLatitude());
      copiedStop.setLongitude(stop.getLongitude());
      copiedStop.setAccurate(stop.getAccurate());
      copiedStop.setTrip(clone);
      clone.getStops().add(copiedStop);
    }
  }

  return clone;
}

private long buildTripDateTimeEpoch(LocalDate date, Long pickupTime) {
  LocalTime parsedPickupTime = parsePickupTime(pickupTime);
  return date.atTime(parsedPickupTime).toEpochSecond(ZoneOffset.UTC);
}

private LocalTime parsePickupTime(Long pickupTime) {
  if (pickupTime == null || pickupTime <= 0) {
    return LocalTime.MIDNIGHT;
  }

  // pickupTime is stored as epoch seconds; take only the UTC time-of-day part.
  return Instant.ofEpochSecond(pickupTime)
      .atZone(ZoneOffset.UTC)
      .toLocalTime()
      .withSecond(0)
      .withNano(0);
}

private Set<DayOfWeekValue> parseAllowedDays(String daysOfWeek, Trip.RecurrenceFrequency frequency) {
  Set<DayOfWeekValue> allowedDays = new HashSet<>();

  if (daysOfWeek == null || daysOfWeek.isBlank()) {
    if (frequency == Trip.RecurrenceFrequency.WEEKLY) {
      throw new RuntimeException("daysOfWeek is required for WEEKLY recurring trip");
    }
    return allowedDays;
  }

  if (frequency != Trip.RecurrenceFrequency.WEEKLY) {
    return allowedDays;
  }

  Arrays.stream(daysOfWeek.split(","))
      .map(String::trim)
      .filter(value -> !value.isBlank())
      .map(DayOfWeekValue::from)
      .forEach(allowedDays::add);

  return allowedDays;
}

private enum DayOfWeekValue {
  MON, TUE, WED, THU, FRI, SAT, SUN;

  static DayOfWeekValue from(String value) {
    return DayOfWeekValue.valueOf(value.trim().substring(0, 3).toUpperCase(Locale.ROOT));
  }

  static DayOfWeekValue from(DayOfWeek value) {
    return DayOfWeekValue.valueOf(value.name().substring(0, 3).toUpperCase(Locale.ROOT));
  }
}


}
