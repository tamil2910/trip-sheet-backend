package com.example.trip_sheet_backend.dtos.RoleGroupDtos;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RoleGroupUpdateDto {
    @NotBlank(message = "Name is required")
    private String name;

    private Set<UUID> permissionIds; // IDs only, not entities

    private String updatedBy;
}
