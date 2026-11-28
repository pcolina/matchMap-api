ALTER TABLE city ADD COLUMN city_key varchar(50) NOT NULL;

ALTER TABLE city ADD CONSTRAINT uk_city_key UNIQUE (city_key);
