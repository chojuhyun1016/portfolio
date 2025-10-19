-- V3: minimal seed for `local_order` (idempotent-ish)
-- MySQL 8에서는 VALUES()가 비권장이라, 새 레코드 별칭을 사용한 방식 권장

INSERT INTO `local_order` (
    id,
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
    version
)
VALUES
    (
        1,
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
        1
    )
    AS new
ON DUPLICATE KEY UPDATE
     order_id = new.order_id,
     order_number = new.order_number,
     order_price = new.order_price,
     published_datetime = new.published_datetime,
     delete_yn = new.delete_yn,
     modified_user_id = new.modified_user_id,
     modified_user_type = new.modified_user_type,
     modified_datetime = new.modified_datetime,
     version = new.version;
