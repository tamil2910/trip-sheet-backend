package com.example.trip_sheet_backend.dtos.TenantDtos;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantCreateWithTaxIdsRequestDto {

  @NotBlank(message = "Tenant name is required")
  @Size(min = 2, message = "Tenant name must contain at least 2 characters")
  private String tenantName;

  @Email(message = "Invalid email format")
  @NotBlank(message = "Contact email is required")
  @Size(max = 255, message = "Contact email must not exceed 255 characters")
  private String contactEmail;

  @Size(max = 50, message = "GST number must not exceed 50 characters")
  private String gstNumber;

  private String address;

  private List<@NotNull(message = "Tax id cannot be null") UUID> taxIds;

}
