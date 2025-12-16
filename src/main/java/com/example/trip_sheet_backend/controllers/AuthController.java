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
import com.example.trip_sheet_backend.models.Permission;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.AdminRepository;
import com.example.trip_sheet_backend.repositories.PermissionRepository;
import com.example.trip_sheet_backend.repositories.RoleRepository;
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
  @Autowired
  private final PasswordEncoder passwordEncoder;
  private final RoleRepository roleRepository;
  private final GoogleAuthService googleAuthService;
  private final AdminRepository adminRepository;
  private final PermissionRepository permissionRepository;
  private final ModelMapper mapper;

  @Value("${GOOGLE_AUTH_CLIENT_ID}")
  private String googleClientId; // 👈 inject env variable here
  
  public AuthController(UserAccountRepository userAccountRepository, JwtTokenUtil jwtTokenUtil, RoleRepository roleRepository, GoogleAuthService googleAuthService,
    PasswordEncoder passwordEncoder, AdminRepository adminRepository,  ModelMapper mapper, PermissionRepository permissionRepository) {
    this.userAccountRepository = userAccountRepository;
    this.jwtTokenUtil = jwtTokenUtil;
    this.roleRepository = roleRepository;
    this.googleAuthService = googleAuthService;
    this.passwordEncoder = passwordEncoder;
    this.adminRepository = adminRepository;
    this.mapper = mapper;
    this.permissionRepository = permissionRepository;
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

    UserAccount result = userAccountRepository.save(payload);

    if ("ADMIN".equals(body.getRole().getName())) {
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

    // Load permissions from RoleGroup
    Set<String> effectivePermissions;
    if (user.getRoleGroup() != null && user.getRoleGroup().getPermissions() != null) {
        effectivePermissions = user.getRoleGroup()
                .getPermissions()
                .stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());
    } else {
        effectivePermissions = Set.of();
    }

    // Generate JWT
    String token = jwtTokenUtil.generateToken(
            user,
            effectivePermissions,
            "user_login",
            identifier
    );

    // Build flat DTO (avoid returning entity)
    LoginUserResponseDTO dto = new LoginUserResponseDTO();
    dto.setId(user.getId());
    dto.setRole(user.getRole() != null ? user.getRole().getName() : null);
    dto.setRoleGroup(user.getRoleGroup() != null ? user.getRoleGroup().getName() : null);
    dto.setUsername(user.getUsername());
    // dto.setTenantType(user.getTenantType() != null ? user.getTenantType().name() : null);

    if (user.getTenant() != null && user.getTenant().getTenantType() != null) {
        dto.setTenantType(user.getTenant().getTenantType().name());
    } else {
        dto.setTenantType(null);
    }


    if (user.getTenant() != null) {
        dto.setTenantId(user.getTenant().getId());
        dto.setTenantName(user.getTenant().getTenantName());
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
          String idToken = (String) payload.get("idToken");

          // ✅ Verify Google token
          Payload googlePayload = this.googleAuthService.verifyToken(idToken);
          if (googlePayload == null) {
              return new ApiResponse<>(false, "Invalid Google ID token", null);
          }

          String email = googlePayload.getEmail();
          String name = (String) googlePayload.get("name");
          String picture = (String) googlePayload.get("picture");
          String googleId = googlePayload.getSubject();

          Optional<UserAccount> existingUser = userAccountRepository.findByEmail(email);
          UserAccount user;

          if (existingUser.isEmpty()) {
              // ✅ Role resolution
              Object roleObj = payload.get("role");
              Role defaultRole = null;

              try {
                  if (roleObj instanceof Map<?, ?> roleMap) {
                      // Case 1: role = { "id": "uuid" }
                      Object idObj = roleMap.get("id");
                      if (idObj != null) {
                          UUID roleId = UUID.fromString(idObj.toString());
                          defaultRole = roleRepository.findById(roleId)
                                  .orElseThrow(() -> new RuntimeException("Role not found for ID: " + roleId));
                      }
                  } else if (roleObj instanceof String roleStr) {
                      // Case 2: role = "ADMIN" or "uuid"
                      try {
                          UUID roleId = UUID.fromString(roleStr);
                          defaultRole = roleRepository.findById(roleId)
                                  .orElseThrow(() -> new RuntimeException("Role not found for ID: " + roleId));
                      } catch (IllegalArgumentException e) {
                          // Not a UUID → treat as role name
                          defaultRole = roleRepository.findByName(roleStr)
                                  .orElseThrow(() -> new RuntimeException("Role not found for name: " + roleStr));
                      }
                  }
              } catch (Exception e) {
                  System.err.println("⚠️ Role resolution error: " + e.getMessage());
              }

              // ✅ Fallback role
              if (defaultRole == null) {
                  defaultRole = roleRepository.findByName("USER")
                          .orElseThrow(() -> new RuntimeException("Default role USER not found"));
              }

              // ✅ Create new user
              user = new UserAccount();
              String baseName = name != null ? name : email.split("@")[0];
              user.setUsername(generateUniqueUsername(baseName));
              user.setEmail(email);
              user.setGoogleId(googleId);
              user.setProfilePicture(picture);
              user.setRole(defaultRole);
              user.setLoginType(UserAccount.LoginType.GOOGLE);

              this.userAccountRepository.saveAndFlush(user);
          } else {
              user = existingUser.get();
            //   System.out.println(user);
          }

          // ✅ Token generation
          if (user.getRole() == null) {
              throw new RuntimeException("User role cannot be null before token generation");
          }

          String token = jwtTokenUtil.generateToken(
                  user.getEmail(),
                  user.getRole().getName(),
                  "google",
                  user.getId(), null
          );

          Map<String, Object> response = new HashMap<>();
          response.put("token", token);
          response.put("user", user);

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



}
