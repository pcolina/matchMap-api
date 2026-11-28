ALTER TABLE user_account ADD COLUMN role varchar(10) NOT NULL DEFAULT 'USER';

ALTER TABLE user_account ADD CONSTRAINT ck_user_account_role CHECK (role IN ('ADMIN', 'USER'));
