CREATE TABLE country (
    id   uuid         PRIMARY KEY,
    code varchar(2)   NOT NULL,
    name varchar(100) NOT NULL
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
    latitude   numeric(9,6),
    longitude  numeric(9,6)
);
