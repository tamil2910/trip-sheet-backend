package com.example.trip_sheet_backend.services.BookingService;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.dtos.TripDtos.BookingCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.BookingResponseDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripStopRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripUpdateRequestDTO;
import com.example.trip_sheet_backend.mappers.BookingResponseMapper;
import com.example.trip_sheet_backend.models.Booking;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.TripStop;
import com.example.trip_sheet_backend.models.Vehicle;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.repositories.BookingRepository;
import com.example.trip_sheet_backend.repositories.DriverRepository;
import com.example.trip_sheet_backend.repositories.DutyTypeRepository;
import com.example.trip_sheet_backend.repositories.PeopleTenantRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.TripRepository;
import com.example.trip_sheet_backend.repositories.VehicleRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeRepository;
import com.example.trip_sheet_backend.services.TripService.TripServiceImp;

@Service
public class BookingServiceImp extends BaseServiceImp<Booking, UUID> implements BookingService {
  private final BookingRepository bookingRepository;
  private final TenantRepository tenantRepository;
  private final TripServiceImp tripServiceImp;
  private final DutyTypeRepository dutyTypeRepository;
  private final VehicleTypeRepository vehicleTypeRepository;
  private final PeopleTenantRepository peopleTenantRepository;
  private final DriverRepository driverRepository;
  private final VehicleRepository vehicleRepository;
  private final ModelMapper mapper;
  private final TripRepository tripRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String geocodeBaseUrl;

  public BookingServiceImp(BookingRepository bookingRepository, TenantRepository tenantRepository,
    TripServiceImp tripServiceImp, DutyTypeRepository dutyTypeRepository, VehicleTypeRepository vehicleTypeRepository,
    PeopleTenantRepository peopleTenantRepository, DriverRepository driverRepository,
        VehicleRepository vehicleRepository, ModelMapper mapper, TripRepository tripRepository,
        @Value("${geocode.base-url:https://nominatim.openstreetmap.org}") String geocodeBaseUrl
  ) {
    super(bookingRepository);
    this.bookingRepository = bookingRepository;
    this.tenantRepository = tenantRepository;
    this.tripServiceImp = tripServiceImp;
    this.dutyTypeRepository = dutyTypeRepository;
    this.vehicleTypeRepository = vehicleTypeRepository;
    this.peopleTenantRepository = peopleTenantRepository;
    this.driverRepository = driverRepository;
    this.vehicleRepository = vehicleRepository;
    this.mapper = mapper;
    this.tripRepository = tripRepository;
    this.objectMapper = new ObjectMapper();
    this.httpClient = HttpClient.newHttpClient();
    this.geocodeBaseUrl = geocodeBaseUrl;
  }

  @Transactional(rollbackFor = Exception.class)
  public Booking createBooking(BookingCreateRequestDTO dto, Tenant tenant, UUID createdBy) {

      System.out.println("===== CREATE BOOKING START =====");

      // Create Booking
      Booking booking = new Booking();
      booking.setBookingCode(dto.getBookingCode());
      booking.setBookingType(dto.getBookingType());
      booking.setAutoGenerateTrips(dto.getAutoGenerateTrips());
      booking.setTenant(tenant);
      booking.setCreatedBy(createdBy.toString());

      // Assign vendor ONLY if tenant is vendor
      if (tenant.getTenantType() == Tenant.TenantType.VENDOR) {
          booking.setVendor(tenant);
      }

      // If vendorId explicitly passed, override
      if (dto.getVendorId() != null) {
          Tenant vendor = tenantRepository.findById(UUID.fromString(dto.getVendorId()))
              .orElseThrow(() -> new RuntimeException("Invalid vendorId"));
          booking.setVendor(vendor);
      }

      // Dates
      if (dto.getStartDate() != null) {
          booking.setStartDate(
              Instant.ofEpochSecond(dto.getStartDate())
                  .atZone(ZoneId.systemDefault())
                  .toLocalDate()
          );
      }

      if (dto.getEndDate() != null) {
          booking.setEndDate(
              Instant.ofEpochSecond(dto.getEndDate())
                  .atZone(ZoneId.systemDefault())
                  .toLocalDate()
          );
      }

      booking.setTrips(new ArrayList<>());

      // Save Booking FIRST
      booking = bookingRepository.save(booking);

      // Create Trips
      if (dto.getTrips() != null) {
          for (TripCreateRequestDTO tripDto : dto.getTrips()) {
              Trip trip = createTripWithTenantRule(tripDto, tenant, createdBy, booking);
              booking.getTrips().add(trip);
          }
      }

      System.out.println("===== CREATE BOOKING SUCCESS =====");

      return booking;
  }

