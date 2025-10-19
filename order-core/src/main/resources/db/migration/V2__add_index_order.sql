-- V2: Minimal indexes for local/dev
-- 이미 V1에 UNIQUE(order_id), UNIQUE(order_number)가 있으므로
-- 자주 쓰일 복합 조회만 보강: (user_id, created_datetime)
-- 존재 시 건너뛰는 멱등 패턴 (INFORMATION_SCHEMA)

-- ===== `order` =====
SET @idx_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'order'
    AND INDEX_NAME = 'idx_order__user_id_created_datetime'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_order__user_id_created_datetime ON `order` (user_id, created_datetime)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ===== `local_order` =====
SET @idx_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'local_order'
    AND INDEX_NAME = 'idx_local_order__user_id_created_datetime'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_local_order__user_id_created_datetime ON `local_order` (user_id, created_datetime)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
