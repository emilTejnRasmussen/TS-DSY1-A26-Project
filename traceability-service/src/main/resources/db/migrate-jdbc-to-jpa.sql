-- Run manually against the existing Traceability database while this service is stopped.
-- Preserve all existing rows and tray relationships. Do not infer product_part rows.
BEGIN;

ALTER TABLE part ALTER COLUMN part_id TYPE BIGINT;
ALTER TABLE tray ALTER COLUMN tray_id TYPE BIGINT;
ALTER TABLE product ALTER COLUMN product_id TYPE BIGINT;
ALTER TABLE tray_part ALTER COLUMN part_id TYPE BIGINT;
ALTER TABLE tray_part ALTER COLUMN tray_id TYPE BIGINT;
ALTER TABLE product_tray ALTER COLUMN product_id TYPE BIGINT;
ALTER TABLE product_tray ALTER COLUMN tray_id TYPE BIGINT;

ALTER TABLE part ADD COLUMN IF NOT EXISTS type VARCHAR(255);
ALTER TABLE part ADD COLUMN IF NOT EXISTS weight NUMERIC(10,2);

CREATE TABLE IF NOT EXISTS product_part
(
    product_id BIGINT NOT NULL REFERENCES product(product_id),
    part_id BIGINT NOT NULL REFERENCES part(part_id),
    PRIMARY KEY (product_id, part_id),
    UNIQUE (part_id)
);

CREATE TABLE IF NOT EXISTS failed_event
(
    id UUID PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    reason TEXT NOT NULL,
    failed_at TIMESTAMP NOT NULL
);

COMMIT;
