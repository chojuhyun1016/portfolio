-- V3: minimal seed (idempotent-ish)
INSERT INTO `order` (id,
                     user_id,
                     user_number,
                     order_id,
                     order_number,
                     order_price,
                     published_datetime,
                     delete_yn,
                     created_user_id,
                     created_user_type,
                     created_datetime,
                     modified_user_id,
                     modified_user_type,
                     modified_datetime,
                     version)
VALUES (1,
        1001,
        'U1001',
        5001,
        'O5001',
        25000,
        NOW(),
        'N',
        1001,
        'SYSTEM',
        NOW(),
        1001,
        'SYSTEM',
        NOW(),
        1)
    ON DUPLICATE KEY
UPDATE order_id =
VALUES (order_id);
