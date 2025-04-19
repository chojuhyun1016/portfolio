package org.example.order.core.infra.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * AOP 설정 클래스
 * - AspectJ 기반 AOP 기능을 활성화
 * - proxyTargetClass = true → CGLIB 기반 프록시 생성 (인터페이스 없어도 동작)
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AopConfig {
}
