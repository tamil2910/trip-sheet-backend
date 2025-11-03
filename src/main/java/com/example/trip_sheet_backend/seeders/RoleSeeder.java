package com.example.trip_sheet_backend.seeders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.repositories.RoleRepository;

@Component
public class RoleSeeder implements CommandLineRunner {

  @Autowired
  private RoleRepository roleRepository;

  @Override
  public void run(String... args) throws Exception {
    if (roleRepository.count() == 0) {
            roleRepository.save(new Role("SUPER_ADMIN", "Super Administrator role"));
            roleRepository.save(new Role("ADMIN", "Administrator role"));
            roleRepository.save(new Role("DRIVER", "Driver role"));
            roleRepository.save(new Role("USER", "Standard & Registered user role"));
            roleRepository.save(new Role("GUEST", "temporary passengers created by USER"));
            System.out.println("Default roles inserted successfully!");
        }
  }

}
