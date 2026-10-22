package com.example.trip_sheet_backend.models.ratecardpackage;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.example.trip_sheet_backend.models.VendorOrganisationRateCard;
import com.example.trip_sheet_backend.models.VendorPartnerRateCard;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rate_card_package_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"rate_card_package_id", "vendor_partner_rate_card_id"}),
    @UniqueConstraint(columnNames = {"rate_card_package_id", "vendor_organisation_rate_card_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class RateCardPackageItem extends BaseModel {
  @ManyToOne(optional = false)
  @JoinColumn(name = "rate_card_package_id", nullable = false)
  private RateCardPackage rateCardPackage;

  @ManyToOne
  @JoinColumn(name = "vendor_partner_rate_card_id")
  private VendorPartnerRateCard vendorPartnerRateCard;

  @ManyToOne
  @JoinColumn(name = "vendor_organisation_rate_card_id")
  private VendorOrganisationRateCard vendorOrganisationRateCard;
}
