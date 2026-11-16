package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicle_service_progress_updates")
@Getter
@Setter
@NoArgsConstructor
public class VehicleServiceProgressUpdate extends BaseModel {
  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_service_id", nullable = false)
  private VehicleServiceRecord vehicleService;

  @Column(nullable = false, length = 4000)
  private String description;

  @Column(name = "photo_key", length = 1024)
  private String photoKey;

  @Column(name = "updated_by_name", nullable = false, length = 255)
  private String updatedByName;
}
