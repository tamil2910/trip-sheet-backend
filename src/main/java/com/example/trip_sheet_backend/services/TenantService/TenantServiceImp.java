package com.example.trip_sheet_backend.services.TenantService;

import java.time.Instant;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.common.services.GlobalBaseServiceImp;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.RoleGroup;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.repositories.RoleGroupRepository;
import com.example.trip_sheet_backend.repositories.RoleRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRepository;
import com.example.trip_sheet_backend.services.EmailService;

@Service
public class TenantServiceImp extends GlobalBaseServiceImp<Tenant, UUID> implements TenantService {
  private final TenantRepository tenantRepository;
  private final VendorPartnerRepository vendorPartnerRepository;
  private final VendorOrganisationRepository vendorOrganisationRepository;
        private final UserAccountRepository userAccountRepository;
        private final RoleRepository roleRepository;
        private final RoleGroupRepository roleGroupRepository;
        private final PasswordEncoder passwordEncoder;
        private final EmailService emailService;
        private static final SecureRandom SECURE_RANDOM = new SecureRandom();
        private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
        private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        private static final String DIGITS = "0123456789";
        private static final String SYMBOLS = "@#$%&*!?";
        private static final String ALL = LOWER + UPPER + DIGITS + SYMBOLS;


  public TenantServiceImp(TenantRepository repository, VendorPartnerRepository vendorPartnerRepository, 
                VendorOrganisationRepository vendorOrganisationRepository,
                UserAccountRepository userAccountRepository,
                RoleRepository roleRepository,
                RoleGroupRepository roleGroupRepository,
                PasswordEncoder passwordEncoder,
                EmailService emailService) {
    super(repository);
    this.tenantRepository = repository;
    this.vendorPartnerRepository = vendorPartnerRepository;
    this.vendorOrganisationRepository = vendorOrganisationRepository;
                this.userAccountRepository = userAccountRepository;
                this.roleRepository = roleRepository;
                this.roleGroupRepository = roleGroupRepository;
                this.passwordEncoder = passwordEncoder;
                this.emailService = emailService;
  }

    // GLOBAL READ ONLY FOR USERACCOUNT
  public Tenant findByIdResource(UUID id) {
      return repository.findById(id).orElse(null);
  }


  @Transactional(rollbackFor = Exception.class)
  public TenantOnboardingResult createOrGetPartnerVendor(
          Tenant requestTenant,
          Tenant primaryVendor,
          UUID createdBy
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

          vendorPartnerRepository.save(partner);
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

      return tenantRepository.save(requestTenant);
  }


  @Transactional(rollbackFor = Exception.class)
  public TenantOnboardingResult createOrGetCorporateTenant(
          Tenant requestTenant,
          Tenant primaryVendor,
          UUID createdBy
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

          vendorOrganisationRepository.save(organisation);
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

      return tenantRepository.save(requestTenant);
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
