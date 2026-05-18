package com.example.trip_sheet_backend.services.VehicleService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;

import com.example.trip_sheet_backend.common.services.GlobalBaseServiceImp;
import com.example.trip_sheet_backend.common.services.UniqueCodeGeneratorService;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleCodeLookupResponseDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleCreateOrLinkResponseDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleInfoDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleTenantResponseDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleUpdateRequestDto;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Vehicle;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.models.VehicleTenantMapping;
import com.example.trip_sheet_backend.models.VehicleTypeCustomName;
import com.example.trip_sheet_backend.repositories.VehicleRepository;
import com.example.trip_sheet_backend.repositories.VehicleTenantMappingRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeCustomNamesRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeRepository;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Service
public class VehicleServiceImp extends GlobalBaseServiceImp<Vehicle, UUID> implements VehicleService {
  private static final String VEHICLE_CODE_PREFIX = "VEH";

  private final VehicleRepository repository;
  private final VehicleTenantMappingRepository vehicleTenantMappingRepository;
  private final VehicleTypeCustomNamesRepository vehicleTypeCustomNamesRepository;
  private final VehicleTypeRepository vehicleTypeRepository;
  private final UniqueCodeGeneratorService uniqueCodeGeneratorService;
  private final ModelMapper mapper;

  public VehicleServiceImp(
      VehicleRepository repository,
      VehicleTenantMappingRepository vehicleTenantMappingRepository,
      VehicleTypeCustomNamesRepository vehicleTypeCustomNamesRepository,
      VehicleTypeRepository vehicleTypeRepository,
      UniqueCodeGeneratorService uniqueCodeGeneratorService,
      ModelMapper mapper
  ) {
    super(repository);
    this.repository = repository;
    this.vehicleTenantMappingRepository = vehicleTenantMappingRepository;
    this.vehicleTypeCustomNamesRepository = vehicleTypeCustomNamesRepository;
    this.vehicleTypeRepository = vehicleTypeRepository;
    this.uniqueCodeGeneratorService = uniqueCodeGeneratorService;
    this.mapper = mapper;
  }

  @Override
  public Vehicle findByVehicleNumberAndTenantId(String vehicleNumber, UUID tenantId) {
    return repository.findByVehicleNumberAndTenant_Id(vehicleNumber, tenantId);
  }

  @Override
  public Vehicle findByVehicleNumber(String vehicleNumber) {
    return repository.findByVehicleNumber(vehicleNumber);
  }

  @Transactional(readOnly = true)
  @Override
  public Page<VehicleTenantResponseDto> getVehiclesByTenant(Tenant tokenTenant, Map<String, Object> filters, Pageable pageable) {
    validateTenant(tokenTenant);

    Specification<VehicleTenantMapping> specification = buildVehicleTenantSpecification(tokenTenant.getId(), filters);
    return vehicleTenantMappingRepository.findAll(specification, pageable)
        .map(VehicleTenantResponseDto::fromEntity);
  }

  @Transactional(readOnly = true)
  @Override
  public VehicleTenantResponseDto getVehicleByTenant(Tenant tokenTenant, UUID vehicleId) {
    VehicleTenantMapping mapping = getTenantVehicleMapping(tokenTenant, vehicleId);
    return VehicleTenantResponseDto.fromEntity(mapping);
  }

