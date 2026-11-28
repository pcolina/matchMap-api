ALTER TABLE country ADD COLUMN successor_id uuid;

ALTER TABLE country
    ADD CONSTRAINT fk_country_successor FOREIGN KEY (successor_id) REFERENCES country (id);

ALTER TABLE country
    ADD CONSTRAINT ck_country_successor_not_self CHECK (successor_id <> id);
