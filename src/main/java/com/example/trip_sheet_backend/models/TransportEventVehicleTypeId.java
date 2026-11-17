package com.example.trip_sheet_backend.models;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class TransportEventVehicleTypeId implements Serializable {
  @Column(name = "event_id")
  private UUID eventId;

  @Column(name = "vehicle_type_id")
  private UUID vehicleTypeId;

  @Override public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof TransportEventVehicleTypeId that)) return false;
    return Objects.equals(eventId, that.eventId) && Objects.equals(vehicleTypeId, that.vehicleTypeId);
  }

  @Override public int hashCode() { return Objects.hash(eventId, vehicleTypeId); }
}
