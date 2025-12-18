package com.example.trip_sheet_backend.dtos;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionResponseDto {

    private UUID id;
    private String name;
    private String moduleName;

    public PermissionResponseDto(UUID id, String name, String moduleName) {
        this.id = id;
        this.name = name;
        this.moduleName = moduleName;
    }
}

