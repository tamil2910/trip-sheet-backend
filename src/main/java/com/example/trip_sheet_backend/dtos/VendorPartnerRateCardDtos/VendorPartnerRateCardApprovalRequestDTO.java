package com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos;

import com.example.trip_sheet_backend.models.VendorPartnerRateCard;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorPartnerRateCardApprovalRequestDTO {

  @NotNull
  private VendorPartnerRateCard.ApprovalStatus approvalStatus;
}
