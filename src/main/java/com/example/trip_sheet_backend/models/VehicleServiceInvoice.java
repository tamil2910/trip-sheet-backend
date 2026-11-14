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
@Table(name = "vehicle_service_invoices")
@Getter
@Setter
@NoArgsConstructor
public class VehicleServiceInvoice extends BaseModel {
  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_service_id", nullable = false)
  private VehicleServiceRecord vehicleService;

  @Column(name = "file_key", nullable = false, length = 1024)
  private String fileKey;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "file_size", nullable = false)
  private Long fileSize;
}
