-- V1: create order table (MySQL)
-- 주의: 테이블명이 예약어(ORDER)라서 반드시 백틱(`) 사용
-- H2 테스트에서는 jdbc:h2:mem:testdb;MODE=MySQL 로 설정 필요

CREATE TABLE IF NOT EXISTS `order`
(
    id                 BIGINT       NOT NULL             COMMENT 'PK' PRIMARY KEY,
    user_id            BIGINT       NOT NULL             COMMENT '회원ID',
    user_number        VARCHAR(50)  NOT NULL             COMMENT '회원번호',
    order_id           BIGINT       NOT NULL             COMMENT '주문ID',
    order_number       VARCHAR(50)  NOT NULL             COMMENT '주문번호',
    order_price        BIGINT       NOT NULL             COMMENT '주문금액',
    published_datetime DATETIME     NOT NULL             COMMENT '구독일시',
    delete_yn          VARCHAR(1)   NOT NULL DEFAULT 'N' COMMENT '삭제여부',
    created_user_id    BIGINT       NOT NULL             COMMENT '등록자',
    created_user_type  VARCHAR(50)  NOT NULL             COMMENT '등록자유형',
    created_datetime   DATETIME     NOT NULL             COMMENT '등록일시',
    modified_user_id   BIGINT       NOT NULL             COMMENT '수정자',
    modified_user_type VARCHAR(50)  NOT NULL             COMMENT '수정자유형',
    modified_datetime  DATETIME     NOT NULL             COMMENT '수정일시',
    version            BIGINT       NOT NULL DEFAULT 0   COMMENT 'Data Version',

    UNIQUE KEY uq_order_order_id (order_id),
    UNIQUE KEY uq_order_order_number (order_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
