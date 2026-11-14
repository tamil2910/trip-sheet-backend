package com.example.trip_sheet_backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleServiceInvoiceRequest {
  @NotBlank
  @Size(max = 255)
  private String fileName;

  private String contentType;

  @NotBlank
  private String base64Data;
}
