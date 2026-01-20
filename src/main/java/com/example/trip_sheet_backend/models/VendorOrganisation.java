package com.example.trip_sheet_backend.models;

import org.springframework.lang.Nullable;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "vendor_organisations",
       uniqueConstraints = @UniqueConstraint(
         columnNames = {"vendor_id", "organisation_id"}
       ))
public class VendorOrganisation extends BaseModel {

  @ManyToOne
  @JoinColumn(name = "vendor_id", nullable = false)
  private Tenant vendor; // tenantType = VENDOR

  @ManyToOne
  @JoinColumn(name = "organisation_id", nullable = false)
  private Tenant organisation; // tenantType = ORGANISATION

  private Boolean active;

  private Long onboardedAt;

  @Nullable
  @Enumerated(EnumType.STRING)
  private ContractStatus contractStatus;
  // ACTIVE, REJECTED, SUSPENDED, TERMINATED, PENDING_APPROVAL

  public enum ContractStatus {
    ACTIVE,
    REJECTED,
    SUSPENDED,
    TERMINATED,
    PENDING_APPROVAL
  }

}
