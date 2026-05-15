package com.example.trip_sheet_backend.services.TenantService;

import java.time.Instant;
import java.security.SecureRandom;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.common.services.UniqueCodeGeneratorService;
import com.example.trip_sheet_backend.common.services.GlobalBaseServiceImp;
import com.example.trip_sheet_backend.dtos.TenantDtos.TenantLinkResponseDto;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.RoleGroup;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.models.VendorOrganisationTax;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.models.VendorPartnerTax;
import com.example.trip_sheet_backend.models.Tax;
import com.example.trip_sheet_backend.repositories.RoleGroupRepository;
import com.example.trip_sheet_backend.repositories.RoleRepository;
import com.example.trip_sheet_backend.repositories.TaxRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationTaxRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerTaxRepository;
import com.example.trip_sheet_backend.services.EmailService;

@Service
public class TenantServiceImp extends GlobalBaseServiceImp<Tenant, UUID> implements TenantService {
  private final TenantRepository tenantRepository;
  private final VendorPartnerRepository vendorPartnerRepository;
  private final VendorOrganisationRepository vendorOrganisationRepository;
        private final VendorPartnerTaxRepository vendorPartnerTaxRepository;
        private final VendorOrganisationTaxRepository vendorOrganisationTaxRepository;
        private final TaxRepository taxRepository;
        private final UserAccountRepository userAccountRepository;
        private final RoleRepository roleRepository;
        private final RoleGroupRepository roleGroupRepository;
        private final PasswordEncoder passwordEncoder;
        private final EmailService emailService;
        private final UniqueCodeGeneratorService uniqueCodeGeneratorService;
        private static final SecureRandom SECURE_RANDOM = new SecureRandom();
        private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
        private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        private static final String DIGITS = "0123456789";
        private static final String SYMBOLS = "@#$%&*!?";
        private static final String ALL = LOWER + UPPER + DIGITS + SYMBOLS;


  public TenantServiceImp(TenantRepository repository, VendorPartnerRepository vendorPartnerRepository, 
                VendorOrganisationRepository vendorOrganisationRepository,
                VendorPartnerTaxRepository vendorPartnerTaxRepository,
                VendorOrganisationTaxRepository vendorOrganisationTaxRepository,
                TaxRepository taxRepository,
                UserAccountRepository userAccountRepository,
                RoleRepository roleRepository,
                RoleGroupRepository roleGroupRepository,
                PasswordEncoder passwordEncoder,
                EmailService emailService,
                UniqueCodeGeneratorService uniqueCodeGeneratorService) {
    super(repository);
    this.tenantRepository = repository;
    this.vendorPartnerRepository = vendorPartnerRepository;
    this.vendorOrganisationRepository = vendorOrganisationRepository;
                this.vendorPartnerTaxRepository = vendorPartnerTaxRepository;
                this.vendorOrganisationTaxRepository = vendorOrganisationTaxRepository;
                this.taxRepository = taxRepository;
                this.userAccountRepository = userAccountRepository;
                this.roleRepository = roleRepository;
                this.roleGroupRepository = roleGroupRepository;
                this.passwordEncoder = passwordEncoder;
                this.emailService = emailService;
                this.uniqueCodeGeneratorService = uniqueCodeGeneratorService;
  }

    // GLOBAL READ ONLY FOR USERACCOUNT
  public Tenant findByIdResource(UUID id) {
      return repository.findById(id).orElse(null);
  }

  @Override
  public Tenant findByUniqueCode(String tenantUniqueCode) {
      String normalizedCode = normalizeTenantUniqueCode(tenantUniqueCode);
      return tenantRepository.findByTenantUniqueCodeIgnoreCase(normalizedCode)
              .orElseThrow(() -> new RuntimeException("Tenant not found for unique code: " + normalizedCode));
  }

