package com.example.trip_sheet_backend.models;

import java.math.BigDecimal;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "organisation_vendor_allocations", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "organisation_settings_id", "vendor_organisation_id" })
})
@Getter
@Setter
@NoArgsConstructor
public class OrganisationVendorAllocation extends BaseModel {

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "organisation_settings_id", nullable = false)
  private OrganisationSettings organisationSettings;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "vendor_organisation_id", nullable = false)
  private VendorOrganisation vendorOrganisation;

  @Column(name = "allocation_percentage", nullable = false, precision = 5, scale = 2)
  private BigDecimal allocationPercentage;
}
