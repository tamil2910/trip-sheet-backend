package com.example.trip_sheet_backend.dtos.TenantJoinRequestDtos;

import com.example.trip_sheet_backend.models.PeopleTenant.OrganisationPersonType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DecideTenantJoinRequestDto {
    private OrganisationPersonType personType;
    private String decisionNote;
}
