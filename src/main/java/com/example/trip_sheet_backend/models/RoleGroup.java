package com.example.trip_sheet_backend.models;

import java.util.HashSet;
import java.util.Set;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "role_groups")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RoleGroup extends BaseModel implements TenantScoped {

    @Column(nullable = false)
    private String name;  // Example: "Dispatch Operators", "Billing Team"

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;   // Role group belongs to specific vendor

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_group_permissions",
            joinColumns = @JoinColumn(name = "role_group_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    @Override
    public Tenant getTenant() {
        return tenant;
    }
}