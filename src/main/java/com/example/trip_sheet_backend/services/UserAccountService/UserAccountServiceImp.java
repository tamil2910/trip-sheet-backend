package com.example.trip_sheet_backend.services.UserAccountService;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.RoleRepository;
// import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;

@Service
public class UserAccountServiceImp extends BaseServiceImp<UserAccount, UUID> implements UserAccountService {
  UserAccountRepository repository;
  RoleRepository roleRepository;
  @Autowired
  // private PasswordEncoder passwordEncoder;

  public UserAccountServiceImp(UserAccountRepository repository, RoleRepository roleRepository) {
    super(repository);
    this.repository = repository;
    this.roleRepository = roleRepository;
  }

  // public Driver createDriver(DriverDto driverDto) {
  //   if(
  //     (driverDto.getPassword() == null || driverDto.getPassword().isBlank()) 
  //     && (driverDto.getGoogleId() == null || driverDto.getGoogleId().isBlank())
  //   ) {
  //     throw new RuntimeException("Either password or Google ID is required.");
  //   }        Driver driver = new Driver();
  //       driver.setName(driverDto.getName());
  //       driver.setEmail(driverDto.getEmail());
  //       driver.setGoogleId(driverDto.getGoogleId());

  //       // Encode password only if present
  //       if (driverDto.getPassword() != null && !driverDto.getPassword().isBlank()) {
  //           driver.setPassword(passwordEncoder.encode(driverDto.getPassword()));
  //       }

  //       // Set role
  //       if (driverDto.getRoleId() != null) {
  //           Role role = this.roleRepository.findById(driverDto.getRoleId())
  //               .orElseThrow(() -> new RuntimeException("Role not found"));
  //           driver.setRole(role);
  //       }
  //     return repository.saveAndFlush(driver);
  // }

}
