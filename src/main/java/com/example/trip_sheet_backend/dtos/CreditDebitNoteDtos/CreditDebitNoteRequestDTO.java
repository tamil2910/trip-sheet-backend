package com.example.trip_sheet_backend.dtos.CreditDebitNoteDtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.CreditDebitNote.ApplyTo;
import com.example.trip_sheet_backend.models.CreditDebitNote.NoteType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreditDebitNoteRequestDTO {
    @NotNull(message = "noteDate is required")
    private Long noteDate;
    @NotNull(message = "noteType is required")
    private NoteType noteType;
    @NotNull(message = "applyTo is required")
    private ApplyTo applyTo;
    private UUID organisationId;
    private UUID vendorPartnerId;
    @NotNull(message = "isNonTaxable is required")
    private Boolean isNonTaxable;
    private List<UUID> taxIds;
    @PositiveOrZero(message = "taxAmount must not be negative")
    private BigDecimal taxAmount;
    @PositiveOrZero(message = "taxableSubTotal must not be negative")
    private BigDecimal taxableSubTotal;
    @NotNull(message = "totalAmount is required")
    @PositiveOrZero(message = "totalAmount must not be negative")
    private BigDecimal totalAmount;
    private String attachmentUrl;
    private String comments;
}
