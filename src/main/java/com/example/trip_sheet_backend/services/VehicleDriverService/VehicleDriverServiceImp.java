package com.example.trip_sheet_backend.services.VehicleDriverService;

import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseService;
import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.dtos.VehicleDriverCreateRequestDto;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.Vehicle;
import com.example.trip_sheet_backend.models.VehicleDriverMapping;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.repositories.VehicleDriverMappingRepository;
import com.example.trip_sheet_backend.services.DriverService.DriverService;
import com.example.trip_sheet_backend.services.RoleService.RoleService;
import com.example.trip_sheet_backend.services.TenantService.TenantService;
import com.example.trip_sheet_backend.services.UserAccountService.UserAccountService;
import com.example.trip_sheet_backend.services.VehicleService.VehicleService;
import com.example.trip_sheet_backend.services.VehicleTypeService.VehicleTypeService;

@Service
public class VehicleDriverServiceImp extends BaseServiceImp<VehicleDriverMapping, UUID> implements VehicleDriverService {

  VehicleDriverMappingRepository repository;

  private final VehicleService vehicleService;
  private final DriverService driverService;
  private final VehicleTypeService vehicleTypeService;
  private final UserAccountService userAccountService;
  private final RoleService roleService;
  private final TenantService tenantService;
  private final BaseService<VehicleDriverMapping, UUID> mappingService;
  private final ModelMapper mapper;

  public VehicleDriverServiceImp(
    VehicleDriverMappingRepository repository, VehicleService vehicleService, DriverService driverService, VehicleTypeService vehicleTypeService,
      UserAccountService userAccountService, RoleService roleService, BaseService<VehicleDriverMapping, UUID> mappingService, ModelMapper mapper,
      TenantService tenantService
  ) {
    super(repository);
    this.vehicleService = vehicleService;
    this.driverService = driverService;
    this.vehicleTypeService = vehicleTypeService;
    this.userAccountService = userAccountService;
    this.roleService = roleService;
    this.mappingService = mappingService;
    this.tenantService = tenantService;
    this.mapper = mapper;

    this.repository = repository;
  }

  @Override
  public VehicleDriverMapping createVehicleAndDriver(VehicleDriverCreateRequestDto dto) {
    // --------------------------------------------------
    // 1. Fetch related entities (FK lookup)
    // --------------------------------------------------
    UUID vehicleTypeId =  UUID.fromString(dto.getVehicle_info().getVehicleTypeId());
    UUID accountId = UUID.fromString(dto.getDriver_info().getUserAccountId());
    UUID roleId = UUID.fromString(dto.getDriver_info().getRoleId());
    UUID tenantId = UUID.fromString(dto.getTenantId());

    VehicleType vehicleType = vehicleTypeService.findByIdResource(vehicleTypeId);


    UserAccount account = userAccountService.findByIdResource(accountId);

    Role role = roleService.findByIdResource(roleId);

    // --------------------------------------------------
    // 2. Map Vehicle
    // --------------------------------------------------
    Vehicle vehicle = mapper.map(dto.getVehicle_info(), Vehicle.class);
    vehicle.setVehicleType(vehicleType);

    vehicle = vehicleService.createResource(vehicle);

    // --------------------------------------------------
    // 3. Map Driver
    // --------------------------------------------------
    Driver driver = mapper.map(dto.getDriver_info(), Driver.class);
    driver.setAccount(account);
    driver.setRole(role);

    driver = driverService.createResource(driver);

    Tenant tenant = tenantService.findByIdResource(tenantId);

    // --------------------------------------------------
    // 4. Create Vehicle-Driver Mapping
    // --------------------------------------------------
    VehicleDriverMapping mapping = new VehicleDriverMapping();
    mapping.setVehicle(vehicle);
    mapping.setDriver(driver);
    mapping.setTenant(tenant);
    mapping.setStatus(VehicleDriverMapping.typeStatus.ACTIVE);

    Optional<VehicleDriverMapping> existingActive = repository.findByDriverIdAndTenantIdAndStatus(
      driver.getId(), tenantId, VehicleDriverMapping.typeStatus.ACTIVE
    );

    if (existingActive.isPresent()) {
        throw new RuntimeException("Driver already has an active vehicle mapping in this tenant.");
    }

    return mappingService.createResource(mapping);
  }

}
