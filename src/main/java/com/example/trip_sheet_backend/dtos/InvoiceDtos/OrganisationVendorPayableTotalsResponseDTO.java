package com.example.trip_sheet_backend.dtos.InvoiceDtos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Aggregate payable values for an organisation's vendor invoices. */
@Getter
@AllArgsConstructor
public class OrganisationVendorPayableTotalsResponseDTO {
    private BigDecimal totalInvoiceAmount;
    private BigDecimal totalReceiptReceived;
    private BigDecimal totalCreditDebitNoteApplied;
    private BigDecimal totalPayableAmount;
}
