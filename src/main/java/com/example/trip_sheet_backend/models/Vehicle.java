package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.google.api.client.util.NullValue;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vehicles")
public class Vehicle extends BaseModel {
  @Column(unique = true, nullable = false)
  @Size(min = 2, max = 20, message = "Model name must be between 2 and 20 characters")
  private String modelName;
  
  @NotBlank(message="Vehicle Number is required!")
  private String vehicleNumber;

  @NotNull
  @JoinColumn(name="vehilce_type_id")
  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
  private VehicleType vehicleType;

  @NullValue
  private String VehicleUniqueCode;
  
  @NullValue
  private String leftSideUrl;

  @NullValue
  private String rightSideUrl;

  @NullValue
  private String backSideUrl;

  @NullValue
  private String frontSideUrl;

  @NullValue
  private String vehProfileUrl;

  @NotBlank(message = "Description is required")
  private String description;

  @NullValue
  private String colour;

  @NotNull(message = "Fuel type is required!")
  private typeFuel fuelType;

  public enum typeFuel {
    DIESEL, ELECTRIC, PETROL, CNG, PETROL_CNG
  }

  @NullValue
  private String registeredOwnerName;

  @NullValue
  private String registrationDate;

  @NullValue
  private String chassisNumber;

  @NullValue
  private String engineNumber;

  @NullValue
  private String insuranceCompanyName;

  @NullValue
  private String policyNumber;

  @NullValue
  private String issueDate;

  @NullValue
  private String dueDate;

  @NullValue
  private String premiumAmount;

  @NullValue
  private String coverAmount;

}
