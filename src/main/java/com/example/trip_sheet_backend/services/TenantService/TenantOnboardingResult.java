package com.example.trip_sheet_backend.services.TenantService;

import com.example.trip_sheet_backend.models.Tenant;

public class TenantOnboardingResult {
    private final Tenant tenant;
    private final boolean newlyCreated;
    private final boolean onboardingUserCreated;
    private final boolean credentialsEmailSent;
    private final String onboardingNote;

    public TenantOnboardingResult(Tenant tenant, boolean newlyCreated) {
        this(tenant, newlyCreated, false, false, "");
    }

    public TenantOnboardingResult(
        Tenant tenant,
        boolean newlyCreated,
        boolean onboardingUserCreated,
        boolean credentialsEmailSent,
        String onboardingNote
    ) {
        this.tenant = tenant;
        this.newlyCreated = newlyCreated;
        this.onboardingUserCreated = onboardingUserCreated;
        this.credentialsEmailSent = credentialsEmailSent;
        this.onboardingNote = onboardingNote;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public boolean isNewlyCreated() {
        return newlyCreated;
    }

    public boolean isOnboardingUserCreated() {
        return onboardingUserCreated;
    }

    public boolean isCredentialsEmailSent() {
        return credentialsEmailSent;
    }

    public String getOnboardingNote() {
        return onboardingNote;
    }
}