  @Override
  public Tenant create(Tenant payload) {
      assignTenantUniqueCodeIfMissing(payload);
      return super.create(payload);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TenantLinkResponseDto linkExistingTenantByUniqueCode(Tenant loggedInTenant, String tenantUniqueCode, UUID createdBy) {
          validateVendorTenant(loggedInTenant);

          Tenant targetTenant = findByUniqueCode(tenantUniqueCode);

          if (targetTenant.getId().equals(loggedInTenant.getId())) {
                  throw new RuntimeException("Vendor cannot add itself");
          }

          if (targetTenant.getTenantType() == Tenant.TenantType.VENDOR) {
                  Optional<VendorPartner> existingPartner = vendorPartnerRepository
                          .findByPrimaryVendorAndPartnerVendor(loggedInTenant, targetTenant);

                  if (existingPartner.isPresent()) {
                          return new TenantLinkResponseDto("VENDOR_PARTNER", existingPartner.get().getId(), targetTenant, true);
                  }

                  VendorPartner partner = new VendorPartner();
                  partner.setPrimaryVendor(loggedInTenant);
                  partner.setPartnerVendor(targetTenant);
                  partner.setContractStatus(null);
                  partner.setOnboardedAt(Instant.now().getEpochSecond());
                  partner.setCreatedBy(createdBy != null ? createdBy.toString() : null);

                  VendorPartner savedPartner = vendorPartnerRepository.save(partner);
                  return new TenantLinkResponseDto("VENDOR_PARTNER", savedPartner.getId(), targetTenant, false);
          }

          Optional<VendorOrganisation> existingOrganisation = vendorOrganisationRepository
                  .findByVendorAndOrganisation_Id(loggedInTenant, targetTenant.getId());

          if (existingOrganisation.isPresent()) {
                  return new TenantLinkResponseDto("VENDOR_ORGANISATION", existingOrganisation.get().getId(), targetTenant, true);
          }

          VendorOrganisation organisation = new VendorOrganisation();
          organisation.setVendor(loggedInTenant);
          organisation.setOrganisation(targetTenant);
          organisation.setContractStatus(null);
          organisation.setActive(true);
          organisation.setOnboardedAt(Instant.now().getEpochSecond());
          organisation.setCreatedBy(createdBy != null ? createdBy.toString() : null);

          VendorOrganisation savedOrganisation = vendorOrganisationRepository.save(organisation);
          return new TenantLinkResponseDto("VENDOR_ORGANISATION", savedOrganisation.getId(), targetTenant, false);
  }


  @Transactional(rollbackFor = Exception.class)
  public TenantOnboardingResult createOrGetPartnerVendor(
          Tenant requestTenant,
          Tenant primaryVendor,
          UUID createdBy,
          List<UUID> taxIds
  ) {

          boolean newlyCreated = false;
          boolean onboardingUserCreated = false;
          boolean credentialsEmailSent = false;
          String onboardingNote = "";
          Optional<Tenant> existingTenant = findExistingTenant(requestTenant);

          Tenant partnerTenant;
          if (existingTenant.isPresent()) {
                  partnerTenant = existingTenant.get();
                  onboardingNote = "Tenant already exists";
          } else {
                  partnerTenant = createNewPartnerTenant(requestTenant, createdBy);
                  newlyCreated = true;
                  OnboardingDispatchResult dispatchResult = createOnboardingUserAndSendCredentials(partnerTenant, createdBy);
                  onboardingUserCreated = dispatchResult.userCreated();
                  credentialsEmailSent = dispatchResult.emailSent();
                  onboardingNote = dispatchResult.note();
          }

      // 2️⃣ Prevent self-linking
      if (partnerTenant.getId().equals(primaryVendor.getId())) {
          throw new RuntimeException("Vendor cannot be its own partner");
      }

      // 3️⃣ Check existing partnership
      boolean alreadyLinked = vendorPartnerRepository
              .existsByPrimaryVendorAndPartnerVendor(
                      primaryVendor,
                      partnerTenant
              );

      if (!alreadyLinked) {
          VendorPartner partner = new VendorPartner();
          partner.setPrimaryVendor(primaryVendor);
          partner.setPartnerVendor(partnerTenant);
          partner.setContractStatus(null);
          partner.setOnboardedAt(Instant.now().getEpochSecond());
          partner.setCreatedBy(createdBy.toString());

          VendorPartner savedPartner = vendorPartnerRepository.save(partner);
          attachTaxesToVendorPartner(primaryVendor, savedPartner, taxIds, createdBy);
      } else {
          VendorPartner existingPartner = vendorPartnerRepository
                  .findByPrimaryVendorAndPartnerVendor(primaryVendor, partnerTenant)
                  .orElseThrow(() -> new RuntimeException("Vendor partner relationship not found"));
          attachTaxesToVendorPartner(primaryVendor, existingPartner, taxIds, createdBy);
      }

                  return new TenantOnboardingResult(
                          partnerTenant,
                          newlyCreated,
                          onboardingUserCreated,
                          credentialsEmailSent,
                          onboardingNote
                  );
  }

  private Tenant createNewPartnerTenant(
          Tenant requestTenant,
          UUID createdBy
  ) {
      requestTenant.setTenantType(Tenant.TenantType.VENDOR);
      requestTenant.setIsActive(true);
      requestTenant.setCreatedBy(createdBy.toString());
      assignTenantUniqueCodeIfMissing(requestTenant);

      return tenantRepository.save(requestTenant);
  }


  @Transactional(rollbackFor = Exception.class)
  public TenantOnboardingResult createOrGetCorporateTenant(
          Tenant requestTenant,
          Tenant primaryVendor,
          UUID createdBy,
          List<UUID> taxIds
  ) {

          boolean newlyCreated = false;
          boolean onboardingUserCreated = false;
          boolean credentialsEmailSent = false;
          String onboardingNote = "";
          Optional<Tenant> existingTenant = findExistingTenant(requestTenant);

          Tenant organisationTenant;
          if (existingTenant.isPresent()) {
                  organisationTenant = existingTenant.get();
                  onboardingNote = "Tenant already exists";
          } else {
                  organisationTenant = createNewCorporateTenant(requestTenant, createdBy);
                  newlyCreated = true;
                  OnboardingDispatchResult dispatchResult = createOnboardingUserAndSendCredentials(organisationTenant, createdBy);
                  onboardingUserCreated = dispatchResult.userCreated();
                  credentialsEmailSent = dispatchResult.emailSent();
                  onboardingNote = dispatchResult.note();
          }

      // 2️⃣ Prevent self-linking
      if (organisationTenant.getId().equals(primaryVendor.getId())) {
          throw new RuntimeException("Vendor cannot be its own client/corporate tenant");
      }

      // 3️⃣ Check existing partnership
      boolean alreadyLinked = vendorOrganisationRepository
              .existsByVendorAndOrganisation(
                      primaryVendor,
                      organisationTenant
              );

      if (!alreadyLinked) {
          VendorOrganisation organisation = new VendorOrganisation();
          organisation.setVendor(primaryVendor);
          organisation.setOrganisation(organisationTenant);
          organisation.setContractStatus(null);
          organisation.setOnboardedAt(Instant.now().getEpochSecond());
          organisation.setCreatedBy(createdBy.toString());

          VendorOrganisation savedOrganisation = vendorOrganisationRepository.save(organisation);
          attachTaxesToVendorOrganisation(primaryVendor, savedOrganisation, taxIds, createdBy);
      } else {
          VendorOrganisation existingOrganisation = vendorOrganisationRepository
                  .findByVendorAndOrganisation_Id(primaryVendor, organisationTenant.getId())
                  .orElseThrow(() -> new RuntimeException("Vendor organisation relationship not found"));
          attachTaxesToVendorOrganisation(primaryVendor, existingOrganisation, taxIds, createdBy);
      }

                  return new TenantOnboardingResult(
                          organisationTenant,
                          newlyCreated,
                          onboardingUserCreated,
                          credentialsEmailSent,
                          onboardingNote
                  );
  }

  private Tenant createNewCorporateTenant(
          Tenant requestTenant,
          UUID createdBy
  ) {
      requestTenant.setTenantType(Tenant.TenantType.ORGANISATION);
      requestTenant.setIsActive(true);
      requestTenant.setCreatedBy(createdBy.toString());
      assignTenantUniqueCodeIfMissing(requestTenant);

      return tenantRepository.save(requestTenant);
  }

  private void assignTenantUniqueCodeIfMissing(Tenant tenant) {
          if (tenant == null) {
                  throw new RuntimeException("Tenant payload is required");
          }

          if (tenant.getTenantUniqueCode() != null && !tenant.getTenantUniqueCode().trim().isEmpty()) {
                  return;
          }

          if (tenant.getTenantType() == null) {
                  throw new RuntimeException("Tenant type is required to generate tenant code");
          }

          String prefix = switch (tenant.getTenantType()) {
                  case VENDOR -> "VEN";
                  case ORGANISATION -> "ORG";
          };

          tenant.setTenantUniqueCode(
                  uniqueCodeGeneratorService.generateUniqueCode(prefix, tenantRepository::existsByTenantUniqueCode)
          );
  }

  private void validateVendorTenant(Tenant loggedInTenant) {
          if (loggedInTenant == null) {
                  throw new RuntimeException("Tenant not found in token");
          }

          if (loggedInTenant.getTenantType() != Tenant.TenantType.VENDOR) {
                  throw new RuntimeException("Only vendors can add tenants by unique code");
          }
  }

  private String normalizeTenantUniqueCode(String tenantUniqueCode) {
          if (tenantUniqueCode == null || tenantUniqueCode.trim().isEmpty()) {
                  throw new RuntimeException("Tenant unique code is required");
          }

          return tenantUniqueCode.trim().toUpperCase();
  }

  private Optional<Tenant> findExistingTenant(Tenant requestTenant) {
          if (requestTenant.getGstNumber() != null && !requestTenant.getGstNumber().isBlank()) {
                  Optional<Tenant> byGst = tenantRepository.findByGstNumber(requestTenant.getGstNumber());
                  if (byGst.isPresent()) {
                          return byGst;
                  }
          }

          if (requestTenant.getContactEmail() != null && !requestTenant.getContactEmail().isBlank()) {
                  return tenantRepository.findByContactEmail(requestTenant.getContactEmail().trim().toLowerCase());
          }

          return Optional.empty();
  }

  private void attachTaxesToVendorPartner(Tenant ownerTenant, VendorPartner vendorPartner, List<UUID> taxIds, UUID createdBy) {
          List<Tax> taxes = resolveTaxesForOwner(ownerTenant, taxIds);

          for (Tax tax : taxes) {
                  boolean exists = vendorPartnerTaxRepository.existsByTenant_IdAndVendorPartner_IdAndTax_Id(
                          ownerTenant.getId(),
                          vendorPartner.getId(),
                          tax.getId()
                  );

                  if (exists) {
                          continue;
                  }

                  VendorPartnerTax mapping = new VendorPartnerTax();
                  mapping.setTenant(ownerTenant);
                  mapping.setVendorPartner(vendorPartner);
                  mapping.setTax(tax);
                  if (createdBy != null) {
                          mapping.setCreatedBy(createdBy.toString());
                          mapping.setUpdatedBy(createdBy.toString());
                  }
                  vendorPartnerTaxRepository.save(mapping);
          }
  }

  private void attachTaxesToVendorOrganisation(Tenant ownerTenant, VendorOrganisation vendorOrganisation, List<UUID> taxIds, UUID createdBy) {
          List<Tax> taxes = resolveTaxesForOwner(ownerTenant, taxIds);

          for (Tax tax : taxes) {
                  boolean exists = vendorOrganisationTaxRepository.existsByTenant_IdAndVendorOrganisation_IdAndTax_Id(
                          ownerTenant.getId(),
                          vendorOrganisation.getId(),
                          tax.getId()
                  );

                  if (exists) {
                          continue;
                  }

                  VendorOrganisationTax mapping = new VendorOrganisationTax();
                  mapping.setTenant(ownerTenant);
                  mapping.setVendorOrganisation(vendorOrganisation);
                  mapping.setTax(tax);
                  if (createdBy != null) {
                          mapping.setCreatedBy(createdBy.toString());
                          mapping.setUpdatedBy(createdBy.toString());
                  }
                  vendorOrganisationTaxRepository.save(mapping);
          }
  }

  private List<Tax> resolveTaxesForOwner(Tenant ownerTenant, List<UUID> taxIds) {
          if (taxIds == null || taxIds.isEmpty()) {
                  return List.of();
          }

          if (ownerTenant == null || ownerTenant.getId() == null) {
                  throw new RuntimeException("Tenant not found in token");
          }

          List<UUID> uniqueTaxIds = new java.util.ArrayList<>(new LinkedHashSet<>(taxIds));
          List<Tax> taxes = taxRepository.findByIdInAndTenant_Id(uniqueTaxIds, ownerTenant.getId());

          if (taxes.size() != uniqueTaxIds.size()) {
                  throw new RuntimeException("One or more taxIds are invalid for the current tenant");
          }

          return taxes;
  }

  private OnboardingDispatchResult createOnboardingUserAndSendCredentials(Tenant tenant, UUID createdBy) {
          String email = tenant.getContactEmail() == null ? null : tenant.getContactEmail().trim().toLowerCase();
          if (email == null || email.isBlank()) {
                  throw new RuntimeException("Tenant contact email is required for onboarding");
          }

          if (userAccountRepository.existsByEmail(email)) {
                  return new OnboardingDispatchResult(false, false, "User already exists with this email. Credentials not sent");
          }

          Role adminRole = roleRepository.findByName("ADMIN")
                  .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

          RoleGroup adminFullGroup = roleGroupRepository.findByNameAndTenantIsNull("ADMIN_FULL")
                  .orElseThrow(() -> new RuntimeException("ADMIN_FULL role group not found"));

          String rawPassword = generateStrongPassword(12);

          UserAccount tenantAdmin = new UserAccount();
          tenantAdmin.setUsername(generateUniqueUsername(tenant.getTenantName()));
          tenantAdmin.setEmail(email);
          tenantAdmin.setPassword(passwordEncoder.encode(rawPassword));
          tenantAdmin.setLoginType(UserAccount.LoginType.EMAIL);
          tenantAdmin.setRole(adminRole);
          tenantAdmin.setTenant(tenant);
          tenantAdmin.setIsActive(true);
          tenantAdmin.setCreatedBy(createdBy.toString());

          if (tenant.getTenantType() == Tenant.TenantType.VENDOR) {
                  tenantAdmin.setTenantType(UserAccount.TenantType.VENDOR);
          } else {
                  tenantAdmin.setTenantType(UserAccount.TenantType.ORGANISATION);
          }

          tenantAdmin.getRoleGroups().add(adminFullGroup);

          userAccountRepository.save(tenantAdmin);

          emailService.sendTenantOnboardingEmail(
                  email,
                  tenant.getTenantName(),
                  tenantAdmin.getUsername(),
                  rawPassword
          );

          return new OnboardingDispatchResult(true, true, "Onboarding user created and credentials sent");
  }

  private String generateStrongPassword(int length) {
          if (length < 8) {
                  throw new IllegalArgumentException("Password length must be at least 8");
          }

          StringBuilder password = new StringBuilder(length);
          password.append(randomChar(LOWER));
          password.append(randomChar(UPPER));
          password.append(randomChar(DIGITS));
          password.append(randomChar(SYMBOLS));

          for (int i = 4; i < length; i++) {
                  password.append(randomChar(ALL));
          }

          char[] chars = password.toString().toCharArray();
          for (int i = chars.length - 1; i > 0; i--) {
                  int j = SECURE_RANDOM.nextInt(i + 1);
                  char temp = chars[i];
                  chars[i] = chars[j];
                  chars[j] = temp;
          }

          return new String(chars);
  }

  private String randomChar(String source) {
          return String.valueOf(source.charAt(SECURE_RANDOM.nextInt(source.length())));
  }

  private String generateUniqueUsername(String tenantName) {
          String base = (tenantName == null || tenantName.isBlank())
                  ? "tenant_admin"
                  : tenantName.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");

          base = base.replaceAll("^_+|_+$", "");
          if (base.isBlank()) {
                  base = "tenant_admin";
          }

          String candidate = base;
          int attempts = 0;
          while (userAccountRepository.findByUsername(candidate).isPresent()) {
                  candidate = base + "_" + (1000 + SECURE_RANDOM.nextInt(9000));
                  attempts++;
                  if (attempts > 20) {
                          candidate = base + "_" + UUID.randomUUID().toString().substring(0, 8);
                          break;
                  }
          }
          return candidate;
  }

        private record OnboardingDispatchResult(boolean userCreated, boolean emailSent, String note) {}
}
