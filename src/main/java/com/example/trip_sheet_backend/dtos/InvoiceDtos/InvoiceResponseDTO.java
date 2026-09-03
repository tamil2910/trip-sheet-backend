package com.example.trip_sheet_backend.dtos.InvoiceDtos;

import java.util.UUID;
import java.util.List;

import com.example.trip_sheet_backend.dtos.TripDtos.TripRelationResponseDTO;
import com.example.trip_sheet_backend.models.Invoice;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.Trip;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InvoiceResponseDTO {
  private UUID id;
  private String invoiceNumber;
  private UUID purchaseOrderId;
  private String tripCode;
  private List<TripRelationResponseDTO> passengers;
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
        tripCode(invoice),
        passengers(invoice),
        invoice.getTenant() == null ? null : invoice.getTenant().getId(),
        invoice.getStatus(),
        invoice.getApprovedBySide(),
        invoice.getApprovedByUserId(),
        invoice.getApprovedAt(),
        invoice.getIsPrintedInvoice(),
        invoice.getIsDownloadedInvoice()
    );
  }

  private static String tripCode(Invoice invoice) {
    Trip trip = trip(invoice);
    return trip == null ? null : trip.getTripCode();
  }

  private static List<TripRelationResponseDTO> passengers(Invoice invoice) {
    Trip trip = trip(invoice);
    if (trip == null || trip.getPassengers() == null) {
      return List.of();
    }
    return trip.getPassengers().stream().map(InvoiceResponseDTO::passenger).toList();
  }

  private static Trip trip(Invoice invoice) {
    if (invoice.getPurchaseOrder() == null || invoice.getPurchaseOrder().getTripSummary() == null) {
      return null;
    }
    return invoice.getPurchaseOrder().getTripSummary().getTripId();
  }

  private static TripRelationResponseDTO passenger(PeopleTenant person) {
    TripRelationResponseDTO result = new TripRelationResponseDTO();
    if (person.getId() != null) {
      result.setId(person.getId().toString());
    }
    result.setName(person.getName());
    result.setPhone(person.getPhone());
    return result;
  }
}