  @Transactional(readOnly = true)
  @Override
  public VehicleCodeLookupResponseDto getVehicleByUniqueCode(Tenant tokenTenant, String uniqueCode) {
    validateTenant(tokenTenant);
    String normalizedCode = normalizeCode(uniqueCode);
    if (normalizedCode == null) {
      throw new RuntimeException("Vehicle unique code is required");
    }

    Vehicle vehicle = repository.findByVehicleUniqueCode(normalizedCode)
        .orElseThrow(() -> new RuntimeException("Vehicle not found for unique code: " + normalizedCode));
    vehicle = ensureVehicleUniqueCode(vehicle);

    VehicleTenantMapping mapping = vehicleTenantMappingRepository
        .findByVehicle_IdAndTenant_Id(vehicle.getId(), tokenTenant.getId())
        .orElse(null);

    return VehicleCodeLookupResponseDto.fromEntity(vehicle, tokenTenant, mapping);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public VehicleTenantResponseDto updateVehicleByTenant(
      Tenant tokenTenant,
      UUID vehicleId,
      VehicleUpdateRequestDto body,
      UUID updatedBy
  ) {
    VehicleTenantMapping mapping = getTenantVehicleMapping(tokenTenant, vehicleId);
    Vehicle vehicle = ensureVehicleUniqueCode(mapping.getVehicle());

    String normalizedVehicleNumber = normalize(body.getVehicleNumber());
    if (normalizedVehicleNumber != null) {
      VehicleTenantMapping existingByNumber = vehicleTenantMappingRepository
          .findByTenant_IdAndVehicle_VehicleNumber(tokenTenant.getId(), normalizedVehicleNumber)
          .orElse(null);
      if (existingByNumber != null && !existingByNumber.getVehicle().getId().equals(vehicle.getId())) {
        throw new RuntimeException("Vehicle number already exists for this tenant");
      }
      vehicle.setVehicleNumber(normalizedVehicleNumber);
    }

    String normalizedModelName = normalizeText(body.getModelName());
    if (normalizedModelName != null) {
      VehicleTenantMapping existingByModelName = vehicleTenantMappingRepository
          .findByTenant_IdAndVehicle_ModelNameIgnoreCase(tokenTenant.getId(), normalizedModelName)
          .orElse(null);
      if (existingByModelName != null && !existingByModelName.getVehicle().getId().equals(vehicle.getId())) {
        throw new RuntimeException("Model name already exists for this tenant");
      }
      vehicle.setModelName(normalizedModelName);
    }

    if (body.getVehicleTypeId() != null) {
      vehicle.setVehicleType(resolveVehicleType(body.getVehicleTypeId(), tokenTenant));
    }

    if (body.getFuelType() != null) vehicle.setFuelType(body.getFuelType());
    if (body.getColour() != null) vehicle.setColour(body.getColour());
    if (body.getDescription() != null) vehicle.setDescription(body.getDescription());
    if (body.getLeftSideUrl() != null) vehicle.setLeftSideUrl(body.getLeftSideUrl());
    if (body.getRightSideUrl() != null) vehicle.setRightSideUrl(body.getRightSideUrl());
    if (body.getBackSideUrl() != null) vehicle.setBackSideUrl(body.getBackSideUrl());
    if (body.getFrontSideUrl() != null) vehicle.setFrontSideUrl(body.getFrontSideUrl());
    if (body.getVehProfileUrl() != null) vehicle.setVehProfileUrl(body.getVehProfileUrl());
    if (body.getRegisteredOwnerName() != null) vehicle.setRegisteredOwnerName(body.getRegisteredOwnerName());
    if (body.getRegistrationDate() != null) vehicle.setRegistrationDate(body.getRegistrationDate());
    if (body.getChassisNumber() != null) vehicle.setChassisNumber(body.getChassisNumber());
    if (body.getEngineNumber() != null) vehicle.setEngineNumber(body.getEngineNumber());
    if (body.getInsuranceCompanyName() != null) vehicle.setInsuranceCompanyName(body.getInsuranceCompanyName());
    if (body.getPolicyNumber() != null) vehicle.setPolicyNumber(body.getPolicyNumber());
    if (body.getIssueDate() != null) vehicle.setIssueDate(body.getIssueDate());
    if (body.getDueDate() != null) vehicle.setDueDate(body.getDueDate());
    if (body.getPremiumAmount() != null) vehicle.setPremiumAmount(body.getPremiumAmount());
    if (body.getCoverAmount() != null) vehicle.setCoverAmount(body.getCoverAmount());

    vehicle.setUpdatedBy(updatedBy.toString());
    Vehicle savedVehicle = update(vehicle.getId(), vehicle);
    mapping.setVehicle(savedVehicle);
    mapping.setUpdatedBy(updatedBy.toString());
    VehicleTenantMapping savedMapping = vehicleTenantMappingRepository.save(mapping);
    return VehicleTenantResponseDto.fromEntity(savedMapping);
  }

  @Override
  public Vehicle create(Vehicle payload) {
    ensureVehicleUniqueCode(payload);
    return repository.save(payload);
  }

  @Override
  public Vehicle update(UUID id, Vehicle payload) {
    Vehicle existing = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("Vehicle not found"));

    if (payload.getId() == null) {
      payload.setId(existing.getId());
    }

    if (payload.getVehicleUniqueCode() == null) {
      payload.setVehicleUniqueCode(existing.getVehicleUniqueCode());
    }

    ensureVehicleUniqueCode(payload);
    return repository.save(payload);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public VehicleCreateOrLinkResponseDto createOrLinkVehicle(VehicleInfoDto body, Tenant tokenTenant, UUID createdBy) {
    if (tokenTenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    String normalizedVehicleNumber = normalize(body.getVehicleNumber());
    if (normalizedVehicleNumber == null) {
      throw new RuntimeException("Vehicle number is required");
    }

    Vehicle existingVehicle = repository.findByTenant_IdAndVehicleNumber(
      tokenTenant.getId(),
      normalizedVehicleNumber
    ).orElse(null);

    if (existingVehicle != null) {
      existingVehicle = ensureVehicleUniqueCode(existingVehicle);

      VehicleTenantMapping existingMapping = vehicleTenantMappingRepository
          .findByVehicle_IdAndTenant_Id(existingVehicle.getId(), tokenTenant.getId())
          .orElse(null);

      if (existingMapping != null) {
        return VehicleCreateOrLinkResponseDto.fromEntity(
            "VEHICLE_ALREADY_LINKED",
            existingVehicle,
            existingMapping,
            true
        );
      }

      VehicleTenantMapping linkedMapping = createVehicleTenantMapping(existingVehicle, tokenTenant, createdBy);
      return VehicleCreateOrLinkResponseDto.fromEntity("VEHICLE_LINKED", existingVehicle, linkedMapping, true);
    }

    Vehicle vehicle = mapper.map(body, Vehicle.class);
    vehicle.setTenant(tokenTenant);
    vehicle.setVehicleNumber(normalizedVehicleNumber);
    vehicle.setVehicleType(resolveVehicleType(body.getVehicleTypeId(), tokenTenant));
    vehicle.setCreatedBy(createdBy.toString());
    vehicle.setUpdatedBy(createdBy.toString());
    vehicle.setIsActive(true);

    ensureVehicleUniqueCode(vehicle);
    Vehicle savedVehicle = repository.save(vehicle);
    VehicleTenantMapping mapping = createVehicleTenantMapping(savedVehicle, tokenTenant, createdBy);
    return VehicleCreateOrLinkResponseDto.fromEntity("VEHICLE_CREATED", savedVehicle, mapping, false);
  }

  private VehicleTenantMapping createVehicleTenantMapping(Vehicle vehicle, Tenant tenant, UUID createdBy) {
    return vehicleTenantMappingRepository.findByVehicle_IdAndTenant_Id(vehicle.getId(), tenant.getId())
        .orElseGet(() -> {
          VehicleTenantMapping mapping = new VehicleTenantMapping();
          mapping.setVehicle(vehicle);
          mapping.setTenant(tenant);
          mapping.setActive(true);
          mapping.setLinkedAt(Instant.now().toEpochMilli());
          mapping.setCreatedBy(createdBy.toString());
          mapping.setUpdatedBy(createdBy.toString());
          return vehicleTenantMappingRepository.save(mapping);
        });
  }

  private VehicleTenantMapping getTenantVehicleMapping(Tenant tokenTenant, UUID vehicleId) {
    validateTenant(tokenTenant);
    return vehicleTenantMappingRepository.findByTenant_IdAndVehicle_Id(tokenTenant.getId(), vehicleId)
        .orElseThrow(() -> new RuntimeException("Vehicle not found for this tenant"));
  }

  private void validateTenant(Tenant tokenTenant) {
    if (tokenTenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }
  }

  private VehicleType resolveVehicleType(String vehicleTypeIdentifier, Tenant tokenTenant) {
    validateTenant(tokenTenant);

    if (vehicleTypeIdentifier == null || vehicleTypeIdentifier.trim().isEmpty()) {
      throw new RuntimeException("Vehicle type ID is required");
    }

    UUID vehicleTypeId;
    try {
      vehicleTypeId = UUID.fromString(vehicleTypeIdentifier);
    } catch (IllegalArgumentException ex) {
      throw new RuntimeException("Invalid vehicle type ID");
    }

    VehicleTypeCustomName customName = vehicleTypeCustomNamesRepository
        .findByIdAndTenant_Id(vehicleTypeId, tokenTenant.getId())
        .orElseGet(() -> vehicleTypeCustomNamesRepository
            .findByVehicleType_IdAndTenant_Id(vehicleTypeId, tokenTenant.getId())
            .orElse(null));

    if (customName != null) {
      return customName.getVehicleType();
    }

    return vehicleTypeRepository.findById(vehicleTypeId)
        .orElseThrow(() -> new RuntimeException("Vehicle type not found for ID: " + vehicleTypeIdentifier));
  }

  private Vehicle ensureVehicleUniqueCode(Vehicle vehicle) {
    String existingCode = vehicle.getVehicleUniqueCode();
    if (existingCode != null && !existingCode.trim().isEmpty()) {
      return vehicle;
    }

    vehicle.setVehicleUniqueCode(
        uniqueCodeGeneratorService.generateUniqueCode(VEHICLE_CODE_PREFIX, repository::existsByVehicleUniqueCode)
    );

    if (vehicle.getId() != null) {
      return repository.save(vehicle);
    }

    return vehicle;
  }

  private String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed.toUpperCase(Locale.ROOT);
  }

  private String normalizeCode(String value) {
    return normalize(value);
  }

  private String normalizeText(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private Specification<VehicleTenantMapping> buildVehicleTenantSpecification(UUID tenantId, Map<String, Object> filters) {
    return (root, query, cb) -> {
      query.distinct(true);

      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.join("tenant").get("id"), tenantId));

      if (filters == null || filters.isEmpty()) {
        return cb.and(predicates.toArray(new Predicate[0]));
      }

      filters.forEach((key, value) -> {
        if (value == null || value.toString().trim().isEmpty() || isReservedFilter(key)) {
          return;
        }

        try {
          Path<?> path = resolveVehicleFilterPath(root, key);
          if (path == null) {
            return;
          }

          Class<?> fieldType = path.getJavaType();
          String stringValue = value.toString().trim();

          if (fieldType.equals(String.class)) {
            predicates.add(cb.like(cb.lower(path.as(String.class)), "%" + stringValue.toLowerCase(Locale.ROOT) + "%"));
          } else if (fieldType.equals(UUID.class)) {
            predicates.add(cb.equal(path, UUID.fromString(stringValue))); 
          } else if (fieldType.isEnum()) {
            Object enumValue = Arrays.stream(fieldType.getEnumConstants())
                .filter(enumConstant -> enumConstant.toString().equalsIgnoreCase(stringValue))
                .findFirst()
                .orElse(null);
            if (enumValue != null) {
              predicates.add(cb.equal(path, enumValue));
            }
          } else {
            predicates.add(cb.equal(path, convertValue(fieldType, stringValue)));
          }
        } catch (Exception ignored) {
          // ignore invalid filters so one bad param does not break the list endpoint
        }
      });

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private boolean isReservedFilter(String key) {
    return "page".equalsIgnoreCase(key) || "size".equalsIgnoreCase(key) || "sort".equalsIgnoreCase(key);
  }

  private Path<?> resolveVehicleFilterPath(Root<VehicleTenantMapping> root, String key) {
    try {
      return switch (key) {
        case "mappingId", "id" -> root.get("id");
        case "activeForTenant" -> root.get("active");
        case "linkedAt" -> root.get("linkedAt");
        case "tenantId" -> root.join("tenant").get("id");
        case "vehicleId" -> root.join("vehicle").get("id");
        case "vehicleUniqueCode" -> root.join("vehicle").get("vehicleUniqueCode");
        case "vehicleNumber" -> root.join("vehicle").get("vehicleNumber");
        case "modelName" -> root.join("vehicle").get("modelName");
        case "description" -> root.join("vehicle").get("description");
        case "colour" -> root.join("vehicle").get("colour");
        case "fuelType" -> root.join("vehicle").get("fuelType");
        case "registeredOwnerName" -> root.join("vehicle").get("registeredOwnerName");
        case "vehicleTypeId" -> root.join("vehicle").join("vehicleType").get("id");
        case "vehicleTypeName" -> root.join("vehicle").join("vehicleType").get("defaultName");
        default -> {
          try {
            yield root.get(key);
          } catch (IllegalArgumentException ex) {
            try {
              yield root.join("vehicle").get(key);
            } catch (IllegalArgumentException innerEx) {
              yield null;
            }
          }
        }
      };
    } catch (Exception ex) {
      return null;
    }
  }

  private Object convertValue(Class<?> fieldType, String value) {
    if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
      return Integer.parseInt(value);
    }
    if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
      return Long.parseLong(value);
    }
    if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
      return Boolean.parseBoolean(value);
    }
    if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
      return Double.parseDouble(value);
    }
    return value;
  }
}
