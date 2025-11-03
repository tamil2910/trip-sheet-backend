package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "drivers")
public class Driver extends BaseModel {
  @Size(min = 2, max = 20, message = "Name must be between 2 and 20 characters")
  private String name;
  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;
  private String profile_picture;
  private String deviceId;

  @NotBlank(message = "Password is required")
  @Size(min = 8, message = "Password must be at least 8 characters long")
  @Pattern(
    regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
    message = "Password must contain at least one uppercase letter, one number, and one special character"
  )
  @Column(nullable = true)
  private String password;

  private String googleId; // store Google’s unique user ID

  @Valid
  @ManyToOne(cascade = CascadeType.MERGE ,fetch = FetchType.EAGER)
  private Role role;
}
