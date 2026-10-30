package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vendor_employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorEmployee extends BaseModel implements TenantScoped {

    private String name;

    @Column(nullable = true)
    private String email;

    @Column(nullable = true)
    private String phone;

    @Column(name = "employee_number", nullable = true)
    private String employeeNumber;

    @Column(name = "is_user", nullable = false)
    private Boolean isUser = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", unique = true)
    private UserAccount account;

    @Override
    public Tenant getTenant() {
        return tenant;
    }

    @Override
    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }
}
