package org.example.order.core.application.order.mapper.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Order 매퍼 전용 스캔 구성
 * ------------------------------------------------------------------------
 * 목적
 * - OrderMapper 및 해당 패키지 내 MapStruct 구현체(@Mapper) 자동 등록.
 * - 외부 모듈(예: worker, api 등)에서 직접 패키지를 스캔하지 않아도 됨.
 * - 상위 오토컨피그(ApplicationAutoConfiguration)에서 통합 임포트됨.
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = "org.example.order.core.application.order.mapper")
public class OrderMapperConfig {
}
