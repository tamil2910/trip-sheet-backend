package com.example.trip_sheet_backend.dtos.RoleGroupDtos;

import java.util.Set;
import java.util.UUID;

import com.example.trip_sheet_backend.dtos.PermissionDtos.PermissionDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RoleGroupResponseDto {
    private UUID id;
    private String name;
    private UUID tenantId;
    private Set<PermissionDTO> permissions;
}
