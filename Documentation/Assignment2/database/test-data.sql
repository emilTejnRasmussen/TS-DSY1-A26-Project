INSERT INTO animal (registration_number, weight, arrival_time)
VALUES ('A-1001', 120.5, '2026-09-18 08:00:00'),
       ('A-1002', 135.2, '2026-09-18 08:15:00'),
       ('A-1003', 110.8, '2026-09-18 08:30:00');


INSERT INTO part_type (name)
VALUES ('Leg'),
       ('Shoulder');


INSERT INTO tray (part_type_id, max_weight)
VALUES ((SELECT part_type_id FROM part_type WHERE name = 'Leg'),
        50),
       ((SELECT part_type_id FROM part_type WHERE name = 'Shoulder'),
        50),
       ((SELECT part_type_id FROM part_type WHERE name = 'Shoulder'),
        50);


-- Tray 1:
-- Contains legs from A-1001 and A-1002

INSERT INTO part (animal_id, tray_id, part_type_id, weight)
VALUES ((SELECT animal_id FROM animal WHERE registration_number = 'A-1001'),
        1,
        (SELECT part_type_id FROM part_type WHERE name = 'Leg'),
        8.5),
       ((SELECT animal_id FROM animal WHERE registration_number = 'A-1002'),
        1,
        (SELECT part_type_id FROM part_type WHERE name = 'Leg'),
        9.1);


-- Tray 2:
-- Contains a shoulder from A-1001

INSERT INTO part (animal_id, tray_id, part_type_id, weight)
VALUES ((SELECT animal_id FROM animal WHERE registration_number = 'A-1001'),
        2,
        (SELECT part_type_id FROM part_type WHERE name = 'Shoulder'),
        6.8);


-- Tray 3:
-- Contains a shoulder from A-1003

INSERT INTO part (animal_id, tray_id, part_type_id, weight)
VALUES ((SELECT animal_id FROM animal WHERE registration_number = 'A-1003'),
        3,
        (SELECT part_type_id FROM part_type WHERE name = 'Shoulder'),
        7.2);


INSERT INTO product_type (name)
VALUES ('Mixed Parts'),
       ('Single Tray Product');


-- Create two products

INSERT INTO product (product_type_id, packaged_at)
VALUES ((SELECT product_type_id
         FROM product_type
         WHERE name = 'Mixed Parts'),
        '2026-09-18 10:00:00'),
       ((SELECT product_type_id
         FROM product_type
         WHERE name = 'Single Tray Product'),
        '2026-09-18 10:30:00');


-- Product 1 uses trays 1, 2 and 3

INSERT INTO product_tray (product_id, tray_id)
VALUES (1, 1),
       (1, 2),
       (1, 3);


-- Product 2 uses tray 3

INSERT INTO product_tray (product_id, tray_id)
VALUES (2, 3);