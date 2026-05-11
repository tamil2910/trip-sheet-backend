package com.example.trip_sheet_backend.controllers;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.AuthDtos.LoginRequestDto;
import com.example.trip_sheet_backend.dtos.AuthDtos.LoginUserResponseDTO;
import com.example.trip_sheet_backend.dtos.UserAccountDtos.UserAccountByFormDto;
import com.example.trip_sheet_backend.models.Admin;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.Permission;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.RoleGroup;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.AdminRepository;
import com.example.trip_sheet_backend.repositories.DriverRepository;
import com.example.trip_sheet_backend.repositories.PermissionRepository;
import com.example.trip_sheet_backend.repositories.RoleGroupRepository;
import com.example.trip_sheet_backend.repositories.RoleRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.security.GoogleAuthService;
import com.example.trip_sheet_backend.security.JwtTokenUtil;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.util.Value;

import jakarta.validation.Valid;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final UserAccountRepository userAccountRepository;
  private final JwtTokenUtil jwtTokenUtil;
  private final RoleGroupRepository roleGroupRepository;
  @Autowired
  private final PasswordEncoder passwordEncoder;
  private final RoleRepository roleRepository;
  private final GoogleAuthService googleAuthService;
  private final AdminRepository adminRepository;
    private final TenantRepository tenantRepository;
    private final PermissionRepository permissionRepository;
    private final DriverRepository driverRepository;
  private final ModelMapper mapper;

  @Value("${GOOGLE_AUTH_CLIENT_ID}")
  private String googleClientId; // 👈 inject env variable here
  
  public AuthController(UserAccountRepository userAccountRepository, JwtTokenUtil jwtTokenUtil, GoogleAuthService googleAuthService,
      PasswordEncoder passwordEncoder, AdminRepository adminRepository, TenantRepository tenantRepository, ModelMapper mapper, PermissionRepository permissionRepository, RoleRepository roleRepository,RoleGroupRepository roleGroupRepository, DriverRepository driverRepository) {
    this.userAccountRepository = userAccountRepository;
    this.jwtTokenUtil = jwtTokenUtil;
    this.roleGroupRepository = roleGroupRepository;
    this.googleAuthService = googleAuthService;
    this.passwordEncoder = passwordEncoder;
    this.adminRepository = adminRepository;
        this.tenantRepository = tenantRepository;
    this.mapper = mapper;
    this.permissionRepository = permissionRepository;
    this.roleRepository = roleRepository;
        this.driverRepository = driverRepository;
  }

  @PreAuthorize("permitAll()")
  @PostMapping("/register") // admin registration via admin portal
  public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody UserAccountByFormDto body) {

    if (body.getEmail() != null && userAccountRepository.existsByEmail(body.getEmail())) {
      throw new RuntimeException("Email already exists");
    }

    if (body.getPhone() != null && userAccountRepository.existsByPhone(body.getPhone())) {
        throw new RuntimeException("Phone already exists");
    }

    if (body.getTenantType() == null) {
        throw new RuntimeException("Tenant type is required");
    }

    if (body.getRole() == null) {
        throw new RuntimeException("Role is required!");
    }

    UserAccount payload = mapper.map(body, UserAccount.class);

    Role role = this.roleRepository.findByName(body.getRole().getName()).orElseThrow(
      () -> new RuntimeException("Role is not available in db!"));

    // Encrypt password BEFORE save
    if (body.getPassword() != null) {
        payload.setPassword(passwordEncoder.encode(body.getPassword()));
    }

    // Assign Role entity
    payload.setRole(role);
    payload.setTenant(null);
    payload.setTenantType(body.getTenantType());


    UserAccount result = userAccountRepository.save(payload);
    if ("DRIVER".equalsIgnoreCase(body.getRole().getName())) {
        attachDriverResources(result, body.getFullName());
    }

    if ("ADMIN".equals(body.getRole().getName())) {

        RoleGroup preTenantGroup =
            roleGroupRepository.findByNameAndTenantIsNull("ADMIN_PRE_TENANT")
                .orElseThrow(() -> new RuntimeException("ADMIN_PRE_TENANT role group not found"));

        result.getRoleGroups().add(preTenantGroup);
        userAccountRepository.save(result);

        Admin adminPayload = new Admin();
        adminPayload.setUserAccount(result);
        Admin adminResult = adminRepository.saveAndFlush(adminPayload);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Admin created successfully", adminResult));
    }


    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Resource created successfully", result));
  }


  @PostMapping("/login")
  public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequestDto payload) {
    String identifier = payload.getIdentifier();
    String password = payload.getPassword();

    // Optional<UserAccount> user = Optional.empty();
    Optional<UserAccount> foundUser;


    if (identifier.contains("@")) {
        foundUser = this.userAccountRepository.findByEmail(identifier);
    } else if (identifier.matches("\\d+")) {
        String phoneNumber =identifier;
        foundUser = this.userAccountRepository.findByPhone(phoneNumber);
    } else {
        foundUser = this.userAccountRepository.findByUsername(identifier);
    }

    if (foundUser.isEmpty() || !passwordEncoder.matches(password, foundUser.get().getPassword())) {
        return new ApiResponse<>(false, "Invalid credentials", null);
    }

    UserAccount user = foundUser.get();
        Tenant resolvedTenant = user.getTenant();

        if (resolvedTenant == null) {
            resolvedTenant = tenantRepository.findByAdmin_UserAccount_Id(user.getId()).orElse(null);
        }

    // Load permissions from all assigned RoleGroups
    Set<String> effectivePermissions;

    if (user.getRoleGroups() != null && !user.getRoleGroups().isEmpty()) {
        effectivePermissions =
            user.getRoleGroups().stream()
                .flatMap(group -> group.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
    } else {
        effectivePermissions = Set.of();
    }


    // Generate JWT
    String token = resolvedTenant != null
        ? jwtTokenUtil.generateToken(user, effectivePermissions, "user_login", identifier, resolvedTenant)
        : jwtTokenUtil.generateToken(user, effectivePermissions, "user_login", identifier);

    // Build flat DTO (avoid returning entity)
    LoginUserResponseDTO dto = new LoginUserResponseDTO();
    dto.setId(user.getId());
    dto.setRole(user.getRole() != null ? user.getRole().getName() : null);
    dto.setRoleGroups(
        user.getRoleGroups()
            .stream()
            .map(RoleGroup::getName)
            .collect(Collectors.toSet())
    );
    dto.setUsername(user.getUsername());
    // dto.setTenantType(user.getTenantType() != null ? user.getTenantType().name() : null);

    if (resolvedTenant != null && resolvedTenant.getTenantType() != null) {
        dto.setTenantType(resolvedTenant.getTenantType().name());
    } else {
        dto.setTenantType(null);
    }


    if (resolvedTenant != null) {
        dto.setTenantId(resolvedTenant.getId());
        dto.setTenantName(resolvedTenant.getTenantName());
    }

    if(user.getTenantType() != null) {
        dto.setTenantType(user.getTenantType().name());
    }


    dto.setPermissions(effectivePermissions);
    // Fetch full Permission objects for grouping
    List<Permission> permissionObjects =
            permissionRepository.findAllByNameIn(effectivePermissions);

    // Group by moduleName
    Map<String, Set<String>> grouped = new HashMap<>();

    for (Permission p : permissionObjects) {
        String module = p.getModuleName();  // e.g. "INVOICE", "DUTY", "EXPENSE"
        grouped.putIfAbsent(module, new HashSet<>());
        grouped.get(module).add(p.getName());
    }

    dto.setModulePermissions(grouped);
    

    Map<String, Object> response = new HashMap<>();
    response.put("token", token);
    response.put("user", dto);
    
    return new ApiResponse<>(true, "Login successful", response);
  }

    @PostMapping("/google-signup")
    public ApiResponse<Map<String, Object>> googleSignup(@RequestBody Map<String, Object> payload) {

        try {
            Boolean newUser = false;
            String idToken = (String) payload.get("idToken");

            // 1️⃣ Verify Google token
            Payload googlePayload = googleAuthService.verifyToken(idToken);
            if (googlePayload == null) {
                return new ApiResponse<>(false, "Invalid Google ID token", null);
            }

            String email = googlePayload.getEmail();
            String name = (String) googlePayload.get("name");
            String picture = (String) googlePayload.get("picture");
            String googleId = googlePayload.getSubject();
            String requestedUsername = normalizeString(payload.get("username"));
            if (requestedUsername == null) {
                requestedUsername = normalizeString(payload.get("userName"));
            }
            String requestedFullName = normalizeString(payload.get("fullName"));
            if (requestedFullName == null) {
                requestedFullName = name;
            }

            Optional<UserAccount> existingUserOpt = userAccountRepository.findByEmail(email);
            UserAccount user;
            Role resolvedRole = null;

            // 2️⃣ USER DOES NOT EXIST → CREATE
            if (existingUserOpt.isEmpty()) {

                // ---------- ROLE RESOLUTION ----------
                Object roleObj = payload.get("role");

                if (roleObj instanceof Map<?, ?> roleMap) {
                    Object idObj = roleMap.get("id");
                    if (idObj != null) {
                        UUID roleId = UUID.fromString(idObj.toString());
                        resolvedRole = roleRepository.findById(roleId)
                                .orElseThrow(() -> new RuntimeException("Role not found for ID"));
                    }
                }
                else if (roleObj instanceof String roleStr) {
                    try {
                        UUID roleId = UUID.fromString(roleStr);
                        resolvedRole = roleRepository.findById(roleId)
                                .orElseThrow(() -> new RuntimeException("Role not found for ID"));
                    } catch (IllegalArgumentException ex) {
                        resolvedRole = roleRepository.findByName(roleStr)
                                .orElseThrow(() -> new RuntimeException("Role not found for name"));
                    }
                }

                // ---------- FALLBACK ROLE ----------
                if (resolvedRole == null) {
                    resolvedRole = roleRepository.findByName("USER")
                            .orElseThrow(() -> new RuntimeException("Default USER role not found"));
                }

                // ---------- CREATE USER ----------
                user = new UserAccount();
                String baseName = (requestedUsername != null) ? requestedUsername : requestedFullName;
                if (baseName == null || baseName.trim().isEmpty()) {
                    baseName = email.split("@")[0];
                }

                user.setUsername(generateUniqueUsername(baseName));
                user.setEmail(email);
                user.setGoogleId(googleId);
                user.setProfilePicture(picture);
                user.setRole(resolvedRole);
                user.setLoginType(UserAccount.LoginType.GOOGLE);
                user.setTenant(null); // IMPORTANT

                userAccountRepository.saveAndFlush(user);
                newUser = true;

                // ---------- 🔐 ADMIN PRE-TENANT ROLE GROUP ----------
                if ("ADMIN".equals(resolvedRole.getName())) {

                    RoleGroup preTenantGroup =
                            roleGroupRepository.findByNameAndTenantIsNull("ADMIN_PRE_TENANT")
                                    .orElseThrow(() ->
                                            new RuntimeException("ADMIN_PRE_TENANT role group not found"));

                    user.getRoleGroups().add(preTenantGroup);
                    userAccountRepository.save(user);
                }

                if ("DRIVER".equalsIgnoreCase(resolvedRole.getName())) {
                    attachDriverResources(user, requestedFullName);
                }

            } 
            // 3️⃣ USER EXISTS → LOGIN
            else {
                user = existingUserOpt.get();
                resolvedRole = user.getRole();
            }

            // 4️⃣ LOAD PERMISSIONS FROM ROLE GROUPS
            Set<String> effectivePermissions;

            if (user.getRoleGroups() != null && !user.getRoleGroups().isEmpty()) {
                effectivePermissions =
                        user.getRoleGroups()
                            .stream()
                            .flatMap(rg -> rg.getPermissions().stream())
                            .map(Permission::getName)
                            .collect(Collectors.toSet());
            } else {
                effectivePermissions = Set.of();
            }

            // 5️⃣ GENERATE TOKEN
            String token = jwtTokenUtil.generateToken(
                    user,
                    effectivePermissions,
                    "google_login",
                    user.getEmail()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", user);
            response.put("newUser", newUser);

            return new ApiResponse<>(true, "Google login/signup successful", response);

        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(false, "Google verification failed: " + e.getMessage(), null);
        }
    }

  private String generateUniqueUsername(String baseUsername) {
    String sanitized = baseUsername.trim().replaceAll("\\s+", "_").toLowerCase();
    Optional<UserAccount> existing = this.userAccountRepository.findByUsername(sanitized);
    if (existing.isPresent()) {
        sanitized = sanitized + "_" + Instant.now().getEpochSecond();
    }
    return sanitized;
  }

    private void attachDriverResources(UserAccount userAccount, String preferredFullName) {
        if (userAccount == null) {
            throw new RuntimeException("User account is required to create driver resources");
        }

        if (driverRepository.findByAccount_Id(userAccount.getId()).isPresent()) {
            ensureDriverRoleGroup(userAccount);
            return;
        }

        String resolvedFullName = normalizeDriverFullName(preferredFullName, userAccount);

        Driver driver = new Driver();
        driver.setFullName(resolvedFullName);
        driver.setProfilePicture(userAccount.getProfilePicture());
        driver.setActive(Boolean.TRUE.equals(userAccount.getIsActive()));
        driver.setAvailable(true);
        driver.setAccount(userAccount);
        driver.setCreatedBy(userAccount.getId().toString());
        driver.setUpdatedBy(userAccount.getId().toString());
        driverRepository.save(driver);

        ensureDriverRoleGroup(userAccount);
    }

    private void ensureDriverRoleGroup(UserAccount userAccount) {
        RoleGroup driverGroup = roleGroupRepository.findByNameAndTenantIsNull("DRIVER_GLOBAL_PERMISSIONS")
                .orElseThrow(() -> new RuntimeException("DRIVER_GLOBAL_PERMISSIONS role group not found"));

        if (userAccount.getRoleGroups() == null) {
            userAccount.setRoleGroups(new HashSet<>());
        }
        userAccount.getRoleGroups().add(driverGroup);
        userAccountRepository.save(userAccount);
    }

    private String normalizeDriverFullName(String preferredFullName, UserAccount userAccount) {
        if (preferredFullName != null && !preferredFullName.trim().isEmpty()) {
            return preferredFullName.trim();
        }

        if (userAccount.getUsername() != null && !userAccount.getUsername().trim().isEmpty()) {
            return userAccount.getUsername().trim();
        }

        if (userAccount.getEmail() != null && !userAccount.getEmail().trim().isEmpty()) {
            return userAccount.getEmail().trim().split("@")[0];
        }

        if (userAccount.getPhone() != null && !userAccount.getPhone().trim().isEmpty()) {
            return userAccount.getPhone().trim();
        }

        return "Driver";
    }

    private String normalizeString(Object value) {
        if (!(value instanceof String text)) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }



}
