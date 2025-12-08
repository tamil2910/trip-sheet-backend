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
            roleRepository.save(new Role("SUPER_ADMIN", "Super Administrator role", null));
            roleRepository.save(new Role("ADMIN", "Administrator role", null));
            roleRepository.save(new Role("DRIVER", "Driver role", null));
            roleRepository.save(new Role("USER", "Standard & Registered user role", null));
            roleRepository.save(new Role("TRAVELLER", "temporary passengers created by GUEST", null));
            roleRepository.save(new Role("GUEST", "permanent TRAVELLER with login credentials, signed up by theirself or created USER, ADMIN, SUPER_ADMIN", null));
            System.out.println("Default roles inserted successfully!");
        }
  }

}
