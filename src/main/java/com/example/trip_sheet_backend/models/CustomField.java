package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "custom_fields")
public class CustomField extends BaseModel {

  @NotBlank(message = "Custom field name is required")
  private String name;

  /**
   * The tenant this field belongs to: the organisation or associate-customer
   * whose people/trips can use the field.
   */
  @NotNull(message = "Tenant is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  /**
   * Vendor that created this field. Null when the target tenant created it
   * themselves. Vendor-created fields are only visible to this vendor.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_vendor_id")
  @JsonIgnore
  private Tenant createdByVendor;

  /**
   * Directional vendor-partner link that scopes an associate-customer field.
   * The field creator must be the link's primary vendor and the tenant must be
   * its partner vendor, marked as an associate customer. Null for organisation
   * fields and vendor-created fields targeting an organisation.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_vendor_partner_id")
  @JsonIgnore
  private VendorPartner ownerVendorPartner;

  private Boolean isActive = true;

  public Tenant getTenant() {
    return tenant;
  }
}
