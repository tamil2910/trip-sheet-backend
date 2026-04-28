package com.example.trip_sheet_backend.services.TripService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import com.example.trip_sheet_backend.dtos.TripDtos.TripArrivedRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripDispatchRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripPassengerCustomFieldValueRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripDropRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripStartRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripStopRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripUpdateRequestDTO;
import com.example.trip_sheet_backend.models.CustomField;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.TripPassengerCustomFieldValue;
import com.example.trip_sheet_backend.models.TripStop;
import com.example.trip_sheet_backend.models.TripSummary;
import com.example.trip_sheet_backend.models.UserAccount;
// import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.Vehicle;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.repositories.DriverRepository;
import com.example.trip_sheet_backend.repositories.DriverTenantMappingRepository;
import com.example.trip_sheet_backend.repositories.DutyTypeRepository;
import com.example.trip_sheet_backend.repositories.PeopleTenantRepository;
import com.example.trip_sheet_backend.repositories.CustomFieldRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.TripRepository;
import com.example.trip_sheet_backend.repositories.TripSummaryRepository;
import com.example.trip_sheet_backend.repositories.VehicleRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import jakarta.persistence.criteria.JoinType;

@Service
public class TripServiceImp extends BaseServiceImp<Trip, UUID> implements TripService {
  private final TripRepository repository;
  private final TenantRepository tenantRepository;

    private final DutyTypeRepository dutyTypeRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final PeopleTenantRepository peopleTenantRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final CustomFieldRepository customFieldRepository;
    private final TripSummaryRepository tripSummaryRepository;
    private final DriverTenantMappingRepository driverTenantMappingRepository;


    private final ModelMapper mapper;

