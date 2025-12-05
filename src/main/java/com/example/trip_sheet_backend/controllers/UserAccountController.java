package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.dtos.UserAccountByFormDto;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.RoleRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.UserAccountService.UserAccountServiceImp;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/accounts")
public class UserAccountController extends BaseController<UserAccount, UUID>{
  private final UserAccountServiceImp service;
  private final RoleRepository roleRepository;
  private final UserAccountRepository userAccountRepository;
  
  private final ModelMapper mapper;

  @Autowired
  private PasswordEncoder passwordEncoder;
  
  public UserAccountController(UserAccountServiceImp service, RoleRepository roleRepository, 
    UserAccountRepository userAccountRepository, ModelMapper mapper){
    super(service);
    this.service = service;
    this.roleRepository = roleRepository;
    this.mapper =  mapper;
    this.userAccountRepository = userAccountRepository;
  }
  
  @PreAuthorize("permitAll()")
  @PostMapping("/register")
  public ResponseEntity<ApiResponse<UserAccount>> create(@Valid @RequestBody UserAccountByFormDto body) {

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
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Resource created successfully", result));
  }
}
