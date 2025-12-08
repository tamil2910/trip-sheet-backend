package com.example.trip_sheet_backend.models;

import java.util.HashSet;
import java.util.Set;

import com.example.trip_sheet_backend.common.models.BaseModel;

import io.micrometer.common.lang.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "roles")
public class Role extends BaseModel {
  @Column(unique = true, nullable = false)
  @Size(min = 2, max = 20, message = "Name must be between 2 and 20 characters")
  private String name;

  @NotBlank(message = "Description is required")
  private String description;

  @Nullable
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "role_permissions",
      joinColumns = @JoinColumn(name = "role_id"),             // this entity
      inverseJoinColumns = @JoinColumn(name = "permission_id") // Permission entity
  )
  private Set<Permission> permissions = new HashSet<>();

}
