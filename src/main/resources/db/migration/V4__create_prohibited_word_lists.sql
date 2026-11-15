CREATE TABLE prohibited_word (
    id          uuid         PRIMARY KEY,
    word        varchar(100) NOT NULL,
    normalized  varchar(100) NOT NULL,
    category    varchar(20)  NOT NULL,
    language    varchar(5)   NOT NULL,
    active      boolean      NOT NULL,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL,
    CONSTRAINT ck_prohibited_word_category CHECK (category IN ('CHILD_SAFETY', 'HATE', 'VIOLENCE', 'SEXUAL', 'IMPERSONATION', 'RESERVED')),
    CONSTRAINT uk_prohibited_word_normalized_category UNIQUE (normalized, category)
);

CREATE TABLE reserved_word (
    id          uuid         PRIMARY KEY,
    word        varchar(100) NOT NULL,
    normalized  varchar(100) NOT NULL,
    category    varchar(20)  NOT NULL,
    language    varchar(5)   NOT NULL,
    active      boolean      NOT NULL,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL,
    CONSTRAINT ck_reserved_word_category CHECK (category IN ('CHILD_SAFETY', 'HATE', 'VIOLENCE', 'SEXUAL', 'IMPERSONATION', 'RESERVED')),
    CONSTRAINT uk_reserved_word_normalized_category UNIQUE (normalized, category)
);
