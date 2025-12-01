package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import io.micrometer.common.lang.Nullable;
import jakarta.persistence.Entity;

import jakarta.persistence.FetchType;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "saved_passengers")
@AllArgsConstructor
@NoArgsConstructor
public class SavedPassenger extends BaseModel {

    private String name;

    @Nullable
    @Email
    private String email;

    @Nullable
    private Long phone;

    @NotNull(message = "User account is required!")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_account_id")
    private UserAccount account;

    @Nullable
    private GenderType gender;

    private Boolean isActive = true;

    public enum GenderType {
        MALE, FEMALE, OTHERS, NONE
    }

    @Nullable
    @JoinColumn(name = "tenant_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Tenant tenant;

    @Nullable
    private AddedUserType byAddedUserType;

    public enum AddedUserType {
        VENDOR, ORGANISATION, GUEST
    }
}
