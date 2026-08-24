package com.example.trip_sheet_backend.dtos.VendorOrganisationDtos;

import com.example.trip_sheet_backend.models.VendorOrganisation;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorOrganisationUpdateRequestDTO {

  private Boolean active;
  private Long onboardedAt;
  private Integer paymentTimelineInDays;

  @Pattern(regexp = "^(gtg|ptd|maxlimitgtg|fixedlimitgtg)$", message = "localBillingStructure must be one of: gtg, ptd, maxlimitgtg, fixedlimitgtg")
  private String localBillingStructure;

  private Integer minGtgKmLimit;
  private Integer minGtgHrLimit;
  private Integer maxGtgKmLimit;
  private Integer maxGtgHrLimit;
  private VendorOrganisation.ContractStatus contractStatus;
  private Long contractStartDate;
  private Long contractEndDate;
}
