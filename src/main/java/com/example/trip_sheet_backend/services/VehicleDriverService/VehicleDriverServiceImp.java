package com.example.trip_sheet_backend.services.VehicleDriverService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.common.services.BaseService;
import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleDriverCreateRequestDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleDriverMappingResponseDto;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.Vehicle;
import com.example.trip_sheet_backend.models.VehicleDriverMapping;
import com.example.trip_sheet_backend.models.VehicleTypeCustomName;
import com.example.trip_sheet_backend.repositories.RoleRepository;
import com.example.trip_sheet_backend.repositories.VehicleDriverMappingRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeCustomNamesRepository;
import com.example.trip_sheet_backend.security.JwtTokenUtil;
import com.example.trip_sheet_backend.services.DriverService.DriverService;
import com.example.trip_sheet_backend.services.UserAccountService.UserAccountService;
import com.example.trip_sheet_backend.services.VehicleService.VehicleService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class VehicleDriverServiceImp extends BaseServiceImp<VehicleDriverMapping, UUID> implements VehicleDriverService {

    private final RoleRepository roleRepository;
    private final VehicleTypeCustomNamesRepository vehicleTypeCustomNamesRepository;

  VehicleDriverMappingRepository repository;

  private final VehicleService vehicleService;
  private final DriverService driverService;
  private final UserAccountService userAccountService;
  private final BaseService<VehicleDriverMapping, UUID> mappingService;
  private final ModelMapper mapper;
  private final JwtTokenUtil jwtTokenUtil;

  public VehicleDriverServiceImp(
    VehicleDriverMappingRepository repository, VehicleService vehicleService, DriverService driverService, UserAccountService userAccountService, BaseService<VehicleDriverMapping, UUID> mappingService, ModelMapper mapper, JwtTokenUtil jwtTokenUtil, VehicleTypeCustomNamesRepository vehicleTypeCustomNamesRepository, RoleRepository roleRepository) {
    super(repository);
    this.vehicleService = vehicleService;
    this.driverService = driverService;
    this.userAccountService = userAccountService;
    this.mappingService = mappingService;
    this.mapper = mapper;

    this.repository = repository;
    this.jwtTokenUtil = jwtTokenUtil;
    this.roleRepository = roleRepository;
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
    
    String vehicleNumber = dto.getVehicle_info().getVehicleNumber();
    Vehicle existingVehicle = vehicleService.findByVehicleNumberAndTenantId(vehicleNumber, userAccount.getTenant().getId());

    if (existingVehicle != null) {
      throw new RuntimeException("Vehicle with the same number already exists.");
    }

    // Create vehicle
    // Vehicle vehicle = new Vehicle();
    Vehicle vehicle = mapper.map(dto.getVehicle_info(), Vehicle.class);

    // extra fields set from service
    // mapper.map(dto.getVehicle_info(), Vehicle.class);
    vehicle.setTenant(userAccount.getTenant());
    vehicle.setVehicleType(vehicleTypeCustomName.getVehicleType());
    vehicle.setCreatedBy(createdBy);
    vehicle.setIsActive(true);


    vehicle = vehicleService.create(vehicle);

        // Create UserAccount for driver, and link it with driver model with driver info
    UserAccount driverUser = new UserAccount();
    VehicleDriverMapping vehicleDriverMapping = new VehicleDriverMapping();

    String driverEmail = dto.getDriver_info().getEmail();
    UserAccount existingDriver = userAccountService.findByEmail(driverEmail);

    if (existingDriver != null) {
      throw new RuntimeException("Driver with the same email already exists.");
    }

    String driverPhone = dto.getDriver_info().getPhone();
    UserAccount existingDriverPhone = userAccountService.findByPhone(driverPhone);

    if (existingDriverPhone != null) {
      throw new RuntimeException("Driver with the same phone number already exists.");
    }

    if ((existingDriver != null || existingDriverPhone != null) & dto.getLinkTenant() == false) {
      throw new RuntimeException("Driver with the same email or phone number already exists.");
    } else {
      if (dto.getLinkTenant() == true) {
        vehicleDriverMapping.setTenant(userAccount.getTenant());
        // write tenant driver mapping relationship here
        // for that create TenantDriverMapping entity/model, create service, repository, controller

      }
    }



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

}
