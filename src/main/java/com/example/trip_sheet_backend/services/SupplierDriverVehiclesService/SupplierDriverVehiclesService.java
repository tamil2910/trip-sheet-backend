package com.example.trip_sheet_backend.services.SupplierDriverVehiclesService;

import java.util.List;
import java.util.Locale;
import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.common.services.UniqueCodeGeneratorService;
import com.example.trip_sheet_backend.dtos.SupplierDriverVehiclesDtos.SupplierDriverVehiclesCreateRequestDTO;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.DriverTenantMapping;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.RoleGroup;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.models.SupplierDriverVehicles;
import com.example.trip_sheet_backend.repositories.DriverRepository;
import com.example.trip_sheet_backend.repositories.DriverTenantMappingRepository;
import com.example.trip_sheet_backend.repositories.RoleGroupRepository;
import com.example.trip_sheet_backend.repositories.RoleRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeRepository;
import com.example.trip_sheet_backend.repositories.SupplierDriverVehiclesRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRepository;

@Service
public class SupplierDriverVehiclesService {
  private final SupplierDriverVehiclesRepository repository;
  private final VendorPartnerRepository vendorPartnerRepository;
  private final UserAccountRepository userAccountRepository;
  private final TenantRepository tenantRepository;
  private final DriverRepository driverRepository;
  private final DriverTenantMappingRepository driverTenantMappingRepository;
  private final RoleRepository roleRepository;
  private final RoleGroupRepository roleGroupRepository;
  private final VehicleTypeRepository vehicleTypeRepository;
  private final UniqueCodeGeneratorService uniqueCodeGeneratorService;

  public SupplierDriverVehiclesService(
      SupplierDriverVehiclesRepository repository,
      VendorPartnerRepository vendorPartnerRepository,
      UserAccountRepository userAccountRepository,
      TenantRepository tenantRepository,
      DriverRepository driverRepository,
      DriverTenantMappingRepository driverTenantMappingRepository,
      RoleRepository roleRepository,
      RoleGroupRepository roleGroupRepository,
      VehicleTypeRepository vehicleTypeRepository,
      UniqueCodeGeneratorService uniqueCodeGeneratorService
  ) {
    this.repository = repository;
    this.vendorPartnerRepository = vendorPartnerRepository;
    this.userAccountRepository = userAccountRepository;
    this.tenantRepository = tenantRepository;
    this.driverRepository = driverRepository;
    this.driverTenantMappingRepository = driverTenantMappingRepository;
    this.roleRepository = roleRepository;
    this.roleGroupRepository = roleGroupRepository;
    this.vehicleTypeRepository = vehicleTypeRepository;
    this.uniqueCodeGeneratorService = uniqueCodeGeneratorService;
  }

  @Transactional(rollbackFor = Exception.class)
  public SupplierDriverVehicles create(
      SupplierDriverVehiclesCreateRequestDTO body,
      Tenant primaryVendor,
      String createdBy
  ) {
    if (primaryVendor == null || primaryVendor.getId() == null
        || primaryVendor.getTenantType() != Tenant.TenantType.VENDOR) {
      throw new RuntimeException("Current vendor not found in token");
    }

    Tenant partnerVendor = vendorPartnerRepository
        .findByPrimaryVendorAndPartnerVendor(primaryVendor, resolvePartnerVendor(body.getPartnerVendorId()))
        .filter(partner -> !Boolean.TRUE.equals(partner.getIsDeleted()))
        .map(VendorPartner::getPartnerVendor)
        .orElseThrow(() -> new RuntimeException("Vendor partner relationship not found"));

    SupplierDriverVehicles entry = new SupplierDriverVehicles();
    entry.setPrimaryVendor(primaryVendor);
    entry.setPartnerVendor(partnerVendor);
    entry.setType(body.getType());

    if (body.getType() == SupplierDriverVehicles.Type.DRIVER) {
      String driverName = requireText(body.getDriverName(), "Driver name is required");
      String phone = requireText(body.getPhone(), "Driver phone is required");
      String email = requireText(body.getEmail(), "Driver email is required");
      phone = phone.trim();
      email = email.trim().toLowerCase(Locale.ROOT);
      if (repository.existsByPrimaryVendor_IdAndPartnerVendor_IdAndPhone(
              primaryVendor.getId(), partnerVendor.getId(), phone)
          || repository.existsByPrimaryVendor_IdAndPartnerVendor_IdAndEmailIgnoreCase(
              primaryVendor.getId(), partnerVendor.getId(), email)) {
        throw new RuntimeException("A driver with this phone or email already exists for this partner");
      }
      entry.setDriverName(driverName.trim());
      entry.setPhone(phone);
      entry.setEmail(email);
      entry.setDriver(resolveOrCreateDriver(entry, primaryVendor, createdBy));
    } else {
      String vehicleNumber = requireText(body.getVehicleNumber(), "Vehicle number is required")
          .trim().toUpperCase(Locale.ROOT);
      if (repository.existsByPrimaryVendor_IdAndPartnerVendor_IdAndVehicleNumberIgnoreCase(
          primaryVendor.getId(), partnerVendor.getId(), vehicleNumber)) {
        throw new RuntimeException("A vehicle with this number already exists for this partner");
      }
      entry.setVehicleNumber(vehicleNumber);
      if (body.getVehicleTypeId() == null) {
        throw new RuntimeException("Vehicle type is required");
      }
      VehicleType vehicleType = vehicleTypeRepository.findById(body.getVehicleTypeId())
          .filter(type -> !Boolean.TRUE.equals(type.getIsDeleted()))
          .orElseThrow(() -> new RuntimeException("Invalid vehicle type"));
      entry.setVehicleType(vehicleType);
    }

    entry.setCreatedBy(createdBy);
    return repository.save(entry);
  }

