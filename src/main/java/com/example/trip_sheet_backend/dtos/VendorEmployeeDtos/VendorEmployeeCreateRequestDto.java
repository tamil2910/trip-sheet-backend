package com.example.trip_sheet_backend.dtos.VendorEmployeeDtos;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VendorEmployeeCreateRequestDto {
    private String name;
    private String email;
    private String phone;
    private String employeeNumber;

    @NotNull
    private Boolean isUser;

    private Set<UUID> roleGroupIds;
}
