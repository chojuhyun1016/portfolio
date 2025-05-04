package org.example.order.core.infra.jpa.repository.security;

import org.example.order.core.domain.security.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * RefreshToken JPA Repository
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String>, RefreshTokenRepositoryCustom {

    Optional<RefreshTokenEntity> findByUserId(String userId);

    void deleteByUserId(String userId);
}
