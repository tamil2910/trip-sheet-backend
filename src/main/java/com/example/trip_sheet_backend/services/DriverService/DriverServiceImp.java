package com.example.trip_sheet_backend.services.DriverService;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.common.services.GlobalBaseServiceImp;
import com.example.trip_sheet_backend.common.services.UniqueCodeGeneratorService;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCreateOrLinkRequestDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCreateOrLinkResponseDto;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.DriverTenantMapping;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.DriverRepository;
import com.example.trip_sheet_backend.repositories.DriverTenantMappingRepository;
import com.example.trip_sheet_backend.repositories.RoleRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;

@Service
public class DriverServiceImp extends GlobalBaseServiceImp<Driver, UUID> implements DriverService {

  private final DriverRepository repository;
  private final RoleRepository roleRepository;
  private final UserAccountRepository userAccountRepository;
  private final DriverTenantMappingRepository driverTenantMappingRepository;
  private final UniqueCodeGeneratorService uniqueCodeGeneratorService;

  public DriverServiceImp(
      DriverRepository repository,
      RoleRepository roleRepository,
      UserAccountRepository userAccountRepository,
      DriverTenantMappingRepository driverTenantMappingRepository,
      UniqueCodeGeneratorService uniqueCodeGeneratorService
  ) {
    super(repository);
    this.repository = repository;
    this.roleRepository = roleRepository;
    this.userAccountRepository = userAccountRepository;
    this.driverTenantMappingRepository = driverTenantMappingRepository;
    this.uniqueCodeGeneratorService = uniqueCodeGeneratorService;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public DriverCreateOrLinkResponseDto createOrLinkDriver(
      DriverCreateOrLinkRequestDto body,
      Tenant tokenTenant,
      UUID createdBy
  ) {
    if (tokenTenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    String requestedCode = normalizeCode(body.getUniqueCode());
    if (requestedCode != null) {
      return linkExistingDriverByCode(requestedCode, tokenTenant, createdBy);
    }

    String normalizedEmail = normalizeEmail(body.getEmail());
    String normalizedPhone = normalize(body.getPhone());

    if (normalizedEmail == null && normalizedPhone == null) {
      throw new RuntimeException("Email or phone is required to create or find a driver");
    }

    Driver existingDriver = findExistingDriver(normalizedEmail, normalizedPhone);
    if (existingDriver != null) {
      boolean alreadyLinked = driverTenantMappingRepository.existsByDriver_IdAndTenant_Id(
          existingDriver.getId(),
          tokenTenant.getId()
      );

      return new DriverCreateOrLinkResponseDto(
          alreadyLinked ? "DRIVER_ALREADY_LINKED" : "DRIVER_EXISTS",
          existingDriver.getDriverCode(),
          true,
          alreadyLinked,
          tokenTenant.getId(),
          getExistingMappingId(existingDriver.getId(), tokenTenant.getId()),
          existingDriver
      );
    }

    validateCreatePayload(body);

    UserAccount driverAccount = resolveOrCreateDriverAccount(body, normalizedEmail, normalizedPhone, createdBy);

    Driver driver = new Driver();
    driver.setDriverCode(
        uniqueCodeGeneratorService.generateUniqueCode(body.getCodePrefix(), repository::existsByDriverCode)
    );
    driver.setFullName(body.getFullName().trim());
    driver.setProfilePicture(body.getProfilePicture());
    driver.setLicenseNumber(body.getLicenseNumber());
    driver.setLicenseExpiry(body.getLicenseExpiry());
    driver.setInsuranceNumber(body.getInsuranceNumber());
    driver.setInsuranceExpiry(body.getInsuranceExpiry());
    driver.setPoliceVerificationId(body.getPoliceVerificationId());
    driver.setBloodGroup(body.getBloodGroup());
    driver.setDriverType(body.getDriverType() != null ? body.getDriverType() : Driver.DriverType.PERMANENT);
    driver.setRating(body.getRating() != null ? body.getRating() : 0.0);
    driver.setActive(body.getActive() != null ? body.getActive() : true);
    driver.setAvailable(body.getAvailable() != null ? body.getAvailable() : true);
    driver.setAccount(driverAccount);
    driver.setCreatedBy(createdBy.toString());
    driver.setUpdatedBy(createdBy.toString());

    Driver savedDriver = repository.save(driver);
    DriverTenantMapping mapping = createMappingIfRequired(savedDriver, tokenTenant, createdBy);

    return new DriverCreateOrLinkResponseDto(
        "DRIVER_CREATED",
        savedDriver.getDriverCode(),
        false,
        true,
        tokenTenant.getId(),
        mapping.getId(),
        savedDriver
    );
  }

  private DriverCreateOrLinkResponseDto linkExistingDriverByCode(
      String driverCode,
      Tenant tokenTenant,
      UUID createdBy
  ) {
    Driver driver = repository.findByDriverCode(driverCode)
        .orElseThrow(() -> new RuntimeException("Driver not found for unique code: " + driverCode));

    Optional<DriverTenantMapping> existingMapping = driverTenantMappingRepository.findByDriver_IdAndTenant_Id(
        driver.getId(),
        tokenTenant.getId()
    );

    if (existingMapping.isPresent()) {
      DriverTenantMapping mapping = existingMapping.get();
      return new DriverCreateOrLinkResponseDto(
          "DRIVER_ALREADY_LINKED",
          driver.getDriverCode(),
          true,
          true,
          tokenTenant.getId(),
          mapping.getId(),
          driver
      );
    }

    DriverTenantMapping mapping = createMappingIfRequired(driver, tokenTenant, createdBy);
    return new DriverCreateOrLinkResponseDto(
        "DRIVER_LINKED",
        driver.getDriverCode(),
        true,
        true,
        tokenTenant.getId(),
        mapping.getId(),
        driver
    );
  }

  private Driver findExistingDriver(String normalizedEmail, String normalizedPhone) {
    Driver driverByEmail = normalizedEmail == null
        ? null
        : repository.findByAccount_Email(normalizedEmail).orElse(null);
    Driver driverByPhone = normalizedPhone == null
        ? null
        : repository.findByAccount_Phone(normalizedPhone).orElse(null);

    if (driverByEmail != null && driverByPhone != null && !driverByEmail.getId().equals(driverByPhone.getId())) {
      throw new RuntimeException("Email and phone belong to different driver accounts");
    }

    return driverByEmail != null ? driverByEmail : driverByPhone;
  }

  private void validateCreatePayload(DriverCreateOrLinkRequestDto body) {
    if (normalize(body.getFullName()) == null) {
      throw new RuntimeException("Full name is required to create a new driver");
    }
    if (normalize(body.getLicenseNumber()) == null) {
      throw new RuntimeException("License number is required to create a new driver");
    }
    if (body.getLicenseExpiry() == null) {
      throw new RuntimeException("License expiry is required to create a new driver");
    }
  }

  private UserAccount resolveOrCreateDriverAccount(
      DriverCreateOrLinkRequestDto body,
      String normalizedEmail,
      String normalizedPhone,
      UUID createdBy
  ) {
    UserAccount accountByEmail = normalizedEmail == null
        ? null
        : userAccountRepository.findByEmail(normalizedEmail).orElse(null);
    UserAccount accountByPhone = normalizedPhone == null
        ? null
        : userAccountRepository.findByPhone(normalizedPhone).orElse(null);

    if (accountByEmail != null && accountByPhone != null && !accountByEmail.getId().equals(accountByPhone.getId())) {
      throw new RuntimeException("Email and phone belong to different user accounts");
    }

    UserAccount existingAccount = accountByEmail != null ? accountByEmail : accountByPhone;
    if (existingAccount != null) {
      repository.findByAccount_Id(existingAccount.getId()).ifPresent(driver -> {
        throw new RuntimeException("Driver already exists with the given email or phone");
      });

      return existingAccount;
    }

    Role driverRole = roleRepository.findByName("DRIVER")
        .orElseThrow(() -> new RuntimeException("DRIVER role not found"));

    UserAccount account = new UserAccount();
    account.setUsername(generateUniqueUsername(body.getFullName(), normalizedEmail, normalizedPhone));
    account.setEmail(normalizedEmail);
    account.setPhone(normalizedPhone);
    account.setRole(driverRole);
    account.setTenant(null);
    account.setTenantType(null);
    account.setIsActive(body.getActive() != null ? body.getActive() : true);
    account.setCreatedBy(createdBy.toString());
    account.setUpdatedBy(createdBy.toString());

    return userAccountRepository.save(account);
  }

  private DriverTenantMapping createMappingIfRequired(Driver driver, Tenant tenant, UUID createdBy) {
    return driverTenantMappingRepository.findByDriver_IdAndTenant_Id(driver.getId(), tenant.getId())
        .orElseGet(() -> {
          DriverTenantMapping mapping = new DriverTenantMapping();
          mapping.setDriver(driver);
          mapping.setTenant(tenant);
          mapping.setActive(true);
          mapping.setLinkedAt(Instant.now().toEpochMilli());
          mapping.setCreatedBy(createdBy.toString());
          mapping.setUpdatedBy(createdBy.toString());
          return driverTenantMappingRepository.save(mapping);
        });
  }

  private UUID getExistingMappingId(UUID driverId, UUID tenantId) {
    return driverTenantMappingRepository.findByDriver_IdAndTenant_Id(driverId, tenantId)
        .map(DriverTenantMapping::getId)
        .orElse(null);
  }

  private String generateUniqueUsername(String fullName, String email, String phone) {
    String base = normalize(fullName);
    if (base == null && email != null) {
      base = email.split("@")[0];
    }
    if (base == null && phone != null) {
      base = "driver" + phone.substring(Math.max(0, phone.length() - 4));
    }
    if (base == null) {
      base = "driver";
    }

    String sanitized = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
    sanitized = sanitized.replaceAll("^_+|_+$", "");
    if (sanitized.isBlank()) {
      sanitized = "driver";
    }

    String candidate = sanitized;
    int suffix = 1;
    while (userAccountRepository.findByUsername(candidate).isPresent()) {
      candidate = sanitized + "_" + suffix++;
    }
    return candidate;
  }

  private String normalize(String value) {
    if (value == null) {
      return null;
    }

    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String normalizeEmail(String value) {
    String normalized = normalize(value);
    return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
  }

  private String normalizeCode(String value) {
    String normalized = normalize(value);
    return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
  }
}
