package com.example.trip_sheet_backend.services.DriverService;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.common.services.GlobalBaseServiceImp;
import com.example.trip_sheet_backend.common.services.UniqueCodeGeneratorService;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCodeLookupResponseDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCreateOrLinkRequestDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCreateOrLinkResponseDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverSetPasswordRequestDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverTenantResponseDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverUpdateRequestDto;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.DriverTenantMapping;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.RoleGroup;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.DriverRepository;
import com.example.trip_sheet_backend.repositories.DriverTenantMappingRepository;
import com.example.trip_sheet_backend.repositories.RoleRepository;
import com.example.trip_sheet_backend.repositories.RoleGroupRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.services.EmailService;

@Service
public class DriverServiceImp extends GlobalBaseServiceImp<Driver, UUID> implements DriverService {

  private static final String DRIVER_CODE_PREFIX = "DRV";

  private final DriverRepository repository;
  private final RoleRepository roleRepository;
  private final UserAccountRepository userAccountRepository;
  private final DriverTenantMappingRepository driverTenantMappingRepository;
  private final RoleGroupRepository roleGroupRepository;
  private final UniqueCodeGeneratorService uniqueCodeGeneratorService;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;

  public DriverServiceImp(
      DriverRepository repository,
      RoleRepository roleRepository,
      UserAccountRepository userAccountRepository,
      DriverTenantMappingRepository driverTenantMappingRepository,
      RoleGroupRepository roleGroupRepository,
      UniqueCodeGeneratorService uniqueCodeGeneratorService,
        PasswordEncoder passwordEncoder,
        EmailService emailService
  ) {
    super(repository);
    this.repository = repository;
    this.roleRepository = roleRepository;
    this.userAccountRepository = userAccountRepository;
    this.driverTenantMappingRepository = driverTenantMappingRepository;
    this.roleGroupRepository = roleGroupRepository;
    this.uniqueCodeGeneratorService = uniqueCodeGeneratorService;
    this.passwordEncoder = passwordEncoder;
    this.emailService = emailService;
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
      return linkExistingDriverByCode(requestedCode, body, tokenTenant, createdBy);
    }

    String normalizedEmail = normalizeEmail(body.getEmail());
    String normalizedPhone = normalize(body.getPhone());

    if (normalizedEmail == null && normalizedPhone == null) {
      throw new RuntimeException("Email or phone is required to create or find a driver");
    }

