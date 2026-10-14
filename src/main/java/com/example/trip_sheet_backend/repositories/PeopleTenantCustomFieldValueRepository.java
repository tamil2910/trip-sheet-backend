package com.example.trip_sheet_backend.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.PeopleTenantCustomFieldValue;

public interface PeopleTenantCustomFieldValueRepository
    extends BaseRepository<PeopleTenantCustomFieldValue, UUID> {

  @Modifying
  @Query("delete from PeopleTenantCustomFieldValue v where v.peopleTenant.id = :personId and v.createdByVendor is null")
  void deleteOrganisationValues(@Param("personId") UUID personId);

  @Modifying
  @Query("delete from PeopleTenantCustomFieldValue v where v.peopleTenant.id = :personId and v.createdByVendor.id = :vendorId")
  void deleteVendorValues(@Param("personId") UUID personId, @Param("vendorId") UUID vendorId);
}
