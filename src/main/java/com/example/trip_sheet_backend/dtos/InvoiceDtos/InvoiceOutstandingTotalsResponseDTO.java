package com.example.trip_sheet_backend.dtos.InvoiceDtos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Aggregate outstanding values, without vendor, organisation, or invoice detail. */
@Getter
@AllArgsConstructor
public class InvoiceOutstandingTotalsResponseDTO {
    private BigDecimal totalInvoiceAmount;
    private BigDecimal totalReceiptReceived;
    private BigDecimal totalCreditDebitNoteApplied;
    private BigDecimal totalOutstandingAmount;
}
