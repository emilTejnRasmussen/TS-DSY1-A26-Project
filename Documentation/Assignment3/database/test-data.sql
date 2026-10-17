INSERT INTO animal (registration_number)
VALUES ('DK-2026-TEST001'),
       ('DK-2026-TEST002');

INSERT INTO part (part_id, animal_registration_number)
VALUES (1, 'DK-2026-TEST001'),
       (2, 'DK-2026-TEST001'),
       (3, 'DK-2026-TEST002');

INSERT INTO tray (tray_id)
VALUES (1), (2);

INSERT INTO tray_part (tray_id, part_id)
VALUES (1, 1),
       (1, 2),
       (2, 3);

INSERT INTO product (product_id)
VALUES (1), (2);

INSERT INTO product_tray (product_id, tray_id)
VALUES (1, 1),
       (1, 2),
       (2, 2);