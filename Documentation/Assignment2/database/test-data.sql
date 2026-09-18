INSERT INTO animal (registration_number, weight)
VALUES ('A-1001', 120.5),
       ('A-1002', 135.2),
       ('A-1003', 110.8);


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


-- Create two products

INSERT INTO product DEFAULT
VALUES;
INSERT INTO product DEFAULT
VALUES;


-- Product 1 uses trays 1, 2 and 3

INSERT INTO product_tray (product_id, tray_id)
VALUES (1, 1),
       (1, 2),
       (1, 3);


-- Product 2 uses tray 3

INSERT INTO product_tray (product_id, tray_id)
VALUES (2, 3);