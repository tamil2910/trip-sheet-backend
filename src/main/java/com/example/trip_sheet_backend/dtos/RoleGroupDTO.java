package com.example.trip_sheet_backend.dtos;

import java.util.Set;
import java.util.UUID;

import com.example.trip_sheet_backend.models.RoleGroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RoleGroupDTO {
  private UUID id;
  private String name;
  private TenantDTO tenant;
  private Set<PermissionDTO> permissions;

  // --- your custom constructor can be placed anywhere ---
  public RoleGroupDTO(RoleGroup rg) {
      this.id = rg.getId();
      this.name = rg.getName();

      // map tenant
      if (rg.getTenant() != null) {
          this.tenant = new TenantDTO(rg.getTenant());
      }

      // map permissions
      if (rg.getPermissions() != null) {
          this.permissions = rg.getPermissions()
                  .stream()
                  .map(PermissionDTO::new)
                  .collect(java.util.stream.Collectors.toSet());
      }
  }
}
