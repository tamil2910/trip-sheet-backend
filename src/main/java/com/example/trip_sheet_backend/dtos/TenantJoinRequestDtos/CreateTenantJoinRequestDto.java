package com.example.trip_sheet_backend.dtos.TenantJoinRequestDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreateTenantJoinRequestDto {
    @NotBlank
    private String tenantCode;

    private String message;

    private UUID approverUserAccountId;

    private String approverName;

    private String approverEmail;
}