  private Trip createTripWithTenantRule(
      TripCreateRequestDTO dto,
      Tenant tenant,
      UUID createdBy,
      Booking booking
  ) {

      Trip trip = new Trip();
      trip.setBooking(booking);
      trip.setTenant(tenant);
      trip.setTripStatus(Trip.TripStatus.CREATED);
      trip.setCreatedBy(createdBy.toString());
      trip.setPickupTime(dto.getPickupTime());
      trip.setEndDate(dto.getEndDate());
      trip.setNotes(dto.getNotes());

      // 🔥 RULE: Auto assign based on tenant type
      if (tenant.getTenantType() == Tenant.TenantType.ORGANISATION) {
          trip.setOrganisation(tenant);
      }

      if (tenant.getTenantType() == Tenant.TenantType.VENDOR) {
          trip.setVendor(tenant);
      }

      // Optional override from DTO
      if (dto.getOrganisationId() != null) {
          Tenant organisation = tenantRepository.findById(UUID.fromString(dto.getOrganisationId()))
              .orElseThrow(() -> new RuntimeException("Invalid organisationId"));
          trip.setOrganisation(organisation);
      }

      if (dto.getVendorId() != null) {
          Tenant vendor = tenantRepository.findById(UUID.fromString(dto.getVendorId()))
              .orElseThrow(() -> new RuntimeException("Invalid vendorId"));
          trip.setVendor(vendor);
      }

      // Required linked entities
      DutyType dutyType = dutyTypeRepository.findById(UUID.fromString(dto.getDutyTypeId()))
          .orElseThrow(() -> new RuntimeException("Invalid dutyTypeId"));

      VehicleType vehicleType = vehicleTypeRepository.findById(UUID.fromString(dto.getVehicleTypeId()))
          .orElseThrow(() -> new RuntimeException("Invalid vehicleTypeId"));

      // Driver driver = driverRepository.findById(UUID.fromString(dto.getDriverId()))
      //     .orElseThrow(() -> new RuntimeException("Invalid driverId"));

      // Vehicle vehicle = vehicleRepository.findById(UUID.fromString(dto.getVehicleId()))
      //     .orElseThrow(() -> new RuntimeException("Invalid vehicleId"));

      trip.setDutyType(dutyType);
      trip.setVehicleType(vehicleType);
      // trip.setDriver(driver);
      // trip.setVehicle(vehicle);

      // Booker
      if (dto.getBookerId() != null) {
          PeopleTenant booker = peopleTenantRepository.findById(UUID.fromString(dto.getBookerId()))
              .orElseThrow(() -> new RuntimeException("Invalid bookerId"));
          trip.setBooker(booker);
      }

      // Passengers
      if (dto.getPassengerIds() != null) {
          List<UUID> ids = dto.getPassengerIds().stream().map(UUID::fromString).toList();
          List<PeopleTenant> passengers = peopleTenantRepository.findAllById(ids);
          trip.setPassengers(passengers);
      }

    // Stops
    List<TripStop> stops = new ArrayList<>();
    if (dto.getStops() != null) {
        for (TripStopRequestDTO stopDto : dto.getStops()) {
          TripStop stop = mapper.map(stopDto, TripStop.class);
          stop.setTrip(trip);
                    boolean hasExactCoordinates = stopDto.getLatitude() != null && stopDto.getLongitude() != null;

          if (stop.getLatitude() == null || stop.getLongitude() == null) {
            // Fallback from backend resolver (implement this method/service)
            double[] coords = findLatLngFromBackend(stopDto); // [lat, lng]

            if (coords != null) {
                if (stop.getLatitude() == null) stop.setLatitude(coords[0]);
                if (stop.getLongitude() == null) stop.setLongitude(coords[1]);
            }
          }

          stop.setAccurate(hasExactCoordinates);

          stops.add(stop);
        }
    }

      trip.setStops(stops);

      return tripRepository.save(trip);
  }

  @Transactional(readOnly = true)
  public Page<BookingResponseDTO> getBookings(UUID tenantId, Pageable pageable) {

      Page<Booking> bookings = bookingRepository.findByTenant_Id(tenantId, pageable);

      return bookings.map(BookingResponseMapper::toDTO);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public BookingResponseDTO updateTripInBooking(
      UUID bookingId,
      UUID tripId,
      TripUpdateRequestDTO dto,
      Tenant tokenTenant,
      UUID updatedBy
  ) {
      if (tokenTenant == null) {
          throw new RuntimeException("Tenant not found in token");
      }

      Booking booking = bookingRepository.findById(bookingId)
          .orElseThrow(() -> new RuntimeException("Booking not found"));

      if (booking.getTenant() == null || !booking.getTenant().getId().equals(tokenTenant.getId())) {
          throw new RuntimeException("You cannot update trip for another tenant booking");
      }

      Trip trip = booking.getTrips().stream()
          .filter(t -> t.getId().equals(tripId))
          .findFirst()
          .orElseThrow(() -> new RuntimeException("Trip not found in this booking"));

      if (dto.getVendorId() != null) {
          Tenant vendor = tenantRepository.findById(UUID.fromString(dto.getVendorId()))
              .orElseThrow(() -> new RuntimeException("Invalid vendorId"));
          trip.setVendor(vendor);
      }

      if (dto.getDutyTypeId() != null) {
          DutyType dutyType = dutyTypeRepository.findById(UUID.fromString(dto.getDutyTypeId()))
              .orElseThrow(() -> new RuntimeException("Invalid dutyTypeId"));
          trip.setDutyType(dutyType);
      }
      if (dto.getVehicleTypeId() != null) {
          VehicleType vehicleType = vehicleTypeRepository.findById(UUID.fromString(dto.getVehicleTypeId()))
              .orElseThrow(() -> new RuntimeException("Invalid vehicleTypeId"));
          trip.setVehicleType(vehicleType);
      }

      if (dto.getDriverId() != null) {
          Driver driver = driverRepository.findById(UUID.fromString(dto.getDriverId()))
              .orElseThrow(() -> new RuntimeException("Invalid driverId"));
          trip.setDriver(driver);
      }

      if (dto.getVehicleId() != null) {
          Vehicle vehicle = vehicleRepository.findById(UUID.fromString(dto.getVehicleId()))
              .orElseThrow(() -> new RuntimeException("Invalid vehicleId"));
          trip.setVehicle(vehicle);
      }

      if (dto.getNotes() != null) {
          trip.setNotes(dto.getNotes());
      }

      if (dto.getStops() != null) {
          trip.getStops().clear();
          for (TripStopRequestDTO stopDto : dto.getStops()) {
              TripStop stop = mapper.map(stopDto, TripStop.class);
              stop.setTrip(trip);
              boolean hasExactCoordinates = stopDto.getLatitude() != null && stopDto.getLongitude() != null;

              if (stop.getLatitude() == null || stop.getLongitude() == null) {
                  double[] coords = findLatLngFromBackend(stopDto);
                  if (coords != null) {
                      if (stop.getLatitude() == null) {
                          stop.setLatitude(coords[0]);
                      }
                      if (stop.getLongitude() == null) {
                          stop.setLongitude(coords[1]);
                      }
                  }
              }

              stop.setAccurate(hasExactCoordinates);

              trip.getStops().add(stop);
          }
      }

      trip.setUpdatedBy(updatedBy.toString());
      tripRepository.save(trip);

      return BookingResponseMapper.toDTO(booking);
  }

  private double[] findLatLngFromBackend(TripStopRequestDTO stopDto) {
      String query = buildGeocodeQuery(stopDto);
      if (query == null || query.isBlank()) {
          return null;
      }

      try {
          String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
          String requestUrl = geocodeBaseUrl + "/search?format=json&limit=1&q=" + encodedQuery;

          HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create(requestUrl))
              .header("Accept", "application/json")
              .header("User-Agent", "trip-sheet-backend/1.0")
              .GET()
              .build();

          HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

          if (response.statusCode() < 200 || response.statusCode() >= 300) {
              return null;
          }

          JsonNode root = objectMapper.readTree(response.body());
          if (!root.isArray() || root.isEmpty()) {
              return null;
          }

          JsonNode firstResult = root.get(0);
          JsonNode latNode = firstResult.get("lat");
          JsonNode lonNode = firstResult.get("lon");

          if (latNode == null || lonNode == null) {
              return null;
          }

          return new double[] {
              Double.parseDouble(latNode.asText()),
              Double.parseDouble(lonNode.asText())
          };
      } catch (Exception ex) {
          return null;
      }
  }

  private String buildGeocodeQuery(TripStopRequestDTO stopDto) {
      if (stopDto.getFormattedAddress() != null && !stopDto.getFormattedAddress().isBlank()) {
          return stopDto.getFormattedAddress();
      }

      if (stopDto.getAddressText() != null && !stopDto.getAddressText().isBlank()) {
          return stopDto.getAddressText();
      }

      return null;
  }



}
