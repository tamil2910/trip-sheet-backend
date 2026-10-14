package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.trip_sheet_backend.models.TripLocationUpdate;

public interface TripLocationUpdateRepository extends JpaRepository<TripLocationUpdate, UUID> {
  Optional<TripLocationUpdate> findFirstByTrip_IdOrderByRecordedAtDesc(UUID tripId);

  @Query("select point from TripLocationUpdate point where point.trip.id = :tripId "
      + "and (:fromTime is null or point.recordedAt >= :fromTime) "
      + "and (:toTime is null or point.recordedAt <= :toTime) "
      + "order by point.recordedAt asc, point.receivedAt asc")
  Page<TripLocationUpdate> findHistory(@Param("tripId") UUID tripId,
      @Param("fromTime") Long fromTime, @Param("toTime") Long toTime, Pageable pageable);
}
