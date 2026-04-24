package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
    name = "trip_passenger_custom_field_values",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "trip_id", "passenger_id", "custom_field_id" })
    },
    indexes = {
        @Index(name = "idx_tpcfv_trip", columnList = "trip_id"),
        @Index(name = "idx_tpcfv_passenger", columnList = "passenger_id"),
        @Index(name = "idx_tpcfv_custom_field", columnList = "custom_field_id")
    })
public class TripPassengerCustomFieldValue extends BaseModel {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trip_id", nullable = false)
  private Trip trip;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "passenger_id", nullable = false)
  private PeopleTenant passenger;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "custom_field_id", nullable = false)
  private CustomField customField;

  @Column(name = "field_value", length = 1000)
  private String value;
}
