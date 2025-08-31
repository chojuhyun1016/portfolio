package org.example.order.core.infra.config;

import org.example.order.core.infra.common.idgen.tsid.config.TsidInfraConfig;
import org.example.order.core.infra.common.secrets.config.SecretsInfraConfig;
import org.example.order.core.infra.crypto.config.CryptoInfraConfig;
import org.example.order.core.infra.dynamo.config.DynamoInfraConfig;
import org.example.order.core.infra.jpa.config.JpaInfraConfig;
import org.example.order.core.infra.lock.config.LockInfraConfig;
import org.example.order.core.infra.redis.config.RedisInfraConfig;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 중앙 통합 설정 (ALL-IN-ONE Import)
 * <p>
 * - 모든 인프라 설정을 한 번에 @Import.
 * - 각 설정은 내부 @ConditionalOnProperty로 보호 → yml의 enabled=false면 Bean 미생성.
 * - 다른 모듈/앱/테스트에서는 이 클래스 하나만 Import 하면 됨.
 */
@Configuration
@ComponentScan(basePackages = {
        "org.example.order.core",
        "org.example.order.common"
})
@EntityScan(basePackages = {
        "org.example.order.domain",
        "org.example.order.core.domain"
})
@EnableJpaRepositories(basePackages = {
        "org.example.order.core.infra.persistence.order.jpa.adapter"
})
@Import({
        // ====== TSID ======
        TsidInfraConfig.class,

        // ====== AWS Secrets Manager (Client, Scheduler) ======
        SecretsInfraConfig.class,

        // ====== Crypto ======
        CryptoInfraConfig.class,

        // ====== DynamoDB ======
        DynamoInfraConfig.class,

        // ====== JPA / Querydsl ======
        JpaInfraConfig.class,

        // ====== 분산락 (Named Lock, Redisson Lock) ======
        LockInfraConfig.class,

        // ====== Redis (Lettuce 등) ======
        RedisInfraConfig.class
})
public class OrderCoreConfig {
}
