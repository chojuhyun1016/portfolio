package org.example.order.core.domain.security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refresh_token")
public class RefreshTokenEntity {

    @Id
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;  // 사용자 ID (PK)

    @Column(name = "token", nullable = false, length = 512)
    private String token;

    @Column(name = "expiry_datetime", nullable = false)
    private LocalDateTime expiryDatetime;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RefreshTokenEntity(String userId, String token, LocalDateTime expiryDatetime) {
        this.userId = userId;
        this.token = token;
        this.expiryDatetime = expiryDatetime;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiryDatetime.isBefore(LocalDateTime.now());
    }
}
