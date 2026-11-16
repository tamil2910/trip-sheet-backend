package com.example.trip_sheet_backend.dtos;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.example.trip_sheet_backend.models.VehicleServiceRecord.ServiceType;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VehicleServiceRequest {
  @NotNull private UUID vehicleId;
  @NotNull private ServiceType serviceType;
  private UUID authorizedServiceCenterId;
  @Size(max=180) private String centerName;
  @Size(max=160) private String callingPersonName;
  @Size(max=40) private String phoneNumber;
  private UUID dispatchCenterId;
  @NotNull private Long requestDate;
  private Long estimatedCompletionDate;
  @NotBlank @Size(max=500) private String takenInFor;
  @NotNull @PositiveOrZero private Integer odometerKm;
  private String requestNotes;
  @DecimalMin("0.00") private BigDecimal estimatedAmount;
  @DecimalMin("0.00") private BigDecimal paidAmount;
  private List<@Valid VehicleServiceExpenseItemRequest> expenseItems;
  @JsonAlias("invoiceAttachments")
  private List<@Valid VehicleServiceInvoiceRequest> invoices;
}
