CREATE TABLE country (
    id   uuid         PRIMARY KEY,
    code varchar(2)   NOT NULL,
    name varchar(100) NOT NULL,
    CONSTRAINT uk_country_code UNIQUE (code),
    CONSTRAINT ck_country_code CHECK (code ~ '^[A-Z]{2}$')
);

CREATE TABLE country_name (
    country_id uuid         NOT NULL REFERENCES country (id),
    language   varchar(10)  NOT NULL,
    name       varchar(100) NOT NULL,
    PRIMARY KEY (country_id, language)
);

CREATE TABLE city (
    id         uuid         PRIMARY KEY,
    country_id uuid         NOT NULL REFERENCES country (id),
    name       varchar(120) NOT NULL,
    latitude   numeric(9,6) NOT NULL,
    longitude  numeric(9,6) NOT NULL,
    CONSTRAINT ck_city_latitude CHECK (latitude BETWEEN -90 AND 90)
);
