package com.example.trip_sheet_backend.dtos.InvoiceDtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Payable summary for an organisation, grouped by the invoicing vendor. */
@Getter
@AllArgsConstructor
public class OrganisationVendorPayableResponseDTO {
    private UUID organisationId;
    private BigDecimal totalInvoiceAmount;
    private BigDecimal totalReceiptReceived;
    private BigDecimal totalCreditDebitNoteApplied;
    private BigDecimal totalPayableAmount;
    private List<VendorPayable> vendors;

    @Getter
    @AllArgsConstructor
    public static class VendorPayable {
        private UUID vendorId;
        private String vendorName;
        private BigDecimal totalInvoiceAmount;
        private BigDecimal receiptReceived;
        private BigDecimal creditDebitNoteApplied;
        private BigDecimal payableAmount;
        private List<InvoicePayable> invoices;
    }

    @Getter
    @AllArgsConstructor
    public static class InvoicePayable {
        private UUID invoiceId;
        private String invoiceNumber;
        private Long invoiceDate;
        private BigDecimal invoiceAmount;
        private BigDecimal receiptReceived;
        private BigDecimal creditDebitNoteApplied;
        private BigDecimal payableAmount;
        private Boolean isPaid;
    }
}
