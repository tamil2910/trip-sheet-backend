package com.example.trip_sheet_backend.dtos.ReceiptDtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiptApplyRequestDTO {
    /** Invoices are paid in this order until amount is exhausted. */
    @NotEmpty(message = "invoiceIds is required")
    private List<UUID> invoiceIds;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than zero")
    private BigDecimal amount;
}
