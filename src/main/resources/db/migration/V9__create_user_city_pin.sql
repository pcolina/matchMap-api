CREATE TABLE user_city_pin (
    user_id    uuid         NOT NULL REFERENCES user_account (id),
    city_id    uuid         NOT NULL REFERENCES city (id),
    status     varchar(20)  NOT NULL,
    created_at timestamptz  NOT NULL,
    updated_at timestamptz  NOT NULL,
    CONSTRAINT pk_user_city_pin PRIMARY KEY (user_id, city_id),
    CONSTRAINT ck_user_city_pin_status CHECK (status IN ('VISITED', 'WANT_TO_GO', 'LIVED'))
);
