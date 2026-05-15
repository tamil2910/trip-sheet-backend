package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "vendor_partner_taxes",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "vendor_partner_id", "tax_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorPartnerTax extends BaseModel implements TenantScoped {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vendor_partner_id", nullable = false)
  private VendorPartner vendorPartner;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tax_id", nullable = false)
  private Tax tax;

  @Override
  public Tenant getTenant() {
    return tenant;
  }

  @Override
  public void setTenant(Tenant tenant) {
    this.tenant = tenant;
  }
}
