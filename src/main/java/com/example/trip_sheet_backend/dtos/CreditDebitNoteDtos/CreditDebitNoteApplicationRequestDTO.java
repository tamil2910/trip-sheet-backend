package com.example.trip_sheet_backend.dtos.CreditDebitNoteDtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreditDebitNoteApplicationRequestDTO {
    @Valid
    private List<InvoiceApplication> invoiceApplications;

    @Valid
    private List<PurchaseInvoiceApplication> purchaseInvoiceApplications;

    @Getter
    @Setter
    public static class InvoiceApplication {
        @NotNull(message = "invoiceId is required")
        private UUID invoiceId;
        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        private BigDecimal amount;
    }

    @Getter
    @Setter
    public static class PurchaseInvoiceApplication {
        @NotNull(message = "purchaseInvoiceId is required")
        private UUID purchaseInvoiceId;
        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        private BigDecimal amount;
    }

    public boolean hasApplications() {
        return (invoiceApplications != null && !invoiceApplications.isEmpty())
            || (purchaseInvoiceApplications != null && !purchaseInvoiceApplications.isEmpty());
    }
}
