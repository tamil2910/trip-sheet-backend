package com.example.trip_sheet_backend.dtos.RoleGroupDtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
// import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RoleGroupCreateDTO {

    @NotBlank
    private String name;

    // @NotNull
    private UUID tenantId;  // instead of Tenant entity

    // Format 1: Permission ids assigned to this role group (UUIDs)
    private Set<UUID> permissionIds;
    
    // Format 2: Permission names assigned to this role group (strings)
    private Set<String> permissions;
}
