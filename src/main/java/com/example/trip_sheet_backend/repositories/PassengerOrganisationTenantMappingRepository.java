package com.example.trip_sheet_backend.repositories;

import com.example.trip_sheet_backend.models.PassengerOrganisationTenantMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PassengerOrganisationTenantMappingRepository extends JpaRepository<PassengerOrganisationTenantMapping, UUID> {
    Optional<PassengerOrganisationTenantMapping> findByUserAccount_IdAndOrganisation_Id(UUID userId, UUID organisationId);
    boolean existsByUserAccount_IdAndOrganisation_Id(UUID userId, UUID organisationId);
}