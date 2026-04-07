package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "trip_stops")
public class TripStop extends BaseModel {
   @ManyToOne
    @JoinColumn(name = "trip_id")
    private Trip trip;

    private Integer sequenceNumber; // 1,2,3... defines order

    @Enumerated(EnumType.STRING)
    private StopType stopType; // PICKUP or DROP

    private String addressText;           // raw dropdown text
    private String formattedAddress;      // formatted Google address
    private Double latitude;
    private Double longitude;
    private Boolean accurate;

    public enum StopType {
        PICKUP,
        DROP
    }

}
