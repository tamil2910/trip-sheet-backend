package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "vehicle_types", uniqueConstraints = {
  @UniqueConstraint(columnNames = {"type_of_vehicle", "seat_count"})
})
public class VehicleType extends BaseModel {

  @NotBlank(message = "Default name is required")
  private String default_name;
  
  private String description;

  @NotNull(message = "Seat count is required")
  @Min(value = 1, message = "Seat count must be at least 1")
  private Integer seat_count;

  @NotNull(message = "Vehicle type is required")
  @Enumerated(EnumType.STRING)
  private typeVehicle type_of_vehicle;

  public enum typeVehicle {
    SEDAN, HATCHBACK, SUV, MUV
  }

  private Boolean is_global = true;
}
