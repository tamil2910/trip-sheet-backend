package com.example.trip_sheet_backend.models;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
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
@Table(name = "vendor_partners",
       uniqueConstraints = @UniqueConstraint(
         columnNames = {"primary_vendor_id", "partner_vendor_id"}
       ))
public class VendorPartner extends BaseModel {

  @ManyToOne
  @JoinColumn(name = "primary_vendor_id", nullable = false)
  private Tenant primaryVendor; // tenantType = VENDOR

  @ManyToOne
  @JoinColumn(name = "partner_vendor_id", nullable = false)
  private Tenant partnerVendor; // tenantType = VENDOR

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

  private Long onboardedAt;

  private Integer paymentTimelineInDays;

  @Pattern(regexp = "^(gtg|ptd|maxlimitgtg|fixedlimitgtg)$", message = "localBillingStructure must be one of: gtg, ptd, maxlimitgtg, fixedlimitgtg")
  private String localBillingStructure;

  private Integer minGtgKmLimit;

  private Integer minGtgHrLimit;

  private Integer maxGtgKmLimit;

  private Integer maxGtgHrLimit;

  private Long contractStartDate;

  private Long contractEndDate;

  @Column(name = "vendor_partner_rate_card_id")
  private UUID vendorPartnerRateCardId;

  @JsonManagedReference
  @OneToMany(mappedBy = "vendorPartner")
  private List<VendorPartnerRateCard> rateCards = new ArrayList<>();

}
