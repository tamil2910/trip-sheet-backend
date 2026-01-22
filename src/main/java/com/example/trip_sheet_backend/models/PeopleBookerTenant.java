package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "people_booker_tenant")
public class PeopleBookerTenant extends BaseModel implements TenantScoped {

  private String name;
  private String email;
  private String phone;
  private String designation;
  

  @Enumerated(EnumType.STRING)
  private GenderType gender;

  public enum GenderType {
    MALE, FEMALE, TransgenderMale, TransgenderFemale, Others, None
  }

  @ManyToOne
  @JoinColumn(name = "organisation_id", nullable = false)
  private Tenant organisation;

  @ManyToOne
  @JoinColumn(name = "vendor_id", nullable = true)
  private Tenant vendor;

  @Override
  public Tenant getTenant() {
    return organisation;
  }

  @Override
  public void setTenant(Tenant tenant) {
    if (tenant == null) return;

    if (tenant.getTenantType() == Tenant.TenantType.ORGANISATION) {
      this.organisation = tenant;
    }
  }

  
}
