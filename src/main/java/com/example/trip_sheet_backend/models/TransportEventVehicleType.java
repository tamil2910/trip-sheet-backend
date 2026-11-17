package com.example.trip_sheet_backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transport_event_vehicle_types")
@Getter
@Setter
@NoArgsConstructor
public class TransportEventVehicleType {
  @EmbeddedId
  private TransportEventVehicleTypeId id = new TransportEventVehicleTypeId();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("eventId")
  @JoinColumn(name = "event_id", nullable = false)
  private TransportEvent event;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("vehicleTypeId")
  @JoinColumn(name = "vehicle_type_id", nullable = false)
  private VehicleType vehicleType;

  @Column(name = "allowed_passenger", nullable = false)
  private int allowedPassenger;
}
