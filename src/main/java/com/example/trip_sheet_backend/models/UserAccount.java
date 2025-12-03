package com.example.trip_sheet_backend.models;

import java.util.UUID;

import com.example.trip_sheet_backend.common.models.BaseModel;

import io.micrometer.common.lang.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
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
public class UserAccount extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String username;

    @Email
    private String email;
    
    @Nullable
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be 10 digits and start with 6-9")
    private String phone;

    @Nullable
    @Pattern(
    regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
    message = "Password must be at least 8 characters, include letters and numbers")
    private String password; // hashed

    @Enumerated(EnumType.STRING)
    private LoginType loginType; // "EMAIL", "PHONE", "USERNAME", "OAUTH", "GOOGLE"

    private String googleId;  // store Google’s unique ID
    private String profilePicture;
    
    private String deviceId;  // store users’s device ID

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    private Boolean active = true;

    public enum LoginType {
        GOOGLE, EMAIL, PHONE, USERNAME, OAUTH
    }
}
