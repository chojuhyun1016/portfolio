-- V2: add indexes to `order`
-- 조회/정렬/동기화 시나리오 기준 권장 인덱스 셋
-- 존재하면 건너뛰고, 없을 때만 생성 (멱등)

-- idx_order__order_id
SET @idx_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'order'
    AND INDEX_NAME = 'idx_order__order_id'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_order__order_id ON `order` (order_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- (다른 인덱스들도 동일 패턴으로 추가)
-- 예시:
-- SET @idx_exists := (
--   SELECT COUNT(1)
--   FROM INFORMATION_SCHEMA.STATISTICS
--   WHERE TABLE_SCHEMA = DATABASE()
--     AND TABLE_NAME = 'order'
--     AND INDEX_NAME = 'idx_order__user_id_created_at'
-- );
-- SET @sql := IF(@idx_exists = 0,
--   'CREATE INDEX idx_order__user_id_created_at ON `order` (user_id, created_at)',
--   'SELECT 1');
-- PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
