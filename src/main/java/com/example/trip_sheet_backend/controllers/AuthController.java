package com.example.trip_sheet_backend.controllers;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.LoginRequestDto;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.RoleRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.security.GoogleAuthService;
import com.example.trip_sheet_backend.security.JwtTokenUtil;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Value;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final UserAccountRepository userAccountRepository;
  private final JwtTokenUtil jwtTokenUtil;
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private final RoleRepository roleRepository;
  private final GoogleAuthService googleAuthService;


  @Value("${GOOGLE_AUTH_CLIENT_ID}")
  private String googleClientId; // 👈 inject env variable here
  
  public AuthController(UserAccountRepository userAccountRepository, JwtTokenUtil jwtTokenUtil, RoleRepository roleRepository, GoogleAuthService googleAuthService) {
    this.userAccountRepository = userAccountRepository;
    this.jwtTokenUtil = jwtTokenUtil;
    this.roleRepository = roleRepository;
    this.googleAuthService = googleAuthService;
  }

  @PostMapping("/login")
  public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequestDto payload) {
    String identifier = payload.getIdentifier();
    String password = payload.getPassword();

    Optional<UserAccount> user = Optional.empty();

    if (identifier.contains("@")) {
        user = this.userAccountRepository.findByEmail(identifier);
    } else if (identifier.matches("\\d+")) {
        Long phoneNumber = Long.parseLong(identifier);
        user = this.userAccountRepository.findByPhone(phoneNumber);
    } else {
        user = this.userAccountRepository.findByUsername(identifier);
    }

    if (user.isEmpty() || !passwordEncoder.matches(password, user.get().getPassword())) {
        return new ApiResponse<>(false, "Invalid credentials", null);
    }

    String role = user.get().getRole().getName();
    String token = jwtTokenUtil.generateToken(identifier, role, "user_account");

    Map<String, Object> response = new HashMap<>();
    response.put("token", token);
    response.put("user", user);

    return new ApiResponse<>(true, "Login successful", response);
  }

  @PostMapping("/google-signup")
  public ApiResponse<Map<String, Object>> googleSignup(@RequestBody Map<String, Object> payload) {
    
    
    try {
      String idToken = (String) payload.get("idToken");
      // ✅ Verify token with Google
      Payload googlePayload = this.googleAuthService.verifyToken(idToken);
      // if (idTokenString == null) {
      //   return new ApiResponse<>(false, "Missing Google ID token", null);
      // }

      // GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
      //   new NetHttpTransport(),
      //   JacksonFactory.getDefaultInstance())
      //   .setAudience(Collections.singletonList(googleClientId))
      //   .build();

      // GoogleIdToken idToken = verifier.verify(idTokenString);
      
      if (googlePayload == null) {
          return new ApiResponse<>(false, "Invalid Google ID token", null);
      }

      String email = googlePayload.getEmail();
      String name = (String) googlePayload.get("name");
      String picture = (String) googlePayload.get("picture");
      String googleId = googlePayload.getSubject();

      // ✅ Now safely handle signup/login
      Optional<UserAccount> existingUser = userAccountRepository.findByEmail(email);

      UserAccount user;
      if (existingUser.isEmpty()) {
          // Fetch role properly
          Role defaultRole = roleRepository.findByName("USER")
                  .orElseThrow(() -> new RuntimeException("Default role USER not found"));

          user = new UserAccount();
          String baseName = name != null ? name : email.split("@")[0];
          user.setUsername(generateUniqueUsername(baseName));
          user.setEmail(email);
          user.setGoogleId(googleId);
          user.setProfilePicture(picture);
          user.setRole(defaultRole);

          user.setLoginType(UserAccount.LoginType.GOOGLE);

          this.userAccountRepository.save(user);
      } else {
          user = existingUser.get();
      }

      String token = jwtTokenUtil.generateToken(user.getEmail(), user.getRole().getName(), "google");

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


  // @PostMapping("/google-signup")
  // public ApiResponse<Map<String, Object>> googleSignup(@RequestBody Map<String, Object> payload) {
  //     String email = (String) payload.get("email");
  //     String name = (String) payload.get("name");
  //     String googleId = (String) payload.get("googleId");
  //     String picture = (String) payload.get("picture");

  //     if (email == null || googleId == null) {
  //         return new ApiResponse<>(false, "Missing Google user data", null);
  //     }

  //     Optional<UserAccount> existingUser = userAccountRepository.findByEmail(email);

  //     UserAccount user;
  //     if (existingUser.isEmpty()) {
  //         // 1️⃣ Assign default role (USER or TRAVELLER)
  //         Role defaultRole = new Role();
  //         defaultRole.setName("USER");

  //         // 2️⃣ Create new account
  //         user = new UserAccount();
  //         user.setEmail(email);
  //         user.setUsername(name != null ? name.replaceAll(" ", "_") : email.split("@")[0]);
  //         user.setGoogleId(googleId);
  //         user.setProfilePicture(picture);
  //         user.setRole(defaultRole);
  //         user.setLoginType("GOOGLE");

  //         userAccountRepository.save(user);
  //     } else {
  //         user = existingUser.get();
  //     }

  //     // 3️⃣ Generate token
  //     String token = jwtTokenUtil.generateToken(user.getEmail(), user.getRole().getName(), "google");

  //     Map<String, Object> response = new HashMap<>();
  //     response.put("token", token);
  //     response.put("user", user);

  //     return new ApiResponse<>(true, "Google login/signup successful", response);
  // }


}
