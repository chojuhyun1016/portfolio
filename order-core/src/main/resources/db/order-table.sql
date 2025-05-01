create table `order`
(
    id                    bigint            not null comment 'PK' primary key,
    user_id               bigint            not null comment '회원ID',
    user_number           varchar(50)       not null comment '회원번호',
    order_id              bigint            not null comment '주문ID',
    order_number          varchar(50)       not null comment '주문번호',
    order_price           bigint            not null comment '주문금액',
    publish_datetime      datetime          null     comment '구독일시',
    delete_yn             varchar(1)        null     comment '삭제여부',
    created_user_id       bigint            null     comment '등록자',
    created_user_type     varchar(20)       null     comment '등록자유형',
    created_datetime      datetime          not null comment '등록일시',
    modified_user_id      bigint            null     comment '수정자',
    modified_user_type    varchar(20)       null     comment '수정자유형',
    modified_datetime     datetime          not null comment '수정일시',
    version               bigint            not null comment 'Data Version'
);
