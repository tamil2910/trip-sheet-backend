package com.example.trip_sheet_backend.dtos.VendorPartnerDtos;

import java.util.List;
import java.util.UUID;
import java.util.Set;

import com.example.trip_sheet_backend.models.VendorPartner.ServiceCategory;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorPartnerUpdateRequestDTO {
  private String tenantName;
  private String contactEmail;
  private String gstNumber;
  private String address;
  private String type;
  private List<@NotNull(message = "Tax id cannot be null") UUID> taxIds;
  private Boolean isActive;
  private Boolean isAssociateSupplier;
  private Boolean isAssociateCustomer;
  private Set<ServiceCategory> serviceCategories;
}
