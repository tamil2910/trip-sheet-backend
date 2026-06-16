package com.example.trip_sheet_backend.repositories;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.PassengerOrganisationTenantMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PassengerOrganisationTenantMappingRepository extends BaseRepository<PassengerOrganisationTenantMapping, UUID> {
    Optional<PassengerOrganisationTenantMapping> findByUserAccount_IdAndOrganisation_Id(UUID userId, UUID organisationId);
    boolean existsByUserAccount_IdAndOrganisation_Id(UUID userId, UUID organisationId);
}