package org.example.order.core.infra;

import org.example.order.core.TestBootApp;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * test-unit 프로필에서 ApplicationContext 가 정상 로딩되는지 확인하는 스모크 테스트
 * - JPA/JDBC 자동구성은 배제되어 있고
 * - 더미 EMF/EM/SharedEM + 더미 JDBC 가 제공되므로
 * - 어떤 @Import 경로로 레이어가 들어와도 컨텍스트 로딩은 실패하지 않아야 한다.
 */
@SpringBootTest(
        classes = TestBootApp.class,
        // ✅ 방법 1: 속성으로 Redisson V2 오토컨피그만 정확히 제외
        properties = "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2"
)
@ActiveProfiles("test-unit")
// ✅ 방법 2(대안): 애너테이션으로 제외 — 위 properties와 둘 중 하나만 사용하세요.
// @EnableAutoConfiguration(exclude = org.redisson.spring.starter.RedissonAutoConfigurationV2.class)
class ContextSmokeTest {

    @Test
    void contextLoads() {
        // 성공 시 아무 일도 일어나지 않는다 (예외만 안나면 성공)
    }
}
