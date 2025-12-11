package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "permissions")
public class Permission extends BaseModel {

    @Column(unique = true, nullable = false)
    private String name; // e.g. TRIP_CREATE, TRIP_VIEW

    private String description;

    // NEW FIELD
    @Column(nullable = false)
    private String moduleName; // e.g. CONTRACT, INVOICE, DUTY, EXPENSE
}
