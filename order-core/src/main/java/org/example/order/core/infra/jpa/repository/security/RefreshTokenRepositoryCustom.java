package org.example.order.core.infra.jpa.repository.security;

import org.example.order.core.application.security.entity.RefreshTokenEntity;

import java.util.List;

/**
 * 커스텀 RefreshTokenRepository 기능
 */
public interface RefreshTokenRepositoryCustom {

    /**
     * 만료된 토큰 전체 조회 (클린업용)
     */
    List<RefreshTokenEntity> findExpiredTokens();

    /**
     * 특정 유저의 모든 토큰 정보 조회 (향후 확장용)
     */
    List<RefreshTokenEntity> findAllTokensByUserId(String userId);
}