    Driver existingDriver = findExistingDriver(normalizedEmail, normalizedPhone);
    if (existingDriver != null) {
      ensureDriverHasAccount(existingDriver, body, normalizedEmail, normalizedPhone, createdBy);
      ensureDriverRoleGroup(existingDriver.getAccount());

      boolean alreadyLinked = driverTenantMappingRepository.existsByDriver_IdAndTenant_Id(
          existingDriver.getId(),
          tokenTenant.getId()
      );

      updateDriverUserAccountWithTenantId(existingDriver.getAccount(), tokenTenant, createdBy);

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
        uniqueCodeGeneratorService.generateUniqueCode(DRIVER_CODE_PREFIX, repository::existsByDriverCode)
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
    ensureDriverRoleGroup(driverAccount);
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

  @Transactional(readOnly = true)
  @Override
  public DriverCodeLookupResponseDto getDriverByUniqueCode(Tenant tokenTenant, String uniqueCode) {
    validateTenant(tokenTenant);
    String normalizedCode = normalizeCode(uniqueCode);
    if (normalizedCode == null) {
      throw new RuntimeException("Unique code is required");
    }

    Driver driver = repository.findByDriverCode(normalizedCode)
        .orElseThrow(() -> new RuntimeException("Driver not found for unique code: " + normalizedCode));

    DriverTenantMapping mapping = driverTenantMappingRepository
        .findByDriver_IdAndTenant_Id(driver.getId(), tokenTenant.getId())
        .orElse(null);

    return DriverCodeLookupResponseDto.fromEntity(driver, tokenTenant, mapping);
  }

  @Transactional(readOnly = true)
  @Override
  public List<DriverTenantResponseDto> getDriversByTenant(Tenant tokenTenant) {
    validateTenant(tokenTenant);
    return driverTenantMappingRepository.findByTenant_Id(tokenTenant.getId()).stream()
        .map(DriverTenantResponseDto::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  @Override
  public Page<DriverTenantResponseDto> searchDriversByTenant(
      Tenant tokenTenant,
      String fullName,
      String phone,
      String email,
      Pageable pageable
  ) {
    validateTenant(tokenTenant);

    String normalizedFullName = normalize(fullName);
    String normalizedPhone = normalize(phone);
    String normalizedEmail = normalizeEmail(email);

    return driverTenantMappingRepository
        .searchByTenantAndDriverFilters(
            tokenTenant.getId(),
            normalizedFullName,
            normalizedPhone,
            normalizedEmail,
            pageable
        )
        .map(DriverTenantResponseDto::fromEntity);
  }

  @Transactional(readOnly = true)
  @Override
  public DriverTenantResponseDto getDriverByTenant(Tenant tokenTenant, UUID driverId) {
    DriverTenantMapping mapping = getTenantDriverMapping(tokenTenant, driverId);
    return DriverTenantResponseDto.fromEntity(mapping);
  }

  @Transactional(readOnly = true)
  @Override
  public DriverTenantResponseDto getMyDriverProfile(UserAccount currentUser) {
    if (currentUser == null) {
      throw new RuntimeException("User not found in token");
    }

    Driver driver = repository.findByAccount_Id(currentUser.getId())
        .orElseThrow(() -> new RuntimeException("Driver profile not found for current user"));

    return DriverTenantResponseDto.fromDriver(driver);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public DriverTenantResponseDto linkDriverToCurrentTenant(Tenant tokenTenant, UUID driverId, UUID createdBy) {
    validateTenant(tokenTenant);

    Driver driver = repository.findById(driverId)
        .orElseThrow(() -> new RuntimeException("Driver not found"));

    deactivateDriverInOtherTenants(driver.getId(), tokenTenant.getId(), createdBy);
    
    updateDriverUserAccountWithTenantId(driver.getAccount(), tokenTenant, createdBy);
    DriverTenantMapping mapping = createMappingIfRequired(driver, tokenTenant, createdBy);
    return DriverTenantResponseDto.fromEntity(mapping);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public DriverTenantResponseDto updateDriverByTenant(
      Tenant tokenTenant,
      UUID driverId,
      DriverUpdateRequestDto body,
      UUID updatedBy
  ) {
    DriverTenantMapping mapping = getTenantDriverMapping(tokenTenant, driverId);
    Driver driver = mapping.getDriver();

    Driver savedDriver = applyDriverProfileUpdates(driver, body, updatedBy);

    mapping.setDriver(savedDriver);
    mapping.setUpdatedBy(updatedBy.toString());
    DriverTenantMapping savedMapping = driverTenantMappingRepository.save(mapping);
    return DriverTenantResponseDto.fromEntity(savedMapping);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public DriverTenantResponseDto updateMyDriverProfile(UserAccount currentUser, DriverUpdateRequestDto body) {
    if (currentUser == null) {
      throw new RuntimeException("User not found in token");
    }

    Driver driver = repository.findByAccount_Id(currentUser.getId())
        .orElseThrow(() -> new RuntimeException("Driver profile not found for current user"));

    Driver savedDriver = applyDriverProfileUpdates(driver, body, currentUser.getId());
    return DriverTenantResponseDto.fromDriver(savedDriver);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public DriverTenantResponseDto setDriverActiveForTenant(
      Tenant tokenTenant,
      UUID driverId,
      boolean active,
      UUID updatedBy
  ) {
    DriverTenantMapping mapping = getTenantDriverMapping(tokenTenant, driverId);
    mapping.setActive(active);
    mapping.setUpdatedBy(updatedBy.toString());
    DriverTenantMapping savedMapping = driverTenantMappingRepository.save(mapping);
    return DriverTenantResponseDto.fromEntity(savedMapping);
  }

  private DriverCreateOrLinkResponseDto linkExistingDriverByCode(
      String driverCode,
      DriverCreateOrLinkRequestDto body,
      Tenant tokenTenant,
      UUID createdBy
  ) {
    Driver driver = repository.findByDriverCode(driverCode)
        .orElseThrow(() -> new RuntimeException("Driver not found for unique code: " + driverCode));

    ensureDriverHasAccount(
        driver,
        body,
        normalizeEmail(body.getEmail()),
        normalize(body.getPhone()),
        createdBy
    );

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

      if (!isDriverRole(existingAccount)) {
        throw new RuntimeException(
            "User already exists with role " + getAccountRoleName(existingAccount) + ". Please use a different email/phone"
        );
      }

      existingAccount.setLoginType(resolveLoginType(normalizedEmail, normalizedPhone, body.getUsername()));
      existingAccount.setUpdatedBy(createdBy.toString());
      return userAccountRepository.save(existingAccount);
    }

    Role driverRole = roleRepository.findByName("DRIVER")
        .orElseThrow(() -> new RuntimeException("DRIVER role not found"));

    UserAccount account = new UserAccount();
    account.setUsername(generateUniqueUsername(body.getUsername(), body.getFullName(), normalizedEmail, normalizedPhone));
    account.setEmail(normalizedEmail);
    account.setPhone(normalizedPhone);
    account.setLoginType(resolveLoginType(normalizedEmail, normalizedPhone, body.getUsername()));
    account.setRole(driverRole);
    account.setTenant(null);
    account.setTenantType(null);
    account.setIsActive(body.getActive() != null ? body.getActive() : true);
    account.setCreatedBy(createdBy.toString());
    account.setUpdatedBy(createdBy.toString());

    return userAccountRepository.save(account);
  }

  private void ensureDriverHasAccount(
      Driver driver,
      DriverCreateOrLinkRequestDto body,
      String normalizedEmail,
      String normalizedPhone,
      UUID createdBy
  ) {
    if (driver.getAccount() != null) {
      return;
    }

    String emailToUse = normalizedEmail;
    String phoneToUse = normalizedPhone;

    if (emailToUse == null && phoneToUse == null) {
      throw new RuntimeException("Email or phone is required when driver has no user account");
    }

    UserAccount accountByEmail = emailToUse == null
        ? null
        : userAccountRepository.findByEmail(emailToUse).orElse(null);
    UserAccount accountByPhone = phoneToUse == null
        ? null
        : userAccountRepository.findByPhone(phoneToUse).orElse(null);

    if (accountByEmail != null && accountByPhone != null && !accountByEmail.getId().equals(accountByPhone.getId())) {
      throw new RuntimeException("Email and phone belong to different user accounts");
    }

    UserAccount account = accountByEmail != null ? accountByEmail : accountByPhone;

    if (account != null) {
      repository.findByAccount_Id(account.getId()).ifPresent(existingDriver -> {
        if (!existingDriver.getId().equals(driver.getId())) {
          throw new RuntimeException("User account is already linked with another driver");
        }
      });
    }

    if (account == null) {
      Role driverRole = roleRepository.findByName("DRIVER")
          .orElseThrow(() -> new RuntimeException("DRIVER role not found"));

      account = new UserAccount();
      account.setUsername(generateUniqueUsername(body.getUsername(), driver.getFullName(), emailToUse, phoneToUse));
      account.setEmail(emailToUse);
      account.setPhone(phoneToUse);
      account.setRole(driverRole);
      account.setTenant(null);
      account.setTenantType(null);
      account.setIsActive(driver.getActive() != null ? driver.getActive() : true);
      account.setCreatedBy(createdBy.toString());
    }

    account.setLoginType(resolveLoginType(emailToUse, phoneToUse, body.getUsername()));
    account.setUpdatedBy(createdBy.toString());

    UserAccount savedAccount = userAccountRepository.save(account);
    driver.setAccount(savedAccount);
    driver.setUpdatedBy(createdBy.toString());
    repository.save(driver);
  }

  private void ensureDriverRoleGroup(UserAccount userAccount) {
    if (userAccount == null) {
      throw new RuntimeException("User account is required to assign driver permissions");
    }

    RoleGroup driverGroup = roleGroupRepository.findByNameAndTenantIsNull("DRIVER_GLOBAL_PERMISSIONS")
        .orElseThrow(() -> new RuntimeException("DRIVER_GLOBAL_PERMISSIONS role group not found"));

    if (userAccount.getRoleGroups() == null) {
      userAccount.setRoleGroups(new HashSet<>());
    }
    userAccount.getRoleGroups().add(driverGroup);
    userAccountRepository.save(userAccount);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void setDriverPasswordForTenant(
      Tenant tokenTenant,
      UUID driverId,
      DriverSetPasswordRequestDto body,
      UUID updatedBy
  ) {
    DriverTenantMapping mapping = getTenantDriverMapping(tokenTenant, driverId);
    Driver driver = mapping.getDriver();
    UserAccount account = driver.getAccount();

    if (account == null) {
      throw new RuntimeException("This driver has no login account yet");
    }

    validatePasswordRequired(body.getPassword());

    String rawPassword = body.getPassword().trim();
    account.setPassword(passwordEncoder.encode(rawPassword));
    account.setUpdatedBy(updatedBy.toString());
    UserAccount savedAccount = userAccountRepository.save(account);

    driver.setAccount(savedAccount);
    driver.setUpdatedBy(updatedBy.toString());
    repository.save(driver);

    emailService.sendDriverPasswordEmail(
        savedAccount.getEmail(),
        driver.getFullName(),
        savedAccount.getUsername(),
        rawPassword
    );
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

  private DriverTenantMapping getTenantDriverMapping(Tenant tokenTenant, UUID driverId) {
    validateTenant(tokenTenant);
    return driverTenantMappingRepository.findByTenant_IdAndDriver_Id(tokenTenant.getId(), driverId)
        .orElseThrow(() -> new RuntimeException("Driver not found for this tenant"));
  }

  private Driver applyDriverProfileUpdates(Driver driver, DriverUpdateRequestDto body, UUID updatedBy) {
    String normalizedEmail = normalizeEmail(body.getEmail());
    String normalizedPhone = normalize(body.getPhone());
    String normalizedUsername = normalize(body.getUsername());

    UserAccount account = driver.getAccount();
    if (account == null && (normalizedEmail != null || normalizedPhone != null || normalizedUsername != null)) {
      throw new RuntimeException(
          "This driver has no login account. Use the driver create/link API with password to create the account first"
      );
    }

    if (normalize(body.getFullName()) != null) {
      driver.setFullName(body.getFullName().trim());
    }
    if (body.getProfilePicture() != null) {
      driver.setProfilePicture(body.getProfilePicture());
    }
    if (normalize(body.getLicenseNumber()) != null) {
      driver.setLicenseNumber(body.getLicenseNumber().trim());
    }
    if (body.getLicenseExpiry() != null) {
      driver.setLicenseExpiry(body.getLicenseExpiry());
    }
    if (body.getInsuranceNumber() != null) {
      driver.setInsuranceNumber(normalize(body.getInsuranceNumber()));
    }
    if (body.getInsuranceExpiry() != null) {
      driver.setInsuranceExpiry(body.getInsuranceExpiry());
    }
    if (body.getPoliceVerificationId() != null) {
      driver.setPoliceVerificationId(normalize(body.getPoliceVerificationId()));
    }
    if (body.getBloodGroup() != null) {
      driver.setBloodGroup(normalize(body.getBloodGroup()));
    }
    if (body.getDriverType() != null) {
      driver.setDriverType(body.getDriverType());
    }
    if (body.getRating() != null) {
      driver.setRating(body.getRating());
    }
    if (body.getAvailable() != null) {
      driver.setAvailable(body.getAvailable());
    }
    driver.setUpdatedBy(updatedBy.toString());

    if (account != null) {
      if (normalizedUsername != null) {
        validateUsernameAvailable(normalizedUsername, account.getId());
        account.setUsername(normalizedUsername);
      }
      if (normalizedEmail != null) {
        validateEmailAvailable(normalizedEmail, account.getId());
        account.setEmail(normalizedEmail);
      }
      if (normalizedPhone != null) {
        validatePhoneAvailable(normalizedPhone, account.getId());
        account.setPhone(normalizedPhone);
      }
      account.setLoginType(resolveLoginType(account.getEmail(), account.getPhone(), account.getUsername()));
      account.setUpdatedBy(updatedBy.toString());
      userAccountRepository.save(account);
    }

    return repository.save(driver);
  }

  private void validateTenant(Tenant tokenTenant) {
    if (tokenTenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }
  }

  private void validateUsernameAvailable(String username, UUID currentAccountId) {
    userAccountRepository.findByUsername(username).ifPresent(existing -> {
      if (currentAccountId == null || !existing.getId().equals(currentAccountId)) {
        throw new RuntimeException("Username already exists");
      }
    });
  }

  private void validateEmailAvailable(String email, UUID currentAccountId) {
    userAccountRepository.findByEmail(email).ifPresent(existing -> {
      if (currentAccountId == null || !existing.getId().equals(currentAccountId)) {
        throw new RuntimeException("Email already exists");
      }
    });
  }

  private void validatePhoneAvailable(String phone, UUID currentAccountId) {
    userAccountRepository.findByPhone(phone).ifPresent(existing -> {
      if (currentAccountId == null || !existing.getId().equals(currentAccountId)) {
        throw new RuntimeException("Phone already exists");
      }
    });
  }

  private UUID getExistingMappingId(UUID driverId, UUID tenantId) {
    return driverTenantMappingRepository.findByDriver_IdAndTenant_Id(driverId, tenantId)
        .map(DriverTenantMapping::getId)
        .orElse(null);
  }

  private String generateUniqueUsername(String requestedUsername, String fullName, String email, String phone) {
    String base = normalize(requestedUsername);
    if (base == null) {
      base = normalize(fullName);
    }
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

  private UserAccount.LoginType resolveLoginType(String email, String phone, String username) {
    if (email != null) {
      return UserAccount.LoginType.EMAIL;
    }
    if (phone != null) {
      return UserAccount.LoginType.PHONE;
    }
    if (normalize(username) != null) {
      return UserAccount.LoginType.USERNAME;
    }
    return null;
  }

  private void validatePasswordRequired(String password) {
    if (normalize(password) == null) {
      throw new RuntimeException("Password is required to create a driver login account");
    }
  }

  private boolean isDriverRole(UserAccount account) {
    String roleName = account != null && account.getRole() != null ? account.getRole().getName() : null;
    return roleName != null && "DRIVER".equalsIgnoreCase(roleName.trim());
  }

  private String getAccountRoleName(UserAccount account) {
    if (account == null || account.getRole() == null || normalize(account.getRole().getName()) == null) {
      return "UNKNOWN";
    }
    return account.getRole().getName().trim();
  }

  private String requirePassword(String password) {
    validatePasswordRequired(password);
    return password;
  }

  
  private void deactivateDriverInOtherTenants(UUID driverId, UUID currentTenantId, UUID updatedBy) {
    driverTenantMappingRepository.findByDriver_Id(driverId).stream()
        .filter(mapping -> !mapping.getTenant().getId().equals(currentTenantId))
        .forEach(mapping -> {
          mapping.setActive(false);
          mapping.setUpdatedBy(updatedBy.toString());
          driverTenantMappingRepository.save(mapping);
        });
  }

  private void updateDriverUserAccountWithTenantId(UserAccount account, Tenant currentTenant, UUID updatedBy) {
    if (account == null) {
      return;
    }
    account.setTenant(currentTenant);
    account.setUpdatedBy(updatedBy.toString());
    userAccountRepository.save(account);
  }
}
