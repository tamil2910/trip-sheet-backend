package com.example.trip_sheet_backend.dtos.VendorEmployeeDtos;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.trip_sheet_backend.models.VendorEmployee;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorEmployeeResponseDto {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String employeeNumber;
    private Boolean isUser;
    private UUID userAccountId;
    private Set<UUID> roleGroupIds;

    public VendorEmployeeResponseDto(VendorEmployee employee) {
        this.id = employee.getId();
        this.name = employee.getName();
        this.email = employee.getEmail();
        this.phone = employee.getPhone();
        this.employeeNumber = employee.getEmployeeNumber();
        this.isUser = employee.getIsUser();
        this.userAccountId = employee.getAccount() == null ? null : employee.getAccount().getId();
        this.roleGroupIds = employee.getAccount() == null || employee.getAccount().getRoleGroups() == null
                ? Set.of()
                : employee.getAccount().getRoleGroups().stream().map(group -> group.getId()).collect(Collectors.toSet());
    }
}
