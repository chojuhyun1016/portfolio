package org.example.order.core.infra.jpa.config;

import com.github.f4b6a3.tsid.TsidFactory;
import org.example.order.core.infra.config.OrderCoreConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * 통합 테스트 공통 컨텍스트 구성 (선택 사용)
 * - OrderCoreConfig + JpaInfraConfig(하위 조립 포함)
 * - 테스트용 TsidFactory 제공 (중복 회피 조건 포함)
 * - 🔐 jpa-it 프로필에서만 활성화
 */
@Profile("jpa-it")
@TestConfiguration
@Import({
        OrderCoreConfig.class,
        JpaInfraConfig.class
})
public class TestJpaBootConfig {

    @Bean
    @ConditionalOnMissingBean(TsidFactory.class)
    public TsidFactory tsidFactory() {
        return TsidFactory.newInstance1024();
    }
}
