package com.example.trip_sheet_backend.models;


import java.util.HashSet;
import java.util.Set;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.micrometer.common.lang.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_accounts")
@AllArgsConstructor
@NoArgsConstructor
public class UserAccount extends BaseModel implements TenantScoped {

    private String username;

    @Column(unique = true)
    @Email
    private String email;

    @Column(unique = true)
    @Nullable
    // @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be 10 digits and start with 6-9")
    private String phone;

    @JsonIgnore
    @Nullable
    private String password; // hashed

    @Nullable
    @Enumerated(EnumType.STRING)
    private LoginType loginType; // "EMAIL", "PHONE", "USERNAME", "OAUTH", "GOOGLE"

    @JsonIgnore
    @Nullable
    private String googleId;  // store Google’s unique ID

    @Nullable
    private String profilePicture;
    
    @JsonIgnore
    @Nullable
    private String deviceId;  // store users’s device ID

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    private Boolean isActive = true;

    @Nullable
    private TenantType tenantType;   

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    @JsonIgnore
    private UserAccount createdByUser;

    // @ManyToOne(fetch = FetchType.EAGER)
    // @JoinColumn(name = "role_group_id")
    // private RoleGroup roleGroup;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_role_groups",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_group_id")
    )
    private Set<RoleGroup> roleGroups = new HashSet<>();


    @Nullable
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    public enum TenantType {
        VENDOR, ORGANISATION
    }

    public enum LoginType {
        GOOGLE, EMAIL, PHONE, USERNAME, OAUTH
    }

    @Override
    public Tenant getTenant() {
        return tenant;
    }
}
