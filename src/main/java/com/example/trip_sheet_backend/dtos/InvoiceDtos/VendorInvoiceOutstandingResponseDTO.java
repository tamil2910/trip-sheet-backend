package com.example.trip_sheet_backend.dtos.InvoiceDtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VendorInvoiceOutstandingResponseDTO {
    private UUID vendorId;
    private BigDecimal totalInvoiceAmount;
    private BigDecimal totalReceiptReceived;
    private BigDecimal totalCreditDebitNoteApplied;
    private BigDecimal totalOutstandingAmount;
    private List<OrganisationOutstanding> organisations;

    @Getter
    @AllArgsConstructor
    public static class OrganisationOutstanding {
        private UUID organisationId;
        private String organisationName;
        private BigDecimal totalInvoiceAmount;
        private BigDecimal receiptReceived;
        private BigDecimal creditDebitNoteApplied;
        private BigDecimal outstandingAmount;
        private List<InvoiceOutstanding> invoices;
    }

    @Getter
    @AllArgsConstructor
    public static class InvoiceOutstanding {
        private UUID invoiceId;
        private String invoiceNumber;
        private Long invoiceDate;
        private BigDecimal invoiceAmount;
        private BigDecimal receiptReceived;
        private BigDecimal creditDebitNoteApplied;
        private BigDecimal outstandingAmount;
        private Boolean isPaid;
    }
}