  @Transactional(readOnly = true)
  public List<SupplierDriverVehicles> getByPartner(Tenant primaryVendor, UUID partnerVendorId) {
    if (primaryVendor == null || primaryVendor.getId() == null
        || primaryVendor.getTenantType() != Tenant.TenantType.VENDOR) {
      throw new RuntimeException("Current vendor not found in token");
    }
    return repository.findByPrimaryVendor_IdAndPartnerVendor_IdAndIsDeletedFalseOrderByCreatedAtDesc(
        primaryVendor.getId(), partnerVendorId);
  }

  private Tenant resolvePartnerVendor(java.util.UUID partnerVendorId) {
    if (partnerVendorId == null) {
      throw new RuntimeException("Partner vendor is required");
    }
    return tenantRepository.findById(partnerVendorId)
        .filter(tenant -> tenant.getTenantType() == Tenant.TenantType.VENDOR)
        .orElseThrow(() -> new RuntimeException("Partner vendor not found"));
  }

  private Driver resolveOrCreateDriver(SupplierDriverVehicles entry, Tenant primaryVendor, String createdBy) {
    var emailAccount = userAccountRepository.findByEmail(entry.getEmail());
    var phoneAccount = userAccountRepository.findByPhone(entry.getPhone());

    if (emailAccount.isPresent() && phoneAccount.isPresent()
        && !emailAccount.get().getId().equals(phoneAccount.get().getId())) {
      throw new RuntimeException("Driver email and phone belong to different user accounts");
    }

    UserAccount account = emailAccount.orElseGet(() -> phoneAccount.orElse(null));
    if (account == null) {
      account = createDriverAccount(entry, createdBy);
    } else if (account.getRole() == null || !"DRIVER".equalsIgnoreCase(account.getRole().getName())) {
      throw new RuntimeException("A user account with this email or phone already exists with a non-driver role");
    }

    Driver driver = driverRepository.findByAccount_Id(account.getId()).orElse(null);
    if (driver == null) {
      driver = createDriver(entry, account, createdBy);
    }
    ensureDriverRoleGroup(account);
    createTenantMappingIfRequired(driver, primaryVendor, createdBy);
    return driver;
  }

  private UserAccount createDriverAccount(SupplierDriverVehicles entry, String createdBy) {
    Role driverRole = roleRepository.findByName("DRIVER")
        .orElseThrow(() -> new RuntimeException("DRIVER role not found"));

    UserAccount account = new UserAccount();
    account.setUsername(generateUniqueUsername(entry.getDriverName(), entry.getEmail(), entry.getPhone()));
    account.setEmail(entry.getEmail());
    account.setPhone(entry.getPhone());
    account.setLoginType(UserAccount.LoginType.EMAIL);
    account.setRole(driverRole);
    account.setTenant(null);
    account.setTenantType(null);
    account.setIsActive(true);
    account.setCreatedBy(createdBy);
    account.setUpdatedBy(createdBy);
    return userAccountRepository.save(account);
  }

  private Driver createDriver(SupplierDriverVehicles entry, UserAccount account, String createdBy) {
    Driver driver = new Driver();
    driver.setDriverCode(uniqueCodeGeneratorService.generateUniqueCode("DRV", driverRepository::existsByDriverCode));
    driver.setFullName(entry.getDriverName());
    driver.setDriverType(Driver.DriverType.PERMANENT);
    driver.setActive(true);
    driver.setAvailable(true);
    driver.setAccount(account);
    driver.setCreatedBy(createdBy);
    driver.setUpdatedBy(createdBy);
    return driverRepository.save(driver);
  }

  private void ensureDriverRoleGroup(UserAccount account) {
    RoleGroup driverGroup = roleGroupRepository.findByNameAndTenantIsNull("DRIVER_GLOBAL_PERMISSIONS")
        .orElseThrow(() -> new RuntimeException("DRIVER_GLOBAL_PERMISSIONS role group not found"));
    if (account.getRoleGroups() == null) {
      account.setRoleGroups(new HashSet<>());
    }
    account.getRoleGroups().add(driverGroup);
    userAccountRepository.save(account);
  }

  private void createTenantMappingIfRequired(Driver driver, Tenant tenant, String createdBy) {
    if (driverTenantMappingRepository.findByDriver_IdAndTenant_Id(driver.getId(), tenant.getId()).isPresent()) {
      return;
    }
    long now = Instant.now().toEpochMilli();
    DriverTenantMapping mapping = new DriverTenantMapping();
    mapping.setDriver(driver);
    mapping.setTenant(tenant);
    mapping.setActive(true);
    mapping.setLinkedAt(now);
    mapping.setCreatedBy(createdBy);
    mapping.setUpdatedBy(createdBy);
    driverTenantMappingRepository.save(mapping);
  }

  private String generateUniqueUsername(String driverName, String email, String phone) {
    String base = driverName == null || driverName.isBlank() ? email.split("@")[0] : driverName;
    String sanitized = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    if (sanitized.isBlank()) {
      sanitized = "driver" + phone.substring(Math.max(0, phone.length() - 4));
    }

    String candidate = sanitized;
    int suffix = 1;
    while (userAccountRepository.findByUsername(candidate).isPresent()) {
      candidate = sanitized + "_" + suffix++;
    }
    return candidate;
  }

  private String requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new RuntimeException(message);
    }
    return value;
  }
}