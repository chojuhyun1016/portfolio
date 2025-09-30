package org.example.order.core.infra.common.idgen.tsid.config.fallback;

import com.github.f4b6a3.tsid.TsidFactory;
import org.example.order.domain.common.id.IdGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneOffset;

/**
 * IdGenerator 폴백 구성
 * ------------------------------------------------------------------
 * 목적
 * - tsid.enabled=false 또는 미설정 등으로 TsidConfig가 동작하지 않아도
 * 애플리케이션이 깨지지 않도록 안전한 IdGenerator를 제공한다.
 * <p>
 * 동작
 * - @ConditionalOnMissingBean(IdGenerator) : 이미 등록돼 있으면 건너뜀
 * - UTC Clock + SecureRandom + nodeBits=10 기반 TSID 생성기
 */
@Configuration
public class FallbackIdGeneratorConfig {

    @Bean
    @ConditionalOnMissingBean(IdGenerator.class)
    public IdGenerator fallbackIdGenerator() {
        TsidFactory factory = TsidFactory.builder()
                .withNodeBits(10)
                .withRandom(new SecureRandom())
                .withClock(Clock.system(ZoneOffset.UTC))
                .build();

        // factory.create().toLong() 형태로 Long 생성
        return () -> factory.create().toLong();
    }
}
