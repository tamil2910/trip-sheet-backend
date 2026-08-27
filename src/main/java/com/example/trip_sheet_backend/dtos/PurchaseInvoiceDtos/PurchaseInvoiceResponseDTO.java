package com.example.trip_sheet_backend.dtos.PurchaseInvoiceDtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.trip_sheet_backend.models.PurchaseInvoice;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PurchaseInvoiceResponseDTO {
  private UUID id;
  private String invoiceNumber;
  private UUID tripSummaryId;
  private UUID payerVendorId;
  private UUID payeeVendorId;
  private BigDecimal amountPayable;
  private BigDecimal amountReceivable;
  private BigDecimal earning;
  private String currencyCode;
  private String rateCardPackageName;
  private String notes;

  public static PurchaseInvoiceResponseDTO fromEntity(PurchaseInvoice value) {
    return new PurchaseInvoiceResponseDTO(value.getId(), value.getInvoiceNumber(),
        value.getTripSummary() == null ? null : value.getTripSummary().getId(),
        value.getPayerVendor() == null ? null : value.getPayerVendor().getId(),
        value.getPayeeVendor() == null ? null : value.getPayeeVendor().getId(),
        value.getAmountPayable(), value.getAmountReceivable(), value.getEarning(),
        value.getCurrencyCode(), value.getRateCardPackageName(), value.getNotes());
  }
}
