ALTER TABLE tenant_join_requests
  ADD COLUMN approver_user_account_id BINARY(16) NULL,
  ADD COLUMN approver_name VARCHAR(255) NULL,
  ADD COLUMN approver_email VARCHAR(255) NULL,
  ADD CONSTRAINT fk_tenant_join_request_approver
    FOREIGN KEY (approver_user_account_id) REFERENCES user_accounts (id);
