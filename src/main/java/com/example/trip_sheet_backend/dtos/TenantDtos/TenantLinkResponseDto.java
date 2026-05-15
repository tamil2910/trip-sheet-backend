package com.example.trip_sheet_backend.dtos.TenantDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Tenant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TenantLinkResponseDto {
  private String linkType;
  private UUID relationId;
  private Tenant tenant;
  private boolean alreadyLinked;
}
