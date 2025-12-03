package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.RoleRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.UserAccountService.UserAccountServiceImp;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/accounts")
public class UserAccountController extends BaseController<UserAccount, UUID>{
  private final UserAccountServiceImp service;
  private final RoleRepository roleRepository;
  
  private final ModelMapper mapper;
  
  public UserAccountController(UserAccountServiceImp service, RoleRepository roleRepository, ModelMapper mapper){
    super(service);
    this.service = service;
    this.roleRepository = roleRepository;
    this.mapper =  mapper;
  }
  
  @PreAuthorize("permitAll()")
  @PostMapping("/register")
  public ResponseEntity<ApiResponse<UserAccount>> create(@Valid @RequestBody UserAccount body) {
    
    String name = body.getRole().getName();
    Role role = this.roleRepository.findByName(name).orElseThrow(() -> new RuntimeException("Role is not available in db!"));

    UserAccount payload = new UserAccount();
    payload = mapper.map(body, UserAccount.class);

    payload.setRole(role);
    

    UserAccount result = this.service.createResource(payload);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Resource created successfully", result));
  }
}
