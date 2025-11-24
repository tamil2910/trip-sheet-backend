package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankAccounts extends BaseModel {

    @Valid
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotBlank(message = "Bank name is required")
    private String bank_name;

    @NotBlank(message = "Account holder name is required")
    private String account_holder_name;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[0-9]{6,18}$", message = "Invalid account number format")
    private String account_number;

    @NotBlank(message = "Branch name is required")
    private String branch_name;

    private String branch_address;

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code")
    private String ifsc_code;

    @Pattern(regexp = "^[\\w.-]+@[\\w.-]+$", message = "Invalid UPI ID")
    private String upi_id;

    private Boolean primary_account = false;

    private Boolean is_active = true;
}
