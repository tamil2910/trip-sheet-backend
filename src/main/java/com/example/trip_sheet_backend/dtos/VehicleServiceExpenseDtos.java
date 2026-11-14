package com.example.trip_sheet_backend.dtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public final class VehicleServiceExpenseDtos {
  private VehicleServiceExpenseDtos() {}

  public record ItemCategoryResponse(UUID id, String name) {}
  public record ItemResponse(UUID id, UUID categoryId, String itemName, String itemType,
      Integer quantity, BigDecimal value, BigDecimal lineTotal) {}
  public record InvoiceResponse(UUID id, String fileName, String contentType,
      Long fileSize, String downloadUrl) {}
  public record InvoiceUploadRequest(@NotEmpty List<@Valid VehicleServiceInvoiceRequest> invoices) {}
}
