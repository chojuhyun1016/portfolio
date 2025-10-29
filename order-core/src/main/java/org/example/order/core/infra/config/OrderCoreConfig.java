package org.example.order.core.infra.config;

import org.example.order.core.infra.common.idgen.tsid.config.TsidInfraConfig;
import org.example.order.core.infra.common.secrets.config.SecretsInfraConfig;
import org.example.order.core.infra.crypto.config.CryptoInfraConfig;
import org.example.order.core.infra.dynamo.config.DynamoInfraConfig;
import org.example.order.core.infra.jpa.config.JpaInfraConfig;
import org.example.order.core.infra.lock.config.LockInfraConfig;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * OrderCoreConfig
 * ------------------------------------------------------------------------
 * 목적
 * - 인프라 구성(TSID, Secrets, Crypto, Dynamo, JPA, Lock, Redis)을 한 곳에서 묶어 제공.
 * - 각 하위 구성은 @ConditionalOnProperty 로 보호되어 프로퍼티 기반 온/오프가 가능.
 * <p>
 * 설계 원칙
 * - 라이브러리 모듈은 @ComponentScan 을 하지 않는다 (스캔은 애플리케이션이 결정).
 * - @EntityScan 도 애플리케이션 루트가 org.example.order 인 경우 대개 불필요.
 * (필요 시 개별 애플리케이션에서 선언)
 */
@Configuration(proxyBeanMethods = false)
@EntityScan(basePackages = {
        // ===== order-domain entity =====
        "org.example.order.domain",
        // ===== infra-persistence converter =====
        "org.example.order.core.infra.jpa.converter"
})
@Import({
        // ===== TSID (ID Generator) =====
        TsidInfraConfig.class,

        // ===== AWS Secrets Manager =====
        SecretsInfraConfig.class,

        // ===== Crypto (암호화) =====
        CryptoInfraConfig.class,

        // ===== DynamoDB =====
        DynamoInfraConfig.class,

        // ===== JPA / Querydsl =====
        JpaInfraConfig.class,

        // ===== Distributed Lock (NamedLock / Redisson) =====
        LockInfraConfig.class
})
public class OrderCoreConfig {
}
