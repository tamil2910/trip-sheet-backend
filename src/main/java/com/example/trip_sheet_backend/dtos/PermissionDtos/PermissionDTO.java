package com.example.trip_sheet_backend.dtos.PermissionDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Permission;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PermissionDTO {
    private UUID id;
    private String name;
    // private String description;
    // private String moduleName;

    public PermissionDTO(Permission permission) {
        this.id = permission.getId();
        this.name = permission.getName();
    }
}
