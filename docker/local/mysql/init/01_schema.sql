-- Runs once on first initialization of the data volume
CREATE DATABASE IF NOT EXISTS order_local;
USE order_local;

-- V1: create `order` table (reserved word → backticks required)
CREATE TABLE IF NOT EXISTS `order`
(
    id                 BIGINT       NOT NULL COMMENT 'PK' PRIMARY KEY,
    user_id            BIGINT       NOT NULL COMMENT '회원ID',
    user_number        VARCHAR(50)  NOT NULL COMMENT '회원번호',
    order_id           BIGINT       NOT NULL COMMENT '주문ID',
    order_number       VARCHAR(50)  NOT NULL COMMENT '주문번호',
    order_price        BIGINT       NOT NULL COMMENT '주문금액',
    published_datetime DATETIME     NULL     COMMENT '구독일시',
    delete_yn          VARCHAR(1)   NULL     COMMENT '삭제여부',
    created_user_id    BIGINT       NULL     COMMENT '등록자',
    created_user_type  VARCHAR(20)  NULL     COMMENT '등록자유형',
    created_datetime   DATETIME     NOT NULL COMMENT '등록일시',
    modified_user_id   BIGINT       NULL     COMMENT '수정자',
    modified_user_type VARCHAR(20)  NULL     COMMENT '수정자유형',
    modified_datetime  DATETIME     NOT NULL COMMENT '수정일시',
    version            BIGINT       NOT NULL COMMENT 'Data Version'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- V2: recommended indexes
CREATE INDEX idx_order__order_id               ON `order` (order_id);
CREATE INDEX idx_order__user_id                ON `order` (user_id);
CREATE INDEX idx_order__published_datetime     ON `order` (published_datetime);
CREATE INDEX idx_order__user_id__order_id      ON `order` (user_id, order_id);
