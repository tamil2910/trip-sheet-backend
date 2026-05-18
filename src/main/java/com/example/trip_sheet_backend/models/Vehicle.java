  package com.example.trip_sheet_backend.models;

  import com.example.trip_sheet_backend.common.models.BaseModel;

  import io.micrometer.common.lang.Nullable;
  import jakarta.persistence.CascadeType;
  import jakarta.persistence.Column;
  import jakarta.persistence.Entity;
  import jakarta.persistence.FetchType;
  import jakarta.persistence.JoinColumn;
  import jakarta.persistence.ManyToOne;
  import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
  @Table(
    name = "vehicles",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "model_name"}),
        @UniqueConstraint(columnNames = {"tenant_id", "vehicle_number"})
    }
  )
  public class Vehicle extends BaseModel {

    // @NotNull(message = "Tenant is required to add vehicle") // tenant id is required if vendor/ organisation adding their vehicle
    @Nullable
    @JoinColumn(name="tenant_id")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    private Tenant tenant;
    
    @Column(name = "model_name", nullable = false)
    @Size(min = 2, max = 20, message = "Model name must be between 2 and 20 characters")
    private String modelName;
    
    @NotBlank(message="Vehicle Number is required!")
    private String vehicleNumber;

    @NotNull
    @JoinColumn(name="vehilce_type_id")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    private VehicleType vehicleType;

    @Nullable
    @Column(name = "vehicle_unique_code", unique = true)
    private String vehicleUniqueCode;
    
    @Nullable
    private String leftSideUrl;

    @Nullable
    private String rightSideUrl;

    @Nullable
    private String backSideUrl;

    @Nullable
    private String frontSideUrl;

    @Nullable
    private String vehProfileUrl;

    // @NotBlank(message = "Description is required")
    @Nullable
    private String description;

    @Nullable
    private String colour;

    @NotNull(message = "Fuel type is required!")
    private typeFuel fuelType;

    public enum typeFuel {
      DIESEL, ELECTRIC, PETROL, CNG, PETROL_CNG
    }

    @Nullable
    private String registeredOwnerName;

    @Nullable
    private String registrationDate;

    @Nullable
    private String chassisNumber;

    @Nullable
    private String engineNumber;

    @Nullable
    private String insuranceCompanyName;

    @Nullable
    private String policyNumber;

    @Nullable
    private String issueDate;

    @Nullable
    private String dueDate;

    @Nullable
    private String premiumAmount;

    @Nullable
    private String coverAmount;

    private Boolean isActive = true;

  }
