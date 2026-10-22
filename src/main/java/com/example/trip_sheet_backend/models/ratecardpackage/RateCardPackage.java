package com.example.trip_sheet_backend.models.ratecardpackage;

import java.util.ArrayList;
import java.util.List;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.models.VendorPartner;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rate_card_packages")
@Getter
@Setter
@NoArgsConstructor
public class RateCardPackage extends BaseModel {
  @Column(nullable = false)
  private String name;

  private Long validFrom;
  private Long validTo;

  @Column(nullable = false)
  private Boolean isDefault = false;

  @ManyToOne
  @JoinColumn(name = "vendor_partner_id")
  private VendorPartner vendorPartner;

  @ManyToOne
  @JoinColumn(name = "vendor_organisation_id")
  private VendorOrganisation vendorOrganisation;

  @OneToMany(mappedBy = "rateCardPackage", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RateCardPackageItem> items = new ArrayList<>();
}
