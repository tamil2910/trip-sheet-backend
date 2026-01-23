package com.example.trip_sheet_backend.models;

import java.util.ArrayList;
import java.util.List;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
@Table(name = "people_tenant")
public class PeopleTenant extends BaseModel implements TenantScoped {

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

  @ManyToMany
  @JoinTable(name = "people_vendor_mapping",
    joinColumns = @JoinColumn(name = "people_id"),
    inverseJoinColumns = @JoinColumn(name = "vendor_id")
  )
  private List<Tenant> attachedVendors = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  private PeopleTenantType tenantType;

  public enum PeopleTenantType {
    WALKIN
  }

  @Enumerated(EnumType.STRING)
  private PeopleType peopleType;

  public enum PeopleType {
    BOOKER, PASSENGER
  }

  @Enumerated(EnumType.STRING)
  private CreatorType creatorType;

  public enum CreatorType {
    VENDOR, ORGANISATION
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "additional_contact_id")
  private PeopleTenant additionalContact;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "emergency_contact_id")
  private PeopleTenant emergencyContact;

  @Override
  public Tenant getTenant() {
   return organisation;
  }

  @Override
  public void setTenant(Tenant tenant) {
  }
  
}
