package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "authorized_service_centers", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "name"}))
@Getter @Setter @NoArgsConstructor
public class AuthorizedServiceCenter extends BaseModel implements TenantScoped {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @Column(nullable = false, length = 180)
  private String name;

  @Column(name = "contact_person_name", nullable = false, length = 160)
  private String contactPersonName;

  @Column(name = "phone_number", nullable = false, length = 40)
  private String phoneNumber;
}
