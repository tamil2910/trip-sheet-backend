package com.example.trip_sheet_backend.models;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.ManyToOne;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "people_tenant_custom_field_values", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "people_tenant_id", "custom_field_id" })
})
public class PeopleTenantCustomFieldValue extends BaseModel {
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "people_tenant_id", nullable = false)
  private PeopleTenant peopleTenant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "custom_field_id", nullable = false)
  private CustomField customField;

  private String value;
}
