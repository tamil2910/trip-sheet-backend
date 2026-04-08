package com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos;
import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.VendorPartner;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorPartnerRateCardBulkCreateRequestDTO {

  @NotNull
  private UUID vendorPartnerId;

  private Integer paymentTimelineInDays;

  @Pattern(regexp = "^(gtg|ptd|maxlimitgtg|fixedlimitgtg)$", message = "localBillingStructure must be one of: gtg, ptd, maxlimitgtg, fixedlimitgtg")
  private String localBillingStructure;

  private Integer minGtgKmLimit;

  private Integer minGtgHrLimit;

  private Integer maxGtgKmLimit;

  private Integer maxGtgHrLimit;

  private VendorPartner.ContractStatus contractStatus;

  private Long contractStartDate;

  private Long contractEndDate;

  @Valid
  @NotEmpty
  private List<VendorPartnerRateCardCreateRequestDTO> rateCards;
}
