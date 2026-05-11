package com.example.trip_sheet_backend.dtos.UserAccountDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Role;
import com.example.trip_sheet_backend.models.UserAccount;

import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserAccountByFormDto {

    private String username;

    private String fullName;

    @Email
    private String email;
    
    @Nullable
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be 10 digits and start with 6-9")
    private String phone;

    // @Pattern(
    //   regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$",
    //   message = "Password must be at least 8 characters and include letters, numbers, and a special character"
    // ) // mandatory special characters
    @Nullable
    @Pattern(
      regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
      message = "Password must be at least 8 characters and include letters and numbers"
    ) // special characters allowed but not mandatory
    private String password; // hashed

    private UserAccount.LoginType loginType; // "EMAIL", "PHONE", "USERNAME", "OAUTH", "GOOGLE"

    @Nullable
    private String profilePicture;


    private Role role;

    private UUID roleGroupId;

    private Boolean isActive = true;

    private UserAccount.TenantType tenantType;
}
