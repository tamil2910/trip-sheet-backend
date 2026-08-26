package com.example.trip_sheet_backend.dtos.InvoiceDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Invoice;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InvoiceResponseDTO {
  private UUID id;
  private String invoiceNumber;
  private UUID purchaseOrderId;
  private UUID tenantId;
  private Invoice.InvoiceStatus status;
  private Invoice.ApprovalSide approvedBySide;
  private String approvedByUserId;
  private Long approvedAt;
  private Boolean isPrintedInvoice;
  private Boolean isDownloadedInvoice;

  public static InvoiceResponseDTO fromEntity(Invoice invoice) {
    return new InvoiceResponseDTO(
        invoice.getId(),
        invoice.getInvoiceNumber(),
        invoice.getPurchaseOrder() == null ? null : invoice.getPurchaseOrder().getId(),
        invoice.getTenant() == null ? null : invoice.getTenant().getId(),
        invoice.getStatus(),
        invoice.getApprovedBySide(),
        invoice.getApprovedByUserId(),
        invoice.getApprovedAt(),
        invoice.getIsPrintedInvoice(),
        invoice.getIsDownloadedInvoice()
    );
  }
}
