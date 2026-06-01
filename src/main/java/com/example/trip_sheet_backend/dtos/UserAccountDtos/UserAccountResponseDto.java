package com.example.trip_sheet_backend.dtos.UserAccountDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.UserAccount;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountResponseDto {
    private String username;

    private String fullName;

    private String email;
    
    private String phone;
   

    private UserAccount.LoginType loginType; // "EMAIL", "PHONE", "USERNAME", "OAUTH", "GOOGLE"

    private String profilePicture;

    private Role role;

    private UUID roleGroupId;

    private Boolean isActive = true;

    private UserAccount.TenantType tenantType;
}
