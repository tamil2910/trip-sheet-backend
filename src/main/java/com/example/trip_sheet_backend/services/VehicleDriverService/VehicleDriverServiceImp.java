package com.example.trip_sheet_backend.services.VehicleDriverService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCreateOrLinkRequestDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCreateOrLinkResponseDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.DriverInfoDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleCreateOrLinkResponseDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleDriverCreateRequestDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleDriverLinkRequestDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleDriverMappingResponseDto;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.DriverTenantMapping;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.Vehicle;
import com.example.trip_sheet_backend.models.VehicleDriverMapping;
import com.example.trip_sheet_backend.models.VehicleTenantMapping;
import com.example.trip_sheet_backend.repositories.DriverTenantMappingRepository;
import com.example.trip_sheet_backend.repositories.VehicleDriverMappingRepository;
import com.example.trip_sheet_backend.repositories.VehicleTenantMappingRepository;
import com.example.trip_sheet_backend.services.DriverService.DriverService;
import com.example.trip_sheet_backend.services.VehicleService.VehicleService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class VehicleDriverServiceImp extends BaseServiceImp<VehicleDriverMapping, UUID> implements VehicleDriverService {

  private final VehicleDriverMappingRepository repository;
  private final VehicleService vehicleService;
  private final DriverService driverService;
  private final DriverTenantMappingRepository driverTenantMappingRepository;
  private final VehicleTenantMappingRepository vehicleTenantMappingRepository;

  public VehicleDriverServiceImp(
      VehicleDriverMappingRepository repository,
      VehicleService vehicleService,
      DriverService driverService,
      DriverTenantMappingRepository driverTenantMappingRepository,
      VehicleTenantMappingRepository vehicleTenantMappingRepository
  ) {
    super(repository);
    this.repository = repository;
    this.vehicleService = vehicleService;
    this.driverService = driverService;
    this.driverTenantMappingRepository = driverTenantMappingRepository;
    this.vehicleTenantMappingRepository = vehicleTenantMappingRepository;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public VehicleDriverMappingResponseDto createVehicleAndDriver(VehicleDriverCreateRequestDto dto, HttpServletRequest request) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID createdBy = (UUID) request.getAttribute("createdBy");

    validateTenant(tokenTenant);

    VehicleCreateOrLinkResponseDto vehicleResponse = vehicleService.createOrLinkVehicle(
        dto.getVehicle_info(),
        tokenTenant,
        createdBy
    );

    DriverCreateOrLinkRequestDto driverRequest = mapDriverRequest(dto.getDriver_info());
    DriverCreateOrLinkResponseDto driverResponse = driverService.createOrLinkDriver(driverRequest, tokenTenant, createdBy);

    if ("DRIVER_EXISTS".equals(driverResponse.getAction()) && Boolean.FALSE.equals(driverResponse.getLinkedToTenant())) {
      DriverCreateOrLinkRequestDto linkRequest = new DriverCreateOrLinkRequestDto();
      linkRequest.setUniqueCode(driverResponse.getUniqueCode());
      linkRequest.setUsername(driverRequest.getUsername());
      linkRequest.setEmail(driverRequest.getEmail());
      linkRequest.setPhone(driverRequest.getPhone());
      linkRequest.setPassword(driverRequest.getPassword());
      driverResponse = driverService.createOrLinkDriver(linkRequest, tokenTenant, createdBy);
    }

    VehicleDriverMapping mapping = createOrActivateVehicleDriverMapping(
        tokenTenant,
        vehicleResponse.getVehicle().getId(),
        driverResponse.getDriver().getId(),
        createdBy
    );

    return VehicleDriverMappingResponseDto.fromEntity(mapping);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public VehicleDriverMappingResponseDto linkDriverAndVehicle(VehicleDriverLinkRequestDto dto, HttpServletRequest request) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");

    VehicleDriverMapping mapping = createOrActivateVehicleDriverMapping(
        tokenTenant,
        dto.getVehicleId(),
        dto.getDriverId(),
        updatedBy
    );

    return VehicleDriverMappingResponseDto.fromEntity(mapping);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public VehicleDriverMappingResponseDto unlinkDriverAndVehicle(VehicleDriverLinkRequestDto dto, HttpServletRequest request) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");

    validateTenant(tokenTenant);

    VehicleDriverMapping mapping = repository.findByDriverIdAndVehicleIdAndTenantId(
        dto.getDriverId(),
        dto.getVehicleId(),
        tokenTenant.getId()
    ).orElseThrow(() -> new RuntimeException("Vehicle-driver mapping not found for this tenant"));

    mapping.setIsActive(false);
    mapping.setUpdatedBy(updatedBy.toString());
    return VehicleDriverMappingResponseDto.fromEntity(repository.save(mapping));
  }

  @Override
  public Map<String, Object> getAllMappedVehiclesWithDriver(UUID tenantId, Pageable pageable) {
    Page<VehicleDriverMappingResponseDto> result = repository
      .findAllMappedVehiclesWithDriverByTenantId(tenantId, pageable)
      .map(VehicleDriverMappingResponseDto::fromEntity);

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

    return response;
  }

  private VehicleDriverMapping createOrActivateVehicleDriverMapping(
      Tenant tokenTenant,
      UUID vehicleId,
      UUID driverId,
      UUID updatedBy
  ) {
    validateTenant(tokenTenant);

    DriverTenantMapping driverTenantMapping = driverTenantMappingRepository
        .findByTenant_IdAndDriver_Id(tokenTenant.getId(), driverId)
        .orElseThrow(() -> new RuntimeException("Driver is not linked to this tenant"));

    VehicleTenantMapping vehicleTenantMapping = vehicleTenantMappingRepository
        .findByTenant_IdAndVehicle_Id(tokenTenant.getId(), vehicleId)
        .orElseThrow(() -> new RuntimeException("Vehicle is not linked to this tenant"));

    Optional<VehicleDriverMapping> samePair = repository.findByDriverIdAndVehicleIdAndTenantId(
        driverId,
        vehicleId,
        tokenTenant.getId()
    );

    if (samePair.isPresent()) {
      VehicleDriverMapping mapping = samePair.get();
      mapping.setIsActive(true);
      mapping.setUpdatedBy(updatedBy.toString());
      return repository.save(mapping);
    }

    repository.findByDriverIdAndTenantIdAndIsActive(driverId, tokenTenant.getId(), true)
        .ifPresent(existing -> {
          throw new RuntimeException("Driver already has an active vehicle mapping in this tenant");
        });

    repository.findByVehicleIdAndTenantIdAndIsActive(vehicleId, tokenTenant.getId(), true)
        .ifPresent(existing -> {
          throw new RuntimeException("Vehicle already has an active driver mapping in this tenant");
        });

    VehicleDriverMapping mapping = new VehicleDriverMapping();
    mapping.setDriver(driverTenantMapping.getDriver());
    mapping.setVehicle(vehicleTenantMapping.getVehicle());
    mapping.setTenant(tokenTenant);
    mapping.setIsActive(true);
    mapping.setCreatedBy(updatedBy.toString());
    mapping.setUpdatedBy(updatedBy.toString());
    return repository.save(mapping);
  }

  private DriverCreateOrLinkRequestDto mapDriverRequest(DriverInfoDto body) {
    DriverCreateOrLinkRequestDto request = new DriverCreateOrLinkRequestDto();
    request.setUniqueCode(body.getUniqueCode());
    request.setUsername(body.getUsername());
    request.setFullName(body.getFullName());
    request.setEmail(body.getEmail());
    request.setPhone(body.getPhone());
    request.setPassword(body.getPassword());
    request.setProfilePicture(body.getProfilePicture());
    request.setLicenseNumber(body.getLicenseNumber());
    request.setLicenseExpiry(body.getLicenseExpiry());
    request.setInsuranceNumber(body.getInsuranceNumber());
    request.setInsuranceExpiry(body.getInsuranceExpiry());
    request.setPoliceVerificationId(body.getPoliceVerificationId());
    request.setBloodGroup(body.getBloodGroup());
    request.setRating(body.getRating());
    request.setActive(body.getActive());
    request.setAvailable(body.getAvailable());

    if (body.getDriverType() != null) {
      request.setDriverType(Driver.DriverType.valueOf(body.getDriverType().name()));
    }

    return request;
  }

  private void validateTenant(Tenant tokenTenant) {
    if (tokenTenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public VehicleDriverMapping linkDriverWithVehicle(Driver driver, Vehicle vehicle, UserAccount updatedBy, Tenant tenant) {

    Boolean isExist = repository.existsByDriverIdAndVehicleIdNotAndTenantIdAndIsActiveTrue(driver.getId(), vehicle.getId(), tenant.getId());
    if (isExist) {
      throw new RuntimeException("Driver is already linked with another vehicle for the given tenant");
    }

    Boolean isExistVehicle = repository.existsByDriverIdNotAndVehicleIdAndTenantIdAndIsActiveTrue(driver.getId(), vehicle.getId(), tenant.getId());

    if (isExistVehicle) {
      throw new RuntimeException("Vehicle is already linked with another driver for the given tenant");
    }

    repository.findByDriverIdAndVehicleIdAndTenantIdAndIsActiveFalse(driver.getId(), vehicle.getId(), tenant.getId())
    .ifPresentOrElse(existing -> {
      existing.setIsActive(true);
      existing.setUpdatedBy(updatedBy.getId().toString());
      repository.saveAndFlush(existing);
    }, () -> {
      VehicleDriverMapping newMapping = new VehicleDriverMapping();
      newMapping.setDriver(driver);
      newMapping.setVehicle(vehicle);
      newMapping.setTenant(tenant);
      newMapping.setIsActive(true);
      newMapping.setCreatedBy(updatedBy.getId().toString());
      newMapping.setUpdatedBy(updatedBy.getId().toString());
      repository.saveAndFlush(newMapping);
    });

    return repository.findByDriverIdAndVehicleIdAndTenantIdAndIsActiveTrue(driver.getId(), vehicle.getId(), tenant.getId())
          .orElseThrow(() -> new RuntimeException("Failed to retrieve the updated mapping after linking"));
  }

  @Transactional(rollbackFor = Exception.class)
  public VehicleDriverMapping unlinkDriverWithVehicle(UUID driverId, UUID vehicleId, UUID updatedBy, UUID tenantId) {

    Boolean isExist = repository.existsByDriverIdAndVehicleIdAndTenantIdAndIsActiveTrue(driverId, vehicleId, tenantId);
    if (!isExist) {
      throw new RuntimeException("Driver is not linked with the vehicle for the given tenant");
    }

    repository.findByDriverIdAndVehicleIdAndTenantIdAndIsActiveTrue(driverId, vehicleId, tenantId)
    .ifPresent(existing -> {

      existing.setIsActive(false);
      existing.setUpdatedBy(updatedBy.toString());
      repository.saveAndFlush(existing);
    });

    return repository.findByDriverIdAndVehicleIdAndTenantIdAndIsActiveFalse(driverId, vehicleId, tenantId)
          .orElseThrow(() -> new RuntimeException("Failed to retrieve the updated mapping after unlinking"));
  }

  @Transactional(rollbackFor = Exception.class)
  public VehicleDriverMapping getCurrentActiveVehicleOfDriver(UUID driverId, Tenant tenant) {

    return repository.findByDriverIdAndTenantIdAndIsActiveTrue(driverId, tenant.getId())
          .orElseThrow(() -> new RuntimeException("Failed to retrieve the active mapping for the driver"));
  }


}
