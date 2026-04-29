package com.example.trip_sheet_backend.models;

import java.util.Locale;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
@Table(name = "dispatch_centers")
public class DispatchCenter extends BaseModel implements TenantScoped {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "address", nullable = false)
  private String address;

  @Column(name = "phone", nullable = false)
  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(name = "dispatch_center_type", nullable = false)
  private DispatchCenterType type;

  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  public enum DispatchCenterType {
    OFFICE("office"),
    DISPATCH_CENTER("dispatch center"),
    OFFICE_AND_DISPATCH_CENTER("office & dispatch center");

    private final String value;

    DispatchCenterType(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @JsonCreator
    public static DispatchCenterType fromValue(String value) {
      if (value == null) {
        return null;
      }

      String normalized = value.trim().toLowerCase(Locale.ROOT);
      return switch (normalized) {
        case "office" -> OFFICE;
        case "dispatch center", "dispatch_center", "dispatch-center" -> DISPATCH_CENTER;
        case "office & dispatch center", "office and dispatch center", "office_dispatch_center", "office-dispatch-center" -> OFFICE_AND_DISPATCH_CENTER;
        default -> throw new IllegalArgumentException("Invalid dispatch center type: " + value);
      };
    }
  }
}