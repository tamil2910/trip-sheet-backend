package com.example.trip_sheet_backend.services.VehicleDriverService;

import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.common.services.BaseService;
import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.dtos.VehicleDriverCreateRequestDto;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.Vehicle;
import com.example.trip_sheet_backend.models.VehicleDriverMapping;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.models.VehicleTypeCustomName;
import com.example.trip_sheet_backend.repositories.RoleRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.repositories.VehicleDriverMappingRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeCustomNamesRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeRepository;
import com.example.trip_sheet_backend.security.JwtTokenUtil;
import com.example.trip_sheet_backend.services.DriverService.DriverService;
import com.example.trip_sheet_backend.services.RoleService.RoleService;
import com.example.trip_sheet_backend.services.TenantService.TenantService;
import com.example.trip_sheet_backend.services.UserAccountService.UserAccountService;
import com.example.trip_sheet_backend.services.VehicleService.VehicleService;
import com.example.trip_sheet_backend.services.VehicleTypeService.VehicleTypeService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class VehicleDriverServiceImp extends BaseServiceImp<VehicleDriverMapping, UUID> implements VehicleDriverService {

    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleTypeCustomNamesRepository vehicleTypeCustomNamesRepository;

  VehicleDriverMappingRepository repository;

  private final VehicleService vehicleService;
  private final DriverService driverService;
  private final VehicleTypeService vehicleTypeService;
  private final UserAccountService userAccountService;
  private final RoleService roleService;
  private final TenantService tenantService;
  private final BaseService<VehicleDriverMapping, UUID> mappingService;
  private final ModelMapper mapper;
  private final JwtTokenUtil jwtTokenUtil;

  public VehicleDriverServiceImp(
    VehicleDriverMappingRepository repository, VehicleService vehicleService, DriverService driverService, VehicleTypeService vehicleTypeService,
      UserAccountService userAccountService, RoleService roleService, BaseService<VehicleDriverMapping, UUID> mappingService, ModelMapper mapper,
      TenantService tenantService, JwtTokenUtil jwtTokenUtil, VehicleTypeCustomNamesRepository vehicleTypeCustomNamesRepository
  , UserAccountRepository userAccountRepository, RoleRepository roleRepository, VehicleTypeRepository vehicleTypeRepository) {
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
    this.jwtTokenUtil = jwtTokenUtil;
    this.userAccountRepository = userAccountRepository;
    this.roleRepository = roleRepository;
    this.vehicleTypeRepository  = vehicleTypeRepository;
    this.vehicleTypeCustomNamesRepository = vehicleTypeCustomNamesRepository;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public VehicleDriverMapping createVehicleAndDriver(VehicleDriverCreateRequestDto dto, HttpServletRequest request) {
    
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String createdBy = (String) auth.getDetails();

    String token = request.getHeader("Authorization").replace("Bearer ", "");
    UUID userId = UUID.fromString(jwtTokenUtil.getUserIdFromToken(token));

    UserAccount userAccount = userAccountService.findByIdResource(userId);

    if (userAccount.getTenant() == null) {
        throw new RuntimeException("User does not belong to any tenant!");
    }

    // 1. Fetch related entities (FK lookup)
    UUID vehicleTypeId =  UUID.fromString(dto.getVehicle_info().getVehicleTypeId());
    VehicleTypeCustomName vehicleTypeCustomName = vehicleTypeCustomNamesRepository.findByIdAndTenant_Id(vehicleTypeId, userAccount.getTenant().getId()).orElseThrow(() -> new RuntimeException("Vehicle type custom name not found!"));
    


    // VehicleType vehicleType = vehicleTypeRepository.findById(vehicleTypeId).orElseThrow(() -> new RuntimeException("Vehicle type not found!"));

    // if(vehicleType == null) {
    //    throw new RuntimeException("Vehicle type resource not found!");
    // }

    Role role = roleRepository.findByName("DRIVER").orElseThrow(() -> new RuntimeException("DRIVER role is not found!"));
    

    // Create vehicle
    // Vehicle vehicle = new Vehicle();
    Vehicle vehicle = mapper.map(dto.getVehicle_info(), Vehicle.class);


    // vehicle.setModelName(dto.getVehicle_info().getModelName());
    // vehicle.setVehicleNumber(dto.getVehicle_info().getVehicleNumber());
    // vehicle.setFuelType(Vehicle.typeFuel.valueOf(dto.getVehicle_info().getFuelType().name()));
    // vehicle.setColour(dto.getVehicle_info().getColour());
    // vehicle.setDescription(dto.getVehicle_info().getDescription());
    // vehicle.setVehicleUniqueCode(dto.getVehicle_info().getVehicleUniqueCode());

    // vehicle.setLeftSideUrl(dto.getVehicle_info().getLeftSideUrl());
    // vehicle.setRightSideUrl(dto.getVehicle_info().getRightSideUrl());
    // vehicle.setBackSideUrl(dto.getVehicle_info().getBackSideUrl());
    // vehicle.setFrontSideUrl(dto.getVehicle_info().getFrontSideUrl());
    // vehicle.setVehProfileUrl(dto.getVehicle_info().getVehProfileUrl());

    // vehicle.setRegisteredOwnerName(dto.getVehicle_info().getRegisteredOwnerName());
    // vehicle.setRegistrationDate(dto.getVehicle_info().getRegistrationDate());
    // vehicle.setChassisNumber(dto.getVehicle_info().getChassisNumber());
    // vehicle.setEngineNumber(dto.getVehicle_info().getEngineNumber());

    // vehicle.setInsuranceCompanyName(dto.getVehicle_info().getInsuranceCompanyName());
    // vehicle.setPolicyNumber(dto.getVehicle_info().getPolicyNumber());
    // vehicle.setIssueDate(dto.getVehicle_info().getIssueDate());
    // vehicle.setDueDate(dto.getVehicle_info().getDueDate());
    // vehicle.setPremiumAmount(dto.getVehicle_info().getPremiumAmount());
    // vehicle.setCoverAmount(dto.getVehicle_info().getCoverAmount());


    // extra fields set from service
    // mapper.map(dto.getVehicle_info(), Vehicle.class);
    vehicle.setTenant(userAccount.getTenant());
    vehicle.setVehicleType(vehicleTypeCustomName.getVehicleType());
    vehicle.setCreatedBy(createdBy);
    vehicle.setIsActive(true);


    vehicle = vehicleService.create(vehicle);

    // Create UserAccount for driver, and link it with driver model with driver info
    UserAccount driverUser = new UserAccount();
    driverUser.setUsername(dto.getDriver_info().getFullName());
    driverUser.setEmail(dto.getDriver_info().getEmail());
    driverUser.setPhone(dto.getDriver_info().getPhone());
    driverUser.setTenant(userAccount.getTenant());
    driverUser.setRole(role);
    driverUser.setCreatedBy(createdBy);

    driverUser = this.userAccountService.createResource(userAccount.getTenant().getId(), driverUser);

    Driver driverProfile = mapper.map(dto.getDriver_info(), Driver.class);
    
    driverProfile.setAccount(driverUser);
    driverProfile.setCreatedBy(createdBy);

    driverProfile = driverService.create(driverProfile);

    // --------------------------------------------------
    // 4. Create Vehicle-Driver Mapping
    // --------------------------------------------------
    VehicleDriverMapping mapping = new VehicleDriverMapping();
    mapping.setVehicle(vehicle);
    mapping.setDriver(driverProfile);
    mapping.setTenant(userAccount.getTenant());
    mapping.setIsActive(true);

    Optional<VehicleDriverMapping> existingActive = repository.findByDriverIdAndTenantIdAndIsActive(
      driverUser.getId(), userAccount.getTenant().getId(), true
    );

    if (existingActive.isPresent()) {
        throw new RuntimeException("Driver already has an active vehicle mapping in this tenant.");
    }

    return mappingService.createResource(userAccount.getTenant().getId(), mapping);
  }

}
