-- V2: add indexes to `order`
-- 조회/정렬/동기화 시나리오 기준 권장 인덱스 셋

CREATE INDEX idx_order__order_id               ON `order` (order_id);
CREATE INDEX idx_order__user_id                ON `order` (user_id);
CREATE INDEX idx_order__published_datetime     ON `order` (published_datetime);
CREATE INDEX idx_order__user_id__order_id      ON `order` (user_id, order_id);

-- 필요 시 고유 제약 (업무 규칙에 맞을 때만 사용)
-- CREATE UNIQUE INDEX uq_order__order_number  ON `order` (order_number);
