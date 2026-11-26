ALTER TABLE user_account
    ADD COLUMN first_name varchar(100),
    ADD COLUMN last_name varchar(100),
    ADD COLUMN nationality varchar(2),
    ADD COLUMN birth_country varchar(2),
    ADD COLUMN subscription varchar(20) NOT NULL DEFAULT 'FREE',
    ADD CONSTRAINT ck_user_account_subscription CHECK (subscription IN ('FREE')),
    ADD CONSTRAINT ck_user_account_nationality CHECK (nationality ~ '^[A-Z]{2}$'),
    ADD CONSTRAINT ck_user_account_birth_country CHECK (birth_country ~ '^[A-Z]{2}$');
