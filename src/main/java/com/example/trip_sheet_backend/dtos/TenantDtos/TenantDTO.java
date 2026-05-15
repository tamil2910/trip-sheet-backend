package com.example.trip_sheet_backend.dtos.TenantDtos;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.Tenant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TenantDTO {
  private UUID id;
  private String tenantName;
  private String contactEmail;
  private String tenantType;
  private String gstNumber;
  private String address;
  private boolean isActive;
  private boolean isVerfiedGst;
  private List<UUID> taxIds;

  public TenantDTO(Tenant tenant) {
    this(tenant, List.of());
  }

  public TenantDTO(Tenant tenant, List<UUID> taxIds) {
    this.id = tenant.getId();
    this.tenantName = tenant.getTenantName();
    this.contactEmail = tenant.getContactEmail();
     // FIX: prevent null enum crash
    this.tenantType = tenant.getTenantType() != null 
            ? tenant.getTenantType().toString()
            : null;
    this.gstNumber = tenant.getGstNumber();
    this.address = tenant.getAddress();
    this.isActive = tenant.getIsActive();
    this.isVerfiedGst = tenant.getVerifiedGst();
    this.taxIds = taxIds == null ? List.of() : taxIds;
  }
}
