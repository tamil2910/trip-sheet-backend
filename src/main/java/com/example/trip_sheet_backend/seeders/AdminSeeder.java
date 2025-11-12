package com.example.trip_sheet_backend.seeders;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.example.trip_sheet_backend.models.Admin;
import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.repositories.AdminRepository;
import com.example.trip_sheet_backend.repositories.RoleRepository;

@Component
public class AdminSeeder implements CommandLineRunner {

  private final AdminRepository adminRepository;
  private final RoleRepository roleRepository;

  public AdminSeeder(AdminRepository adminRepository, RoleRepository roleRepository) {
      this.adminRepository = adminRepository;
      this.roleRepository = roleRepository;
  }

  @Override
  public void run(String... args) throws Exception {

    if (adminRepository.count() == 0) {
      Role adminRole = roleRepository.findByName("ADMIN")
              .orElseGet(() -> {
                  Role newRole = new Role();
                  newRole.setName("ADMIN");
                  newRole.setDescription("Administrator role with full permissions");
                  return roleRepository.save(newRole);
              });

      List<Admin> admins = List.of(
          new Admin("Tamilarasan B", "tamilarasan2910@gmail.com", "TamilArchu@2921", adminRole, null),
          new Admin("Archana", "archanaraju@gmail.com", "Archana@122", adminRole, null),
          new Admin("Raghav", "hello@tripsheet.com", "Tripsheet@2025", adminRole, null)
      );

      for (Admin admin : admins) {
          if (adminRepository.findByEmail(admin.getEmail()).isEmpty()) {
              adminRepository.save(admin);
              System.out.println("✅ Admin created: " + admin.getEmail());
          } else {
              System.out.println("ℹ️ Admin already exists: " + admin.getEmail());
          }
      }
    }
  }
}
