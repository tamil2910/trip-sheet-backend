package com.example.trip_sheet_backend.dtos.VendorEmployeeDtos;

import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VendorEmployeeUpdateRequestDto {
    private String name;
    private String email;
    private String phone;
    private String employeeNumber;
    private String password;
    private Boolean isUser;
    private Set<UUID> roleGroupIds;
}
