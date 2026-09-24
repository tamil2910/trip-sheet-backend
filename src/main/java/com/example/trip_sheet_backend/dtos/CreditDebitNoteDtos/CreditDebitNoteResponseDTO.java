package com.example.trip_sheet_backend.dtos.CreditDebitNoteDtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.CreditDebitNote;
import com.example.trip_sheet_backend.models.CreditDebitNote.ApplyTo;
import com.example.trip_sheet_backend.models.CreditDebitNote.NoteType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreditDebitNoteResponseDTO {
    private UUID id;
    private String noteNumber;
    private Long noteDate;
    private NoteType noteType;
    private ApplyTo applyTo;
    private UUID organisationId;
    private UUID vendorPartnerId;
    private Boolean isNonTaxable;
    private List<UUID> taxIds;
    private BigDecimal taxAmount;
    private BigDecimal taxableSubTotal;
    private BigDecimal totalAmount;
    private BigDecimal remainingAmount;
    private List<InvoiceApplication> invoiceApplications;
    private List<PurchaseInvoiceApplication> purchaseInvoiceApplications;
    private String attachmentUrl;
    private String comments;

    public static CreditDebitNoteResponseDTO fromEntity(CreditDebitNote note) {
        return new CreditDebitNoteResponseDTO(note.getId(), note.getNoteNumber(), note.getNoteDate(), note.getNoteType(), note.getApplyTo(),
            note.getOrganisation() == null ? null : note.getOrganisation().getId(),
            note.getVendorPartner() == null ? null : note.getVendorPartner().getId(), note.getIsNonTaxable(),
            note.getTaxList().stream().map(tax -> tax.getId()).toList(), note.getTaxAmount(), note.getTaxableSubTotal(),
            note.getTotalAmount(), note.getRemainingAmount(),
            note.getInvoiceApplications().stream().filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .map(item -> new InvoiceApplication(item.getInvoice().getId(), item.getAmount())).toList(),
            note.getPurchaseInvoiceApplications().stream().filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .map(item -> new PurchaseInvoiceApplication(item.getPurchaseInvoice().getId(), item.getAmount())).toList(),
            note.getAttachmentUrl(), note.getComments());
    }

    @Getter
    @AllArgsConstructor
    public static class InvoiceApplication {
        private UUID invoiceId;
        private BigDecimal amount;
    }

    @Getter
    @AllArgsConstructor
    public static class PurchaseInvoiceApplication {
        private UUID purchaseInvoiceId;
        private BigDecimal amount;
    }
}
