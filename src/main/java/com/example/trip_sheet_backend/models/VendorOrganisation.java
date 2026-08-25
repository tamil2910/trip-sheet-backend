package com.example.trip_sheet_backend.models;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Pattern;
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

  private Integer paymentTimelineInDays;

  @Pattern(regexp = "^(gtg|ptd|maxlimitgtg|fixedlimitgtg)$", message = "localBillingStructure must be one of: gtg, ptd, maxlimitgtg, fixedlimitgtg")
  private String localBillingStructure;

  private Integer minGtgKmLimit;

  private Integer minGtgHrLimit;

  private Integer maxGtgKmLimit;

  private Integer maxGtgHrLimit;

  @Nullable
  @Enumerated(EnumType.STRING)
  private ContractStatus contractStatus;
  // ACTIVE, REJECTED, SUSPENDED, TERMINATED, PENDING_APPROVAL

  private Long contractStartDate;

  private Long contractEndDate;

  @ManyToMany
  @JoinTable(
      name = "vendor_organisation_taxes",
      joinColumns = @JoinColumn(name = "vendor_organisation_id"),
      inverseJoinColumns = @JoinColumn(name = "tax_id")
  )
  private List<Tax> taxList = new ArrayList<>();

  @JsonManagedReference
  @OneToMany(mappedBy = "vendorOrganisation")
  private List<VendorOrganisationRateCard> rateCards = new ArrayList<>();

  public enum ContractStatus {
    ACTIVE,
    REJECTED,
    SUSPENDED,
    TERMINATED,
    PENDING_APPROVAL
  }

}
