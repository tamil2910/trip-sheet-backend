package com.example.trip_sheet_backend.dtos.InvoiceDtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceUpdateRequestDTO {
  private String invoiceNumber;
  private Long invoiceDate;
  private Long dueDate;
  private Long invoicePeriodStart;
  private Long invoicePeriodEnd;
}