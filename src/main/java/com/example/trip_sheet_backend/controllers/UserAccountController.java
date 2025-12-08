package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.dtos.UserAccountByFormDto;
import com.example.trip_sheet_backend.models.Admin;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.AdminRepository;
import com.example.trip_sheet_backend.repositories.RoleRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.security.JwtTokenUtil;
import com.example.trip_sheet_backend.services.UserAccountService.UserAccountServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/accounts")
public class UserAccountController extends BaseController<UserAccount, UUID>{
  private final UserAccountServiceImp service;
  private final RoleRepository roleRepository;
  private final UserAccountRepository userAccountRepository;
  private final AdminRepository adminRepository;
  private final ModelMapper mapper;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private final JwtTokenUtil jwtTokenUtil;
  
  public UserAccountController(UserAccountServiceImp service, RoleRepository roleRepository, 
    UserAccountRepository userAccountRepository, ModelMapper mapper, AdminRepository 
    adminRepository, JwtTokenUtil jwtTokenUtil){
    super(service);
    this.service = service;
    this.roleRepository = roleRepository;
    this.mapper =  mapper;
    this.userAccountRepository = userAccountRepository;
    this.adminRepository = adminRepository;
    this.jwtTokenUtil = jwtTokenUtil;
  }
  
  @PreAuthorize("permitAll()")
  @PostMapping("/register")
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

    UserAccount result = this.service.createResource(payload);

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

  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  @PostMapping("/add")
  public ResponseEntity<ApiResponse<?>> createUser(
    HttpServletRequest request, 
    @Valid @RequestBody UserAccountByFormDto body) {

    if (body.getEmail() != null && userAccountRepository.existsByEmail(body.getEmail())) {
      throw new RuntimeException("Email already exists");
    }

    if (body.getPhone() != null && userAccountRepository.existsByPhone(body.getPhone())) {
        throw new RuntimeException("Phone already exists");
    }


    UserAccount payload = mapper.map(body, UserAccount.class);

    if ("ADMIN".equals(body.getRole().getName())) {
      throw new RuntimeException("Admin can not be added in this route!");
    }

    Role role = this.roleRepository.findByName(body.getRole().getName()).orElseThrow(
      () -> new RuntimeException("Role is not available in db!"));

    String token = request.getHeader("Authorization").replace("Bearer ", "");

    UUID userId = UUID.fromString(jwtTokenUtil.getUserIdFromToken(token));

    UserAccount userAccount = userAccountRepository
            .findById(userId)
            .orElseThrow(() -> new RuntimeException("Admin resource not found!"));

    if (userAccount.getTenant() == null) {
      throw new RuntimeException("Tenant resource not found to add user!");
    }

    // Encrypt password BEFORE save
    if (body.getPassword() != null) {
        payload.setPassword(passwordEncoder.encode(body.getPassword()));
    }

    // Assign Role entity
    payload.setRole(role);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String createdBy = (String) auth.getDetails();

    payload.setCreatedBy(createdBy);

    payload.setCreatedByUser(userAccount);    

    UserAccount result = this.service.createResource(payload);
    

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Resource created successfully", result));
  }

}
