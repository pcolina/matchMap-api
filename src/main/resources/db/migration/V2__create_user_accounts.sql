CREATE TABLE user_account (
    id             uuid         PRIMARY KEY,
    email          varchar(254) NOT NULL,
    username       varchar(30)  NOT NULL,
    display_name   varchar(50)  NOT NULL,
    password_hash  varchar(255),
    google_subject varchar(255),
    created_at     timestamptz  NOT NULL,
    CONSTRAINT uk_user_account_email UNIQUE (email),
    CONSTRAINT uk_user_account_username UNIQUE (username),
    CONSTRAINT uk_user_account_google_subject UNIQUE (google_subject)
);

CREATE TABLE refresh_token (
    id         uuid        PRIMARY KEY,
    account_id uuid        NOT NULL REFERENCES user_account (id),
    session_id uuid        NOT NULL,
    token_hash varchar(64) NOT NULL,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    used_at    timestamptz,
    revoked_at timestamptz,
    CONSTRAINT uk_refresh_token_token_hash UNIQUE (token_hash)
);

CREATE INDEX ix_refresh_token_session_id ON refresh_token (session_id);
CREATE INDEX ix_refresh_token_account_id ON refresh_token (account_id);
