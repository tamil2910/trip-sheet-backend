package com.example.trip_sheet_backend.models;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "people_tenant_custom_field_values")
public class PeopleTenantCustomFieldValue extends BaseModel {
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "people_tenant_id", nullable = false)
  private PeopleTenant peopleTenant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "custom_field_id", nullable = false)
  private CustomField customField;

  /** True when the organisation allows vendors to view this organisation-owned value. */
  @Column(name = "is_visible_to_vendor", nullable = false)
  private Boolean isVisibleToVendor = false;

  /** Null for organisation-authored values; set for a vendor's separate value. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_vendor_id")
  @JsonIgnore
  private Tenant createdByVendor;

  private String value;
}
