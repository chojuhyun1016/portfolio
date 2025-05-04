package org.example.order.core.infra.jpa.repository.security.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.order.core.domain.security.entity.RefreshTokenEntity;
import org.example.order.core.infra.jpa.repository.security.RefreshTokenRepositoryCustom;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RefreshToken 커스텀 쿼리 구현체
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QRefreshTokenEntity REFRESH_TOKEN = QRefreshTokenEntity.refreshTokenEntity;

    @Override
    public List<RefreshTokenEntity> findExpiredTokens() {
        return queryFactory
                .selectFrom(REFRESH_TOKEN)
                .where(REFRESH_TOKEN.expiryDatetime.before(LocalDateTime.now()))
                .fetch();
    }

    @Override
    public List<RefreshTokenEntity> findAllTokensByUserId(String userId) {
        return queryFactory
                .selectFrom(REFRESH_TOKEN)
                .where(REFRESH_TOKEN.userId.eq(userId))
                .fetch();
    }
}
