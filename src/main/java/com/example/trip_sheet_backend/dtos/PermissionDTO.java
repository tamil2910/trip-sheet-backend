package com.example.trip_sheet_backend.dtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Permission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PermissionDTO {
    private UUID id;
    private String name;
    private String description;
    private String moduleName;

    public PermissionDTO(Permission permission) {
        this.id = permission.getId();
        this.name = permission.getName();
        this.description = permission.getDescription();
        this.moduleName = permission.getModuleName();
    }
}
