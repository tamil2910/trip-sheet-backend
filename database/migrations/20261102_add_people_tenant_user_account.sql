ALTER TABLE people_tenant
  ADD COLUMN user_account_id BINARY(16) NULL,
  ADD INDEX idx_people_tenant_user_account (user_account_id),
  ADD CONSTRAINT fk_people_tenant_user_account
    FOREIGN KEY (user_account_id) REFERENCES user_accounts (id);
