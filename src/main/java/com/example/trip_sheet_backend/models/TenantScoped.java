package com.example.trip_sheet_backend.models;

public interface TenantScoped {
  Tenant getTenant();

  void setTenant(Tenant tenant);
}
