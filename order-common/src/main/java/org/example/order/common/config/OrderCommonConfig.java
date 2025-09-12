package org.example.order.common.config;

import org.springframework.context.annotation.Configuration;

/**
 * OrderCommonConfig (no-op, for B안 유지용)
 * ------------------------------------------------------------------------
 * - B안(정석 오토컨피그)에서는 실제 자동 구성은
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 * 에 등록된 AutoConfiguration 들이 담당합니다.
 * - 본 클래스는 레거시 호환을 위해 남겨두는 빈(no-op) 구성 클래스입니다.
 * - @EnableAutoConfiguration, @ComponentScan 등을 사용하지 않으므로
 * 과도한 스캔/중복 빈 등록이 발생하지 않습니다.
 * <p>
 * 사용 지침:
 * - 새 코드에서는 이 클래스를 @Import 하지 마세요.
 * - 반드시 오토컨피그 경유로 빈이 주입되도록 유지하세요.
 */
@Deprecated(forRemoval = false, since = "1.0")
@Configuration(proxyBeanMethods = false)
public class OrderCommonConfig {
}
