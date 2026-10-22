package com.example.trip_sheet_backend.dtos.ExpenseDtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ExpenseBreakdownRequests {
  private ExpenseBreakdownRequests() {}

  public record PettyCashRequest(
      UUID expenseId,
      @NotNull LocalDate spentDate,
      @NotBlank @Size(max = 250) String description,
      @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
      @Size(max = 200) String paidTo,
      String comment) {}

  public record VehicleExpenseRequest(
      UUID expenseId,
      @NotNull UUID vehicleId,
      @NotNull LocalDate expenseDate,
      @NotBlank @Size(max = 250) String description,
      @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
      @Size(max = 200) String vendorName,
      @Size(max = 100) String referenceNumber,
      String comment) {}

  public record StatusActionRequest(@Size(max = 1000) String reason) {}
}
