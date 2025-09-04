package org.example.order.api.master;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * 통합 테스트 전용 부트스트랩
 * - 최소 자동설정만 적용
 * - Redis/Redisson 제외는 각 테스트 클래스의 @TestPropertySource에서 처리
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class IntegrationBoot {
}
