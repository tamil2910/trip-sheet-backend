package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;
// import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "admins", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "user_account_id",})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Admin extends BaseModel {

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinColumn(referencedColumnName = "id", nullable = false, unique = true, name = "user_account_id")
    private UserAccount userAccount;

    private Boolean isActive = true;

    private String profilePicture;
}
