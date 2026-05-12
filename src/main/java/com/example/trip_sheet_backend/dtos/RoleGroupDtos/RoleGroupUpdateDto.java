package com.example.trip_sheet_backend.dtos.RoleGroupDtos;

import java.util.Set;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RoleGroupUpdateDto {
    private String name;

    private Set<UUID> permissionIds; // IDs only, not entities

    private Set<String> permissions; // Permission names

    private String updatedBy;
}
