package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
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
  private Integer km;

  private Integer hr;

  private String name;

  @Enumerated(EnumType.STRING)
  private typeDuty type_of_duty;

  @Enumerated(EnumType.STRING)
  private TypeAirportTransfer airport_transfer_type;

  public enum typeDuty {
    LOCAL, OUTSTATION, AIRPORT_TRANSFER_FIXED, AIRPORT_TRANSFER_KM
  }

  public enum TypeAirportTransfer {
    PICKUP, DROP
  }

  private Boolean is_global = true;
}
