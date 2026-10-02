package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "labels", indexes = {
    @Index(name = "idx_label_tenant_deleted_name", columnList = "tenant_id, is_deleted, name")
})
@Getter 
@Setter 
public class Label extends BaseModel implements TenantScoped {
    @NotBlank(message = "Label name is required")
    @Column(nullable = false)
    private String name;

    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Override
    public Tenant getTenant() {
        return tenant;
    }
    @Override
    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }
}
