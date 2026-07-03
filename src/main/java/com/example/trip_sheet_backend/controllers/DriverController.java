package com.example.trip_sheet_backend.controllers;

import com.example.trip_sheet_backend.repositories.DriverTenantMappingRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.GlobalBaseController;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCodeLookupResponseDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCreateOrLinkRequestDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCreateOrLinkResponseDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverTenantLinkRequestDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverSetPasswordRequestDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverTenantLinkRequestByTenantDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverTenantResponseDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverUpdateRequestDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleDriverLinkRequestDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleDriverMappingResponseDto;
import com.example.trip_sheet_backend.dtos.TenantVehiclesDtos.TenantVehiclesDto;
import com.example.trip_sheet_backend.dtos.TenantVehiclesDtos.VehiclesDto;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.DriverTenantMapping;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.Vehicle;
import com.example.trip_sheet_backend.models.VehicleDriverMapping;
import com.example.trip_sheet_backend.models.VehicleTenantMapping;
import com.example.trip_sheet_backend.repositories.DriverRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.VehicleRepository;
import com.example.trip_sheet_backend.repositories.VehicleTenantMappingRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.DriverService.DriverServiceImp;
import com.example.trip_sheet_backend.services.VehicleDriverService.VehicleDriverServiceImp;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/drivers")
public class DriverController extends GlobalBaseController<Driver, UUID> {
  private final DriverTenantMappingRepository driverTenantMappingRepository;
  private final DriverServiceImp driverService;
  private final VehicleDriverServiceImp vehicleDriverService;
  private final DriverRepository driverRepository;
  private final TenantRepository tenantRepository;
  private final VehicleTenantMappingRepository vehicleTenantMappingRepository;
  private final ObjectMapper objectMapper;
  private final VehicleRepository vehicleRepository;

  public DriverController(
      DriverServiceImp driverService,
      VehicleDriverServiceImp vehicleDriverService,
      DriverRepository driverRepository,
      TenantRepository tenantRepository, DriverTenantMappingRepository driverTenantMappingRepository,
      VehicleTenantMappingRepository vehicleTenantMappingRepository,
      ObjectMapper objectMapper,
      VehicleRepository vehicleRepository
  ) {
    super(driverService);
    this.driverService = driverService;
    this.vehicleDriverService = vehicleDriverService;
    this.driverRepository = driverRepository;
    this.tenantRepository = tenantRepository;
    this.driverTenantMappingRepository = driverTenantMappingRepository;
    this.vehicleTenantMappingRepository = vehicleTenantMappingRepository;
    this.objectMapper = objectMapper;
    this.vehicleRepository = vehicleRepository;
  }

  @PostMapping("/create")
  public ResponseEntity<ApiResponse<DriverCreateOrLinkResponseDto>> createDriver(
      @Valid @RequestBody DriverCreateOrLinkRequestDto body,
      HttpServletRequest request
  ) {
    UUID createdBy = (UUID) request.getAttribute("createdBy");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    if (tokenTenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    DriverCreateOrLinkResponseDto response = driverService.createOrLinkDriver(body, tokenTenant, createdBy);
    String message = switch (response.getAction()) {
      case "DRIVER_CREATED" -> "Driver created and linked successfully";
      case "DRIVER_LINKED" -> "Existing driver linked successfully";
      case "DRIVER_ALREADY_LINKED" -> "Driver is already linked with this tenant";
      default -> "Driver already exists. Use the returned unique code to link";
    };

    return ResponseEntity.ok(new ApiResponse<>(true, message, response));
  }

  @PreAuthorize("hasAuthority('CAN_CREATE_DRIVER')")
  @PostMapping("/link")
  public ResponseEntity<ApiResponse<DriverTenantResponseDto>> linkDriverWithCurrentTenant(
      @Valid @RequestBody DriverTenantLinkRequestDto body,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID createdBy = (UUID) request.getAttribute("createdBy");
    DriverTenantResponseDto response = driverService.linkDriverToCurrentTenant(tokenTenant, body.getDriverId(), createdBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Driver linked with current tenant successfully", response));
  }

  @PreAuthorize("hasAuthority('CAN_READ_DRIVER')")
  @GetMapping
  public ResponseEntity<ApiResponse<List<DriverTenantResponseDto>>> getDrivers(HttpServletRequest request) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    List<DriverTenantResponseDto> drivers = driverService.getDriversByTenant(tokenTenant);
    return ResponseEntity.ok(new ApiResponse<>(true, "Drivers fetched successfully", drivers));
  }

  @PreAuthorize("hasAuthority('CAN_READ_DRIVER')")
  @GetMapping("/search")
  public ResponseEntity<ApiResponse<Map<String, Object>>> searchDrivers(
      @RequestParam(required = false) String fullName,
      @RequestParam(required = false) String phone,
      @RequestParam(required = false) String email,
      Pageable pageable,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    Page<DriverTenantResponseDto> result = driverService.searchDriversByTenant(
        tokenTenant,
        fullName,
        phone,
        email,
        pageable
    );

    Map<String, Object> response = new HashMap<>();
    response.put("data", result.getContent());
    response.put("currentPage", result.getNumber());
    response.put("pageSize", result.getSize());
    response.put("currentPageCount", result.getNumberOfElements());
    response.put("totalItems", result.getTotalElements());
    response.put("totalPages", result.getTotalPages());
    response.put("isFirst", result.isFirst());
    response.put("isLast", result.isLast());
    response.put("hasNext", result.hasNext());
    response.put("hasPrevious", result.hasPrevious());

    return ResponseEntity.ok(new ApiResponse<>(true, "Drivers fetched successfully", response));
  }

  @GetMapping("/by-code/{uniqueCode}")
  public ResponseEntity<ApiResponse<DriverCodeLookupResponseDto>> getDriverByUniqueCode(
      @PathVariable String uniqueCode,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    DriverCodeLookupResponseDto driver = driverService.getDriverByUniqueCode(tokenTenant, uniqueCode);
    return ResponseEntity.ok(new ApiResponse<>(true, "Driver fetched successfully", driver));
  }

  @PreAuthorize("hasAuthority('CAN_READ_DRIVER')")
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<DriverTenantResponseDto>> getDriver(
      @PathVariable UUID id,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    DriverTenantResponseDto driver = driverService.getDriverByTenant(tokenTenant, id);
    return ResponseEntity.ok(new ApiResponse<>(true, "Driver fetched successfully", driver));
  }

  @PreAuthorize("hasAuthority('CAN_UPDATE_DRIVER')")
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<DriverTenantResponseDto>> updateDriver(
      @PathVariable UUID id,
      @Valid @RequestBody DriverUpdateRequestDto body,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");
    DriverTenantResponseDto driver = driverService.updateDriverByTenant(tokenTenant, id, body, updatedBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Driver updated successfully", driver));
  }

  @PreAuthorize("hasRole('DRIVER') or hasRole('SUPER_ADMIN')")
  @PatchMapping("/me")
  public ResponseEntity<ApiResponse<DriverTenantResponseDto>> updateMyDriverProfile(
      @Valid @RequestBody DriverUpdateRequestDto body,
      HttpServletRequest request
  ) {
    UserAccount currentUser = (UserAccount) request.getAttribute("user");
    if (currentUser == null) {
      throw new RuntimeException("User not found in token");
    }

    DriverTenantResponseDto driver = driverService.updateMyDriverProfile(currentUser, body);
    return ResponseEntity.ok(new ApiResponse<>(true, "Driver profile updated successfully", driver));
  }

  @PatchMapping("/{id}/inactive")
  @PreAuthorize("hasAuthority('CAN_UPDATE_DRIVER')")
  public ResponseEntity<ApiResponse<DriverTenantResponseDto>> makeDriverInactiveForTenant(
      @PathVariable UUID id,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");
    DriverTenantResponseDto driver = driverService.setDriverActiveForTenant(tokenTenant, id, false, updatedBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Driver marked inactive for current tenant", driver));
  }

  @PreAuthorize("hasAuthority('CAN_UPDATE_DRIVER')")
  @PatchMapping("/{id}/active")
  public ResponseEntity<ApiResponse<DriverTenantResponseDto>> makeDriverActiveForTenant(
      @PathVariable UUID id,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");
    DriverTenantResponseDto driver = driverService.setDriverActiveForTenant(tokenTenant, id, true, updatedBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Driver marked active for current tenant", driver));
  }

  @PreAuthorize("hasAuthority('CAN_UPDATE_DRIVER')")
  @PostMapping("/{id}/password")
  public ResponseEntity<ApiResponse<Void>> setDriverPasswordForTenant(
      @PathVariable UUID id,
      @Valid @RequestBody DriverSetPasswordRequestDto body,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");
    driverService.setDriverPasswordForTenant(tokenTenant, id, body, updatedBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Driver password set successfully and sent by email", null));
  }

  @PreAuthorize("hasAuthority('CAN_UPDATE_VEHICLEDRIVERMAPPING')")
  @PostMapping("/link-vehicle")
  public ResponseEntity<ApiResponse<VehicleDriverMappingResponseDto>> createVehicleLink(
      @Valid @RequestBody VehicleDriverLinkRequestDto body,
      HttpServletRequest request
  ) {
    VehicleDriverMappingResponseDto response = vehicleDriverService.linkDriverAndVehicle(body, request);
    return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle linked with driver successfully", response));
  }

  @PreAuthorize("hasRole('DRIVER')")
  @PostMapping("/link-tenant")
  public ResponseEntity<ApiResponse<DriverTenantResponseDto>> createTenantLink(
      @Valid @RequestBody DriverTenantLinkRequestByTenantDto body,
      HttpServletRequest request
  ) {
    UserAccount currentUser = (UserAccount) request.getAttribute("user");
    if (currentUser == null) {
      throw new RuntimeException("User not found in token");
    }

    Driver driver = driverRepository.findByAccount_Id(currentUser.getId())
        .orElseThrow(() -> new RuntimeException("Driver profile not found for current user"));

    Tenant tenant = tenantRepository.findById(body.getTenantId())
        .orElseThrow(() -> new RuntimeException("Tenant not found"));

    DriverTenantResponseDto response = driverService.linkDriverToCurrentTenant(
        tenant,
        driver.getId(),
        currentUser.getId()
    );

    return ResponseEntity.ok(new ApiResponse<>(true, "Tenant linked with driver successfully", response));
  }

  @PreAuthorize("hasRole('DRIVER')")
  @GetMapping("/tenant-vehicles/{tenantId}")
  public ResponseEntity<ApiResponse<TenantVehiclesDto>> listOfTenantVehicles(@PathVariable UUID tenantId, HttpServletRequest request) {
    UserAccount currentUser = (UserAccount) request.getAttribute("user");
    if (currentUser == null) {
      throw new RuntimeException("User not found in token");
    }
    Driver driver = driverRepository.findByAccount_Id(currentUser.getId())
        .orElseThrow(() -> new RuntimeException("Driver profile not found for current user"));

    Tenant tenant = tenantRepository.findById(tenantId)
        .orElseThrow(() -> new RuntimeException("Tenant not found"));

    DriverTenantMapping driverTenant = driverTenantMappingRepository.findByTenant_IdAndDriver_Id(tenant.getId(), driver.getId())
        .orElseThrow(() -> new RuntimeException("Driver is not linked with the specified tenant"));

    List<VehicleTenantMapping> vehicleTenantMappings = vehicleTenantMappingRepository.findByTenant_Id(tenantId);
    // return ResponseEntity.ok(new ApiResponse<>(true, "List of tenant vehicles retrieved successfully", vehicleTenantMappings));
    TenantVehiclesDto tenantVehiclesDto = new TenantVehiclesDto();
    tenantVehiclesDto.setTenantId(tenantId);
    tenantVehiclesDto.setTenantName(tenant.getTenantName());

    List<VehiclesDto> vehicles = new ArrayList<>();
    Map<String, Object> vehicleMap = new HashMap<>();
    vehicleTenantMappings.forEach(vt -> {
        vehicleMap.put("id", vt.getVehicle().getId());
        vehicleMap.put("vehicleNumber", vt.getVehicle().getVehicleNumber());
        vehicleMap.put("vehicleUniqueCode", vt.getVehicle().getVehicleUniqueCode());
        vehicleMap.put("vehicleType", vt.getVehicle().getVehicleType());
        vehicles.add(objectMapper.convertValue(vehicleMap, VehiclesDto.class));
    });

    tenantVehiclesDto.setVehicles(vehicles);
    
    return ResponseEntity.ok(new ApiResponse<>(true, "List of tenant vehicles retrieved successfully", tenantVehiclesDto));
  }

  @PreAuthorize("hasRole('DRIVER')")
  @PatchMapping("/link-vehicle/{vehicleId}")
  public ResponseEntity<ApiResponse<VehicleDriverMapping>> linkWithVehicle(@PathVariable UUID vehicleId, HttpServletRequest request) {
    UserAccount currentUser = (UserAccount) request.getAttribute("user");
    if (currentUser == null) {
      throw new RuntimeException("User not found in token");
    }

    if (currentUser.getTenant() == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    Driver driver = driverRepository.findByAccount_Id(currentUser.getId())
        .orElseThrow(() -> new RuntimeException("Driver profile not found for current user"));

    Tenant tenant = tenantRepository.findById(currentUser.getTenant().getId())
        .orElseThrow(() -> new RuntimeException("Tenant not found"));

    Vehicle vehicle = vehicleRepository.findById(vehicleId)
        .orElseThrow(() -> new RuntimeException("Vehicle not found"));
    
    VehicleDriverMapping vD = vehicleDriverService.linkDriverWithVehicle(driver, vehicle, currentUser, tenant);
    return ResponseEntity.ok(new ApiResponse<>(true, "Driver linked with vehicle successfully", vD));
  }

  @PreAuthorize("hasRole('DRIVER')")
  @PatchMapping("/unlink-vehicle/{vehicleId}")
  public ResponseEntity<ApiResponse<VehicleDriverMapping>> unlinkWithVehicle(@PathVariable UUID vehicleId, HttpServletRequest request) {
    UserAccount currentUser = (UserAccount) request.getAttribute("user");
    if (currentUser == null) {
      throw new RuntimeException("User not found in token");
    }

    if (currentUser.getTenant() == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    Driver driver = driverRepository.findByAccount_Id(currentUser.getId())
        .orElseThrow(() -> new RuntimeException("Driver profile not found for current user"));

    Tenant tenant = tenantRepository.findById(currentUser.getTenant().getId())
        .orElseThrow(() -> new RuntimeException("Tenant not found"));

    Vehicle vehicle = vehicleRepository.findById(vehicleId)
        .orElseThrow(() -> new RuntimeException("Vehicle not found"));
    
    VehicleDriverMapping vD = vehicleDriverService.unlinkDriverWithVehicle(driver.getId(), vehicle.getId(), currentUser.getId(), tenant.getId());
    return ResponseEntity.ok(new ApiResponse<>(true, "Driver unlinked from vehicle successfully", vD));
  }


}
