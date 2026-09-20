CREATE TABLE animal
(
    animal_id           SERIAL PRIMARY KEY,
    registration_number VARCHAR(100) UNIQUE NOT NULL,
    weight              DOUBLE PRECISION    NOT NULL,
    arrival_time        TIMESTAMP           NOT NULL
);

CREATE TABLE part_type
(
    part_type_id SERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL
);

CREATE TABLE tray
(
    tray_id      SERIAL PRIMARY KEY,
    part_type_id INT              NOT NULL REFERENCES part_type (part_type_id),
    max_weight   DOUBLE PRECISION NOT NULL
);

CREATE TABLE part
(
    part_id      SERIAL PRIMARY KEY,
    animal_id    INT              NOT NULL REFERENCES animal (animal_id),
    tray_id      INT REFERENCES tray (tray_id),
    part_type_id INT              NOT NULL REFERENCES part_type (part_type_id),
    weight       DOUBLE PRECISION NOT NULL
);

CREATE TABLE product_type
(
    product_type_id SERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL
);

CREATE TABLE product
(
    product_id      SERIAL PRIMARY KEY,
    product_type_id INT       NOT NULL REFERENCES product_type (product_type_id),
    packaged_at     TIMESTAMP NOT NULL
);

CREATE TABLE product_tray
(
    product_id INT NOT NULL REFERENCES product (product_id),
    tray_id    INT NOT NULL REFERENCES tray (tray_id),

    PRIMARY KEY (product_id, tray_id)
);