CREATE TABLE animal
(
    registration_number VARCHAR(100) PRIMARY KEY
);

CREATE TABLE part
(
    part_id BIGINT PRIMARY KEY,
    animal_registration_number VARCHAR(100) NOT NULL REFERENCES animal(registration_number),
    type VARCHAR(255),
    weight NUMERIC(10,2)
);

CREATE TABLE tray
(
    tray_id BIGINT PRIMARY KEY
);

CREATE TABLE tray_part
(
    tray_id BIGINT NOT NULL REFERENCES tray(tray_id),
    part_id BIGINT NOT NULL REFERENCES part(part_id),
    PRIMARY KEY (tray_id, part_id),
    UNIQUE (part_id)
);

CREATE TABLE product
(
    product_id BIGINT PRIMARY KEY
);

CREATE TABLE product_tray
(
    product_id BIGINT NOT NULL REFERENCES product(product_id),
    tray_id BIGINT NOT NULL REFERENCES tray(tray_id),
    PRIMARY KEY (product_id, tray_id)
);

CREATE TABLE product_part
(
    product_id BIGINT NOT NULL REFERENCES product(product_id),
    part_id BIGINT NOT NULL REFERENCES part(part_id),
    PRIMARY KEY (product_id, part_id),
    UNIQUE (part_id)
);

CREATE TABLE processed_event
(
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE failed_event
(
    id UUID PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    reason TEXT NOT NULL,
    failed_at TIMESTAMP NOT NULL
);
