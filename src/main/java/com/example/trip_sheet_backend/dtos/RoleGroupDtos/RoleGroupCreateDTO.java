package com.example.trip_sheet_backend.dtos.RoleGroupDtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull
    private UUID tenantId;  // instead of Tenant entity

    // Permission ids assigned to this role group
    private Set<UUID> permissionIds;
}
