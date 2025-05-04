create table `refresh_token`
(
    user_id             varchar(100)    not null comment '사용자 ID (PK)' primary key,
    token               varchar(512)    not null comment 'Refresh 토큰',
    expiry_datetime     datetime        not null comment 'Refresh 토큰 만료 시각',
    created_at          datetime        not null comment '토큰 생성 시각'
) comment='RefreshToken 저장소';
