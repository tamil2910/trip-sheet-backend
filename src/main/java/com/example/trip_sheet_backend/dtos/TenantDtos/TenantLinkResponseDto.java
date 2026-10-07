package com.example.trip_sheet_backend.dtos.TenantDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Tenant;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantLinkResponseDto {
  private String linkType;
  private UUID relationId;
  private Tenant tenant;
  private boolean alreadyLinked;
  private Boolean isActive;
  private Boolean isAssociateSupplier;
  private Boolean isAssociateCustomer;

  public TenantLinkResponseDto(String linkType, UUID relationId, Tenant tenant, boolean alreadyLinked) {
    this(linkType, relationId, tenant, alreadyLinked, null, null, null);
  }
}
