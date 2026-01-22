package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.PeopleBookerTenant;

public interface PeopleBookerTenantRepository extends BaseRepository<PeopleBookerTenant, UUID> {
  Optional<PeopleBookerTenant> findByPhoneAndOrganisation_Id(String phone, UUID organisationId);
  Optional<PeopleBookerTenant> findByNameAndPhoneAndOrganisation_Id(String name, String phone, UUID organisationId);
}
