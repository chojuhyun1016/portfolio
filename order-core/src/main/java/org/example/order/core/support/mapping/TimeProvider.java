package org.example.order.core.support.mapping;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 시간/시계 주입 헬퍼
 * - 테스트 시 Clock 고정/주입으로 재현성 보장
 */
public class TimeProvider {
    private final Clock clock;

    public TimeProvider() {
        this(Clock.systemDefaultZone());
    }

    public TimeProvider(Clock clock) {
        this.clock = clock;
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
