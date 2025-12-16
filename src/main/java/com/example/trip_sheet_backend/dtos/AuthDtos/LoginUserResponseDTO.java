package com.example.trip_sheet_backend.dtos.AuthDtos;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

// import com.example.trip_sheet_backend.models.Tenant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginUserResponseDTO {
    private UUID id;
    private String username;
    // private String email;
    // private String phone;
    private String role;
    private String roleGroup; // only name
    private UUID tenantId;
    private String tenantName;
    // private Tenant.TenantType tenantType;
    private String tenantType;
    private Set<String> permissions;

    // NEW (grouped by module)
    private Map<String, Set<String>> modulePermissions;
}
