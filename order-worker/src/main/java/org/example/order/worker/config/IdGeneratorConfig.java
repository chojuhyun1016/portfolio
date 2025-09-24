package org.example.order.worker.config;

import com.github.f4b6a3.tsid.TsidFactory;
import org.example.order.core.infra.common.idgen.tsid.config.TsidInfraConfig;
import org.example.order.domain.common.id.IdGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneId;

/**
 * IdGenerator 구성 (Worker)
 * ------------------------------------------------------------------------
 * 목적
 * - tsid.enabled=true 인 경우 core 모듈의 TSID 구성(@TsidInfraConfig)을 그대로 사용
 * - 그래도 IdGenerator 빈이 없으면(프로퍼티 누락 등) 안전한 폴백 빈을 등록
 * <p>
 * 동작
 * - @Import(TsidInfraConfig): tsid.enabled=true 일 때 TsidFactory/IdGenerator 빈이 등록됨
 * - @ConditionalOnMissingBean(IdGenerator.class): 위가 없을 때만 로컬 TsidFactory로 폴백
 */
@Configuration
@Import(TsidInfraConfig.class)
public class IdGeneratorConfig {

    /**
     * 폴백 IdGenerator
     * - tsid.enabled=false 이거나 TSID 설정이 누락된 환경에서 사용
     * - nodeBits 기본(10), 시스템 타임존, SecureRandom 기반
     */
    @Bean
    @ConditionalOnMissingBean(IdGenerator.class)
    public IdGenerator fallbackIdGenerator() {
        TsidFactory factory = TsidFactory.builder()
                .withNodeBits(10)
                .withRandom(new SecureRandom())
                .withClock(Clock.system(ZoneId.systemDefault()))
                .build();

        return () -> factory.create().toLong();
    }
}