  public TripServiceImp(TripRepository repository, TenantRepository tenantRepository, 
    DutyTypeRepository dutyTypeRepository, VehicleTypeRepository vehicleTypeRepository, 
    PeopleTenantRepository peopleTenantRepository, ModelMapper mapper, DriverRepository driverRepository,
      VehicleRepository vehicleRepository, CustomFieldRepository customFieldRepository,
      TripSummaryRepository tripSummaryRepository, DriverTenantMappingRepository driverTenantMappingRepository) {
    super(repository);
    this.repository = repository;
    this.tenantRepository = tenantRepository;
    this.dutyTypeRepository = dutyTypeRepository;
    this.vehicleTypeRepository = vehicleTypeRepository;
    this.mapper = mapper;
    this.peopleTenantRepository = peopleTenantRepository;
    this.driverRepository = driverRepository;
    this.vehicleRepository = vehicleRepository;
    this.customFieldRepository = customFieldRepository;
    this.tripSummaryRepository = tripSummaryRepository;
    this.driverTenantMappingRepository = driverTenantMappingRepository;
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
  trip.setIsManualTrip(Boolean.TRUE.equals(createTripDto.getIsManualTrip()));
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
  List<PeopleTenant> selectedPassengers = new ArrayList<>();
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

    selectedPassengers = people;
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

  // ✅ Passenger custom fields
  trip.setPassengerCustomFieldValues(new ArrayList<>());
  if (createTripDto.getPassengerCustomFieldValues() != null && !createTripDto.getPassengerCustomFieldValues().isEmpty()) {

    if (selectedPassengers.isEmpty()) {
      throw new RuntimeException("passengerIds are required when passengerCustomFieldValues are provided");
    }

    Set<UUID> validPassengerIds = selectedPassengers.stream().map(PeopleTenant::getId).collect(java.util.stream.Collectors.toSet());
    Set<String> payloadDuplicateGuard = new HashSet<>();

    for (TripPassengerCustomFieldValueRequestDTO item : createTripDto.getPassengerCustomFieldValues()) {
      UUID passengerId = UUID.fromString(item.getPassengerId());
      UUID customFieldId = UUID.fromString(item.getCustomFieldId());

      if (!validPassengerIds.contains(passengerId)) {
        throw new RuntimeException("Custom-field value passenger must be present in passengerIds");
      }

      String uniqueKey = passengerId + "::" + customFieldId;
      if (!payloadDuplicateGuard.add(uniqueKey)) {
        throw new RuntimeException("Duplicate custom field provided for same passenger");
      }

      PeopleTenant passenger = selectedPassengers.stream()
          .filter(p -> passengerId.equals(p.getId()))
          .findFirst()
          .orElseThrow(() -> new RuntimeException("Invalid passenger in custom-field values"));

      CustomField customField = customFieldRepository
          .findByIdAndTenant_Id(customFieldId, organisation.getId())
          .orElseThrow(() -> new RuntimeException("Invalid custom field for organisation"));

      TripPassengerCustomFieldValue valueRow = new TripPassengerCustomFieldValue();
      valueRow.setTrip(trip);
      valueRow.setPassenger(passenger);
      valueRow.setCustomField(customField);
      valueRow.setValue(item.getValue() == null ? null : item.getValue().trim());
      valueRow.setCreatedBy(createdBy.toString());

      trip.getPassengerCustomFieldValues().add(valueRow);
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

@Transactional(rollbackFor = Exception.class)
public Trip updateTrip(UUID tenantId, UUID tripId, TripUpdateRequestDTO updateDto, UUID updatedBy) {
  Trip trip = findTripForTenant(tenantId, tripId);
  List<Trip> existingSeriesTrips = isParentSeriesTrip(trip) ? getActiveSeriesTrips(tenantId, trip.getId()) : List.of();

  if (updateDto.getTripCode() != null) {
    trip.setTripCode(updateDto.getTripCode());
  }
  if (updateDto.getTripType() != null) {
    trip.setTripType(updateDto.getTripType());
  }
  if (updateDto.getRecurrenceInterval() != null) {
    trip.setRecurrenceInterval(updateDto.getRecurrenceInterval());
  }
  if (updateDto.getDaysOfWeek() != null) {
    trip.setDaysOfWeek(updateDto.getDaysOfWeek());
  }
  if (updateDto.getRecurrenceFrequency() != null) {
    trip.setRecurrenceFrequency(updateDto.getRecurrenceFrequency());
  }

  if (updateDto.getParentTripId() != null) {
    trip.setParentTrip(resolveParentTrip(updateDto.getParentTripId(), tripId));
  }
  if (updateDto.getOrganisationId() != null) {
    trip.setOrganisation(resolveTenant(updateDto.getOrganisationId(), "Invalid organisation"));
  }
  if (updateDto.getVendorId() != null) {
    trip.setVendor(resolveOptionalTenant(updateDto.getVendorId(), "Invalid vendor"));
  }
  if (updateDto.getDriverId() != null) {
    trip.setDriver(resolveOptionalDriver(updateDto.getDriverId()));
  }
  if (updateDto.getVehicleId() != null) {
    trip.setVehicle(resolveOptionalVehicle(updateDto.getVehicleId()));
  }
  if (updateDto.getDutyTypeId() != null) {
    trip.setDutyType(resolveDutyType(updateDto.getDutyTypeId()));
  }
  if (updateDto.getVehicleTypeId() != null) {
    trip.setVehicleType(resolveVehicleType(updateDto.getVehicleTypeId()));
  }
  if (updateDto.getBookerId() != null) {
    trip.setBooker(resolveOptionalPeople(updateDto.getBookerId(), "Invalid booker"));
  }

  if (updateDto.getNotes() != null) {
    trip.setNotes(updateDto.getNotes());
  }
  if (updateDto.getIsManualTrip() != null) {
    trip.setIsManualTrip(updateDto.getIsManualTrip());
  }
  if (updateDto.getPickupTime() != null) {
    trip.setPickupTime(updateDto.getPickupTime());
  }
  if (updateDto.getStartDate() != null) {
    trip.setStartDate(updateDto.getStartDate());
  }
  if (updateDto.getEndDate() != null) {
    trip.setEndDate(updateDto.getEndDate());
  }

  if (updatedBy != null) {
    trip.setUpdatedBy(updatedBy.toString());
  }

  if (updateDto.getStops() != null) {
    replaceStops(trip, updateDto.getStops());
  }

  List<PeopleTenant> effectivePassengers = trip.getPassengers() == null
      ? new ArrayList<>()
      : new ArrayList<>(trip.getPassengers());

  if (updateDto.getPassengerIds() != null) {
    effectivePassengers = resolvePassengers(updateDto.getPassengerIds());
    trip.setPassengers(effectivePassengers);
  }

  if (updateDto.getPassengerCustomFieldValues() != null) {
    replacePassengerCustomFieldValues(trip, effectivePassengers, updateDto.getPassengerCustomFieldValues());
  }

  if (isParentSeriesTrip(trip)) {
    return updateParentSeriesTrip(trip, existingSeriesTrips, updateDto, updatedBy);
  }

  return repository.save(trip);
}

@Override
@Transactional(readOnly = true)
public List<Trip> getParentAndChildTrips(UUID tenantId, UUID tripId) {
  Trip selectedTrip = findByIdResource(tenantId, tripId);
  if (selectedTrip == null) {
    return List.of();
  }

  Trip rootTrip = selectedTrip;
  while (rootTrip.getParentTrip() != null) {
    rootTrip = rootTrip.getParentTrip();
  }

  UUID rootTripId = rootTrip.getId();
  List<Trip> relatedTrips = new ArrayList<>(repository.findAll((root, query, cb) -> {
    query.distinct(true);

    var tenantPredicate = cb.conjunction();
    if (tenantId != null) {
      try {
        tenantPredicate = cb.equal(root.join("tenant").get("id"), tenantId);
      } catch (Exception ignored) {
        tenantPredicate = cb.conjunction();
      }
    }

    var rootPredicate = cb.equal(root.get("id"), rootTripId);
    var childPredicate = cb.equal(root.join("parentTrip", JoinType.LEFT).get("id"), rootTripId);

    return cb.and(tenantPredicate, cb.or(rootPredicate, childPredicate));
  }, Pageable.unpaged()).getContent());

  relatedTrips.sort(Comparator.comparing(trip -> trip.getPickupTime() != null ? trip.getPickupTime() : Long.MAX_VALUE));

  return relatedTrips;
}

@Override
@Transactional(rollbackFor = Exception.class)
public Trip splitChildTrip(UUID tenantId, UUID tripId) {
  Trip trip = findByIdResource(tenantId, tripId);
  if (trip == null) {
    throw new RuntimeException("Trip not found");
  }

  if (trip.getParentTrip() == null) {
    throw new RuntimeException("Cannot split a parent trip. Only child trips can be detached from their parent.");
  }

  trip.setParentTrip(null);
  return repository.save(trip);
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

private Trip updateParentSeriesTrip(
    Trip rootTrip,
    List<Trip> existingSeriesTrips,
    TripUpdateRequestDTO updateDto,
    UUID updatedBy
) {
  if (rootTrip.getTripType() != Trip.TripType.MULTI_DAY && rootTrip.getTripType() != Trip.TripType.RECURRING) {
    return repository.save(rootTrip);
  }

  long seriesStart = updateDto.getStartDate() != null
      ? updateDto.getStartDate()
      : getSeriesBoundary(existingSeriesTrips, true);
  long seriesEnd = updateDto.getEndDate() != null
      ? updateDto.getEndDate()
      : getSeriesBoundary(existingSeriesTrips, false);

  List<Long> desiredOccurrences = buildSeriesOccurrenceEpochs(rootTrip, seriesStart, seriesEnd);
  if (desiredOccurrences.isEmpty()) {
    throw new RuntimeException("No trip dates generated for the provided range");
  }

  Map<LocalDate, Trip> existingByDate = new HashMap<>();
  for (Trip trip : existingSeriesTrips) {
    existingByDate.put(extractTripDate(trip), trip);
  }

  Trip templateSnapshot = cloneTripTemplate(rootTrip);
  Map<LocalDate, Trip> scheduledTrips = new LinkedHashMap<>();

  for (Long occurrenceEpoch : desiredOccurrences) {
    LocalDate occurrenceDate = epochToUtcDate(occurrenceEpoch);
    Trip scheduledTrip = existingByDate.remove(occurrenceDate);
    boolean isNewTrip = scheduledTrip == null;
    if (scheduledTrip == null) {
      scheduledTrip = new Trip();
    }

    syncTripFromTemplate(scheduledTrip, templateSnapshot, occurrenceEpoch, updatedBy, isNewTrip);
    scheduledTrips.put(occurrenceDate, scheduledTrip);
  }

  Trip newRoot = scheduledTrips.values().iterator().next();
  newRoot.setParentTrip(null);
  newRoot.setIsDeleted(false);
  newRoot.setDeletedAt(null);
  newRoot.setDeletedBy(null);
  Trip savedRoot = repository.save(newRoot);

  for (Trip scheduledTrip : scheduledTrips.values()) {
    if (scheduledTrip == savedRoot) {
      continue;
    }
    scheduledTrip.setParentTrip(savedRoot);
    scheduledTrip.setIsDeleted(false);
    scheduledTrip.setDeletedAt(null);
    scheduledTrip.setDeletedBy(null);
    repository.save(scheduledTrip);
  }

  for (Trip obsoleteTrip : existingByDate.values()) {
    softDeleteTrip(obsoleteTrip, updatedBy);
    repository.save(obsoleteTrip);
  }

  return savedRoot;
}

private Tenant resolveTenant(String tenantId, String invalidMessage) {
  return tenantRepository.findById(UUID.fromString(tenantId))
      .orElseThrow(() -> new RuntimeException(invalidMessage));
}

private Tenant resolveOptionalTenant(String tenantId, String invalidMessage) {
  if (!hasText(tenantId)) {
    return null;
  }
  return resolveTenant(tenantId, invalidMessage);
}

private DutyType resolveDutyType(String dutyTypeId) {
  return dutyTypeRepository.findById(UUID.fromString(dutyTypeId))
      .orElseThrow(() -> new RuntimeException("Invalid duty type"));
}

private VehicleType resolveVehicleType(String vehicleTypeId) {
  return vehicleTypeRepository.findById(UUID.fromString(vehicleTypeId))
      .orElseThrow(() -> new RuntimeException("Invalid vehicle type"));
}

private Driver resolveOptionalDriver(String driverId) {
  if (!hasText(driverId)) {
    return null;
  }
  return driverRepository.findById(UUID.fromString(driverId))
      .orElseThrow(() -> new RuntimeException("Invalid driver"));
}

private Vehicle resolveOptionalVehicle(String vehicleId) {
  if (!hasText(vehicleId)) {
    return null;
  }
  return vehicleRepository.findById(UUID.fromString(vehicleId))
      .orElseThrow(() -> new RuntimeException("Invalid vehicle"));
}

private PeopleTenant resolveOptionalPeople(String peopleId, String invalidMessage) {
  if (!hasText(peopleId)) {
    return null;
  }

  PeopleTenant person = peopleTenantRepository.findById(UUID.fromString(peopleId))
      .orElseThrow(() -> new RuntimeException(invalidMessage));

  if (person.getName() == null) {
    throw new RuntimeException(invalidMessage);
  }

  return person;
}

private Trip resolveParentTrip(String parentTripId, UUID tripId) {
  if (!hasText(parentTripId)) {
    return null;
  }

  UUID parentId = UUID.fromString(parentTripId);
  if (parentId.equals(tripId)) {
    throw new RuntimeException("Trip cannot be its own parent");
  }

  return repository.findById(parentId)
      .orElseThrow(() -> new RuntimeException("Invalid parent trip"));
}

private List<PeopleTenant> resolvePassengers(List<String> passengerIds) {
  if (passengerIds == null || passengerIds.isEmpty()) {
    return new ArrayList<>();
  }

  List<UUID> ids = passengerIds.stream()
      .map(UUID::fromString)
      .toList();

  List<PeopleTenant> people = peopleTenantRepository.findAllById(ids);
  if (people.size() != ids.size()) {
    throw new RuntimeException("One or more passengers not found");
  }

  people.forEach(p -> {
    if (p.getName() == null) {
      throw new RuntimeException("Passenger name cannot be null: " + p.getId());
    }
  });

  return new ArrayList<>(people);
}

private void replaceStops(Trip trip, List<TripStopRequestDTO> stopDtos) {
  trip.getStops().clear();

  if (stopDtos == null || stopDtos.isEmpty()) {
    return;
  }

  for (TripStopRequestDTO stopDto : stopDtos) {
    if (stopDto.getAddressText() == null) {
      throw new RuntimeException("Stop addressText cannot be null");
    }

    TripStop stop = mapper.map(stopDto, TripStop.class);
    stop.setTrip(trip);
    trip.getStops().add(stop);
  }
}

private void replacePassengerCustomFieldValues(
    Trip trip,
    List<PeopleTenant> passengers,
    List<TripPassengerCustomFieldValueRequestDTO> valueDtos
) {
  trip.getPassengerCustomFieldValues().clear();

  if (valueDtos == null || valueDtos.isEmpty()) {
    return;
  }

  if (passengers == null || passengers.isEmpty()) {
    throw new RuntimeException("passengerIds are required when passengerCustomFieldValues are provided");
  }
  if (trip.getOrganisation() == null) {
    throw new RuntimeException("Organisation is required for passenger custom field values");
  }

  Set<UUID> validPassengerIds = passengers.stream()
      .map(PeopleTenant::getId)
      .collect(java.util.stream.Collectors.toSet());
  Set<String> payloadDuplicateGuard = new HashSet<>();

  for (TripPassengerCustomFieldValueRequestDTO item : valueDtos) {
    UUID passengerId = UUID.fromString(item.getPassengerId());
    UUID customFieldId = UUID.fromString(item.getCustomFieldId());

    if (!validPassengerIds.contains(passengerId)) {
      throw new RuntimeException("Custom-field value passenger must be present in passengerIds");
    }

    String uniqueKey = passengerId + "::" + customFieldId;
    if (!payloadDuplicateGuard.add(uniqueKey)) {
      throw new RuntimeException("Duplicate custom field provided for same passenger");
    }

    PeopleTenant passenger = passengers.stream()
        .filter(p -> passengerId.equals(p.getId()))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Invalid passenger in custom-field values"));

    CustomField customField = customFieldRepository
        .findByIdAndTenant_Id(customFieldId, trip.getOrganisation().getId())
        .orElseThrow(() -> new RuntimeException("Invalid custom field for organisation"));

    TripPassengerCustomFieldValue valueRow = new TripPassengerCustomFieldValue();
    valueRow.setTrip(trip);
    valueRow.setPassenger(passenger);
    valueRow.setCustomField(customField);
    valueRow.setValue(item.getValue() == null ? null : item.getValue().trim());
    valueRow.setCreatedBy(trip.getCreatedBy());
    valueRow.setUpdatedBy(trip.getUpdatedBy());

    trip.getPassengerCustomFieldValues().add(valueRow);
  }
}

private boolean hasText(String value) {
  return value != null && !value.isBlank();
}

private boolean isParentSeriesTrip(Trip trip) {
  return trip != null
      && trip.getParentTrip() == null
      && (trip.getTripType() == Trip.TripType.MULTI_DAY || trip.getTripType() == Trip.TripType.RECURRING);
}

private List<Trip> getActiveSeriesTrips(UUID tenantId, UUID rootTripId) {
  List<Trip> seriesTrips = repository.findAll((root, query, cb) -> {
    query.distinct(true);

    var rootPredicate = cb.equal(root.get("id"), rootTripId);
    var childPredicate = cb.equal(root.join("parentTrip", JoinType.LEFT).get("id"), rootTripId);
    var notDeletedPredicate = cb.equal(root.get("isDeleted"), false);

    if (tenantId == null) {
      return cb.and(notDeletedPredicate, cb.or(rootPredicate, childPredicate));
    }

    var tenantPredicate = cb.equal(root.join("tenant").get("id"), tenantId);
    return cb.and(tenantPredicate, notDeletedPredicate, cb.or(rootPredicate, childPredicate));
  }, Pageable.unpaged()).getContent();

  seriesTrips.sort(Comparator.comparing(trip -> trip.getPickupTime() != null ? trip.getPickupTime() : Long.MAX_VALUE));
  return seriesTrips;
}

private long getSeriesBoundary(List<Trip> seriesTrips, boolean startBoundary) {
  var stream = seriesTrips.stream()
      .mapToLong(trip -> trip.getPickupTime() != null ? trip.getPickupTime() : trip.getStartDate());

  return (startBoundary ? stream.min() : stream.max())
      .orElseThrow(() -> new RuntimeException(startBoundary
          ? "Unable to determine current trip series start date"
          : "Unable to determine current trip series end date"));
}

private List<Long> buildSeriesOccurrenceEpochs(Trip templateTrip, long seriesStart, long seriesEnd) {
  if (templateTrip.getTripType() == Trip.TripType.MULTI_DAY) {
    return buildMultiDayOccurrenceEpochs(templateTrip, seriesStart, seriesEnd);
  }

  if (templateTrip.getTripType() == Trip.TripType.RECURRING) {
    return buildRecurringOccurrenceEpochs(templateTrip, seriesStart, seriesEnd);
  }

  return List.of(seriesStart);
}

private List<Long> buildMultiDayOccurrenceEpochs(Trip templateTrip, long seriesStart, long seriesEnd) {
  LocalDate start = Instant.ofEpochSecond(seriesStart).atZone(ZoneOffset.UTC).toLocalDate();
  LocalDate end = Instant.ofEpochSecond(seriesEnd).atZone(ZoneOffset.UTC).toLocalDate();

  if (end.isBefore(start)) {
    throw new RuntimeException("endDate must be greater than or equal to startDate for MULTI_DAY trip");
  }

  List<Long> occurrences = new ArrayList<>();
  LocalDate cursor = start;
  while (!cursor.isAfter(end)) {
    occurrences.add(buildTripDateTimeEpoch(cursor, templateTrip.getPickupTime()));
    cursor = cursor.plusDays(1);
  }
  return occurrences;
}

private List<Long> buildRecurringOccurrenceEpochs(Trip templateTrip, long seriesStart, long seriesEnd) {
  if (templateTrip.getRecurrenceFrequency() == null) {
    throw new RuntimeException("recurrenceFrequency is required for RECURRING trip");
  }
  if (templateTrip.getRecurrenceInterval() == null || templateTrip.getRecurrenceInterval() < 1) {
    throw new RuntimeException("recurrenceInterval must be at least 1 for RECURRING trip");
  }

  LocalDate start = Instant.ofEpochSecond(seriesStart).atZone(ZoneOffset.UTC).toLocalDate();
  LocalDate end = Instant.ofEpochSecond(seriesEnd).atZone(ZoneOffset.UTC).toLocalDate();

  if (end.isBefore(start)) {
    throw new RuntimeException("endDate must be greater than or equal to startDate for RECURRING trip");
  }

  List<Long> occurrences = new ArrayList<>();

  if (templateTrip.getRecurrenceFrequency() == Trip.RecurrenceFrequency.MONTHLY) {
    LocalDate cursor = start;
    while (!cursor.isAfter(end)) {
      occurrences.add(buildTripDateTimeEpoch(cursor, templateTrip.getPickupTime()));
      cursor = cursor.plusMonths(templateTrip.getRecurrenceInterval());
    }
    return occurrences;
  }

  Set<DayOfWeekValue> allowedDays = parseAllowedDays(templateTrip.getDaysOfWeek(), templateTrip.getRecurrenceFrequency());
  LocalDate cursor = start;
  while (!cursor.isAfter(end)) {
    long weeksFromStart = ChronoUnit.WEEKS.between(start, cursor);
    boolean matchesInterval = weeksFromStart % templateTrip.getRecurrenceInterval() == 0;

    if (matchesInterval && allowedDays.contains(DayOfWeekValue.from(cursor.getDayOfWeek()))) {
      occurrences.add(buildTripDateTimeEpoch(cursor, templateTrip.getPickupTime()));
    }

    cursor = cursor.plusDays(1);
  }

  return occurrences;
}

private void syncTripFromTemplate(Trip target, Trip template, long occurrenceEpoch, UUID updatedBy, boolean isNewTrip) {
  target.setTripCode(template.getTripCode());
  target.setTripType(template.getTripType());
  target.setRecurrenceInterval(template.getRecurrenceInterval());
  target.setDaysOfWeek(template.getDaysOfWeek());
  target.setRecurrenceFrequency(template.getRecurrenceFrequency());
  target.setOrganisation(template.getOrganisation());
  target.setTenant(template.getTenant());
  target.setVendor(template.getVendor());
  target.setAssignedByVendor(template.getAssignedByVendor());
  target.setPreviousVendor(template.getPreviousVendor());
  target.setNotes(template.getNotes());
  target.setDriver(template.getDriver());
  target.setVehicle(template.getVehicle());
  target.setDutyType(template.getDutyType());
  target.setVehicleType(template.getVehicleType());
  target.setBooker(template.getBooker());
  target.setPassengers(template.getPassengers() == null ? new ArrayList<>() : new ArrayList<>(template.getPassengers()));
  target.setPickupTime(occurrenceEpoch);
  target.setStartDate(occurrenceEpoch);
  target.setEndDate(occurrenceEpoch);
  target.setUpdatedBy(updatedBy != null ? updatedBy.toString() : template.getUpdatedBy());

  if (isNewTrip) {
    target.setCreatedBy(updatedBy != null ? updatedBy.toString() : template.getCreatedBy());
    target.setTripStatus(Trip.TripStatus.CREATED);
    target.setStartOtp((long) ThreadLocalRandom.current().nextInt(1000, 10000));
    target.setEndOtp((long) ThreadLocalRandom.current().nextInt(1000, 10000));
  }

  copyStopsFromTemplate(target, template);
  copyPassengerCustomFieldValuesFromTemplate(target, template);
}

private void copyStopsFromTemplate(Trip target, Trip template) {
  target.getStops().clear();

  if (template.getStops() == null || template.getStops().isEmpty()) {
    return;
  }

  for (TripStop stop : template.getStops()) {
    TripStop copiedStop = new TripStop();
    copiedStop.setSequenceNumber(stop.getSequenceNumber());
    copiedStop.setStopType(stop.getStopType());
    copiedStop.setAddressText(stop.getAddressText());
    copiedStop.setFormattedAddress(stop.getFormattedAddress());
    copiedStop.setLatitude(stop.getLatitude());
    copiedStop.setLongitude(stop.getLongitude());
    copiedStop.setAccurate(stop.getAccurate());
    copiedStop.setTrip(target);
    target.getStops().add(copiedStop);
  }
}

private void copyPassengerCustomFieldValuesFromTemplate(Trip target, Trip template) {
  target.getPassengerCustomFieldValues().clear();

  if (template.getPassengerCustomFieldValues() == null || template.getPassengerCustomFieldValues().isEmpty()) {
    return;
  }

  for (TripPassengerCustomFieldValue sourceValue : template.getPassengerCustomFieldValues()) {
    TripPassengerCustomFieldValue copiedValue = new TripPassengerCustomFieldValue();
    copiedValue.setTrip(target);
    copiedValue.setPassenger(sourceValue.getPassenger());
    copiedValue.setCustomField(sourceValue.getCustomField());
    copiedValue.setValue(sourceValue.getValue());
    copiedValue.setCreatedBy(sourceValue.getCreatedBy());
    copiedValue.setUpdatedBy(target.getUpdatedBy());
    target.getPassengerCustomFieldValues().add(copiedValue);
  }
}

private void softDeleteTrip(Trip trip, UUID deletedBy) {
  trip.setIsDeleted(true);
  trip.setDeletedAt(Instant.now().toEpochMilli());
  trip.setDeletedBy(deletedBy != null ? deletedBy.toString() : trip.getUpdatedBy());
}

private LocalDate extractTripDate(Trip trip) {
  Long epoch = trip.getPickupTime() != null ? trip.getPickupTime() : trip.getStartDate();
  if (epoch == null) {
    throw new RuntimeException("Trip date is missing for trip: " + trip.getId());
  }
  return epochToUtcDate(epoch);
}

private LocalDate epochToUtcDate(long epochSeconds) {
  return Instant.ofEpochSecond(epochSeconds)
      .atZone(ZoneOffset.UTC)
      .toLocalDate();
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
  clone.setPassengerCustomFieldValues(new ArrayList<>());
  if (source.getPassengerCustomFieldValues() != null) {
    for (TripPassengerCustomFieldValue sourceValue : source.getPassengerCustomFieldValues()) {
      TripPassengerCustomFieldValue copiedValue = new TripPassengerCustomFieldValue();
      copiedValue.setTrip(clone);
      copiedValue.setPassenger(sourceValue.getPassenger());
      copiedValue.setCustomField(sourceValue.getCustomField());
      copiedValue.setValue(sourceValue.getValue());
      copiedValue.setCreatedBy(sourceValue.getCreatedBy());
      clone.getPassengerCustomFieldValues().add(copiedValue);
    }
  }
  clone.setBooker(source.getBooker());
  clone.setPickupTime(source.getPickupTime());
  clone.setStartDate(source.getStartDate());
  clone.setEndDate(source.getEndDate());
  clone.setIsManualTrip(source.getIsManualTrip());
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

@Override
@Transactional(rollbackFor = Exception.class)
public Trip dispatchTrip(UUID tokenTenantId, Tenant tokenTenant, UserAccount user, UUID tripID, TripDispatchRequestDTO dispatchData) {
  Trip trip = findTripForTenant(tokenTenantId, tripID);
  authorizeTripLifecycleAction(trip, tokenTenantId, tokenTenant, user, false);
  ensureTripStatus(trip, Trip.TripStatus.CREATED, Trip.TripStatus.CONFIRMED);

  TripSummary summary = getOrCreateTripSummary(trip);
  summary.setDispatchLat(dispatchData.getDispatchLat());
  summary.setDispatchLng(dispatchData.getDispatchLng());
  tripSummaryRepository.save(summary);

  trip.setTripStatus(Trip.TripStatus.DISPATCHED);
  return repository.save(trip);
}

@Override
@Transactional(rollbackFor = Exception.class)
public Trip arrivedTrip(UUID tokenTenantId, Tenant tokenTenant, UserAccount user, UUID tripID, TripArrivedRequestDTO arrivedData) {
  Trip trip = findTripForTenant(tokenTenantId, tripID);
  authorizeTripLifecycleAction(trip, tokenTenantId, tokenTenant, user, false);
  ensureTripStatus(trip, Trip.TripStatus.DISPATCHED, Trip.TripStatus.STARTED);

  TripSummary summary = getOrCreateTripSummary(trip);
  summary.setArrivedLat(arrivedData.getArrivedLat());
  summary.setArrivedLng(arrivedData.getArrivedLng());
  tripSummaryRepository.save(summary);

  trip.setTripStatus(Trip.TripStatus.ARRIVED);
  return repository.save(trip);
}

@Override
@Transactional(rollbackFor = Exception.class)
public Trip startTrip(UUID tokenTenantId, Tenant tokenTenant, UserAccount user, UUID tripID, TripStartRequestDTO startData) {
  Trip trip = findTripForTenant(tokenTenantId, tripID);
  authorizeTripLifecycleAction(trip, tokenTenantId, tokenTenant, user, false);
  ensureTripStatus(trip, Trip.TripStatus.ARRIVED, Trip.TripStatus.DISPATCHED);

  if (trip.getStartOtp() == null || !trip.getStartOtp().equals(startData.getStartOtp())) {
    throw new RuntimeException("Invalid start OTP");
  }

  TripSummary summary = getOrCreateTripSummary(trip);
  summary.setTripStartKmOdo(startData.getTripStartKmOdo());
  summary.setTripStartLat(startData.getTripStartLat());
  summary.setTripStartLng(startData.getTripStartLng());
  summary.setTripStartTime(System.currentTimeMillis());
  tripSummaryRepository.save(summary);

  trip.setTripStatus(Trip.TripStatus.STARTED);
  return repository.save(trip);
}

@Override
@Transactional(rollbackFor = Exception.class)
public Trip dropTrip(UUID tokenTenantId, Tenant tokenTenant, UserAccount user, UUID tripID, TripDropRequestDTO dropData) {
  Trip trip = findTripForTenant(tokenTenantId, tripID);
  authorizeTripLifecycleAction(trip, tokenTenantId, tokenTenant, user, true);
  ensureTripStatus(trip, Trip.TripStatus.STARTED, Trip.TripStatus.ARRIVED);

  if (trip.getEndOtp() == null || !trip.getEndOtp().equals(dropData.getEndOtp())) {
    throw new RuntimeException("Invalid end OTP");
  }

  TripSummary summary = getOrCreateTripSummary(trip);
  summary.setTripEndKmOdo(dropData.getTripEndKmOdo());
  summary.setTripEndLat(dropData.getTripEndLat());
  summary.setTripEndLng(dropData.getTripEndLng());
  summary.setTripEndTime(System.currentTimeMillis());
  summary.setGarageEndLat(dropData.getTripEndLat());
  summary.setGarageEndLng(dropData.getTripEndLng());
  summary.setGarageEndTime(System.currentTimeMillis());
  tripSummaryRepository.save(summary);

  trip.setTripStatus(Trip.TripStatus.COMPLETED);
  return repository.save(trip);
}

@Transactional(rollbackFor = Exception.class)
public Trip dispatchTrip(UUID tokenTenantId, UUID tripID, Map<String, Object> dispatchData) {
  TripDispatchRequestDTO dto = new TripDispatchRequestDTO();
  dto.setDispatchLat(toDouble(dispatchData.get("dispatchLat")));
  dto.setDispatchLng(toDouble(dispatchData.get("dispatchLng")));
  return dispatchTrip(tokenTenantId, null, null, tripID, dto);
}

private Trip findTripForTenant(UUID tenantId, UUID tripId) {
  Trip trip = findByIdResource(tenantId, tripId);
  if (trip == null || Boolean.TRUE.equals(trip.getIsDeleted())) {
    throw new RuntimeException("Trip not found");
  }
  return trip;
}

private void ensureTripStatus(Trip trip, Trip.TripStatus... allowedStatuses) {
  if (trip == null) {
    throw new RuntimeException("Trip not found");
  }

  if (allowedStatuses == null || allowedStatuses.length == 0) {
    return;
  }

  for (Trip.TripStatus allowedStatus : allowedStatuses) {
    if (trip.getTripStatus() == allowedStatus) {
      return;
    }
  }

  throw new RuntimeException("Trip cannot transition from status " + trip.getTripStatus());
}

private void authorizeTripLifecycleAction(
    Trip trip,
    UUID tenantId,
    Tenant tokenTenant,
    UserAccount user,
    boolean closingManualTrip
) {
  if (trip == null) {
    throw new RuntimeException("Trip not found");
  }
  if (user == null) {
    throw new RuntimeException("Authenticated user not found");
  }

  if (Boolean.TRUE.equals(trip.getIsManualTrip())) {
    if (tokenTenant == null || tokenTenant.getTenantType() != Tenant.TenantType.VENDOR) {
      throw new RuntimeException("Only vendor users can update manual trips");
    }

    if (closingManualTrip && isDriverUser(user)) {
      throw new RuntimeException("Driver cannot close manual trip");
    }
    return;
  }

  if (tenantId == null) {
    throw new RuntimeException("Tenant not found in token");
  }
  if (trip.getDriver() == null) {
    throw new RuntimeException("Driver is not assigned to this trip");
  }

  Driver actorDriver = driverRepository.findByAccount_Id(user.getId())
      .orElseThrow(() -> new RuntimeException("Only driver can update non-manual trip"));

  if (!actorDriver.getId().equals(trip.getDriver().getId())) {
    throw new RuntimeException("Only assigned driver can update this trip");
  }

  driverTenantMappingRepository.findByDriver_IdAndTenant_Id(actorDriver.getId(), tenantId)
      .filter(mapping -> Boolean.TRUE.equals(mapping.getActive()))
      .orElseThrow(() -> new RuntimeException("Driver does not belong to this tenant"));
}

private boolean isDriverUser(UserAccount user) {
  if (user == null || user.getId() == null) {
    return false;
  }
  return driverRepository.findByAccount_Id(user.getId()).isPresent();
}

private TripSummary getOrCreateTripSummary(Trip trip) {
  return tripSummaryRepository.findByTripId_Id(trip.getId())
      .map(existing -> {
        if (existing.getTenant() == null) {
          existing.setTenant(trip.getTenant());
        }
        return existing;
      })
      .orElseGet(() -> {
        TripSummary summary = new TripSummary();
        summary.setTripId(trip);
        summary.setTenant(trip.getTenant());
        return summary;
      });
}

private Double toDouble(Object value) {
  if (value == null) {
    return null;
  }
  if (value instanceof Number number) {
    return number.doubleValue();
  }
  try {
    return Double.parseDouble(value.toString());
  } catch (Exception ex) {
    throw new RuntimeException("Invalid coordinate value: " + value);
  }
}


}
