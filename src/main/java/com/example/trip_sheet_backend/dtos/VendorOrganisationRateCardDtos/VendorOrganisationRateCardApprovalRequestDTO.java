package com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos;

import com.example.trip_sheet_backend.models.VendorOrganisationRateCard;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorOrganisationRateCardApprovalRequestDTO {

  @NotNull
  private VendorOrganisationRateCard.ApprovalStatus approvalStatus;
}
