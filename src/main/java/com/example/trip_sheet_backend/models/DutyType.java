package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import io.micrometer.common.lang.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
// import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "duty_types")
public class DutyType extends BaseModel {
  @Nullable
  @Min(value = 0, message = "KM cannot be negative")
  private Integer km;

  @Nullable
  @Min(value = 0, message = "HR cannot be negative")
  private Integer hr;

  @Nullable
  @Min(value = 1, message = "Max hours per day must be at least 1")
  private Integer maxHrPerDay;

  @Nullable
  @Min(value = 0, message = "Total hours must be 0 or above")
  private Integer totalHr;

  @Nullable
  @Min(value = 0, message = "Total KM must be 0 or above")
  private Integer totalKm;

  @Nullable
  @Min(value = 1, message = "Max days must be at least 1")
  private Integer maxDays;

  // @NotBlank(message = "Duty type name is required")
  private String name;

  @NotNull(message = "Type of duty is required")
  @Enumerated(EnumType.STRING)
  @Column(name = "type_of_duty")
  private typeDuty typeOfDuty;

  @Nullable
  @Enumerated(EnumType.STRING)
  private TypeAirportTransfer airportTransferType;

  private Boolean isGlobal = true;

  public enum typeDuty {
      LOCAL,
      OUTSTATION,
      AIRPORT_TRANSFER_FIXED,
      AIRPORT_TRANSFER_KM,
      MONTHLY_BOOKING_MAX_HR,
      MONTHLY_BOOKING_TOTAL_HR,
      PICKUP_DROP
  }

  public enum TypeAirportTransfer {
      PICKUP,
      DROP
  }
}

