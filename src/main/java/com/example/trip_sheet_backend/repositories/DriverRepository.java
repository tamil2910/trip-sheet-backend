package com.example.trip_sheet_backend.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.trip_sheet_backend.models.Driver;

@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {

}