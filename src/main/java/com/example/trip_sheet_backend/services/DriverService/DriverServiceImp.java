package com.example.trip_sheet_backend.services.DriverService;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
// import com.example.trip_sheet_backend.dtos.DriverDto;
import com.example.trip_sheet_backend.models.Driver;
// import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.repositories.DriverRepository;
import com.example.trip_sheet_backend.repositories.RoleRepository;
// import com.example.trip_sheet_backend.response_setups.ApiResponse;

@Service
public class DriverServiceImp extends BaseServiceImp<Driver, UUID> implements DriverService {

  DriverRepository repository;
  RoleRepository roleRepository;

  @Autowired
  // private PasswordEncoder passwordEncoder;

  public DriverServiceImp(DriverRepository repository, RoleRepository roleRepository) {
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
