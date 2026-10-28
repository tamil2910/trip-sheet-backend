package com.example.trip_sheet_backend.services;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.trip_sheet_backend.models.BankAccount;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.BankAccountRepository;

@Service
public class BankAccountService {
    private final BankAccountRepository repository;

    public BankAccountService(BankAccountRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BankAccount> list(UUID tenantId) {
        return repository.findAllByTenant_IdAndIsDeletedFalse(tenantId);
    }

    @Transactional(readOnly = true)
    public BankAccount get(UUID tenantId, UUID id) {
        return repository.findByIdAndTenant_IdAndIsDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank account not found"));
    }

    @Transactional
    public BankAccount create(UUID tenantId, UUID actorId, BankAccount input) {
        BankAccount account = new BankAccount();
        copyFields(input, account);
        account.setTenant(tenantReference(tenantId));
        account.setCreatedBy(actorId == null ? null : actorId.toString());
        account.setUpdatedBy(actorId == null ? null : actorId.toString());
        if (account.getPrimary_account() == null) account.setPrimary_account(false);
        if (account.getIs_active() == null) account.setIs_active(true);
        return repository.save(account);
    }

    @Transactional
    public BankAccount update(UUID tenantId, UUID actorId, UUID id, BankAccount input) {
        BankAccount account = get(tenantId, id);
        copyFields(input, account);
        account.setUpdatedBy(actorId == null ? null : actorId.toString());
        return repository.save(account);
    }

    @Transactional
    public void delete(UUID tenantId, UUID actorId, UUID id) {
        BankAccount account = get(tenantId, id);
        account.setIs_active(false);
        account.setIsDeleted(true);
        account.setDeletedAt(System.currentTimeMillis());
        account.setDeletedBy(actorId == null ? null : actorId.toString());
        repository.save(account);
    }

    private static void copyFields(BankAccount source, BankAccount target) {
        target.setBank_name(source.getBank_name());
        target.setAccount_holder_name(source.getAccount_holder_name());
        target.setAccount_number(source.getAccount_number());
        target.setBranch_name(source.getBranch_name());
        target.setBranch_address(source.getBranch_address());
        target.setIfsc_code(source.getIfsc_code());
        target.setUpi_id(source.getUpi_id());
        target.setPrimary_account(source.getPrimary_account());
        target.setIs_active(source.getIs_active());
    }

    private static Tenant tenantReference(UUID tenantId) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        return tenant;
    }
}
