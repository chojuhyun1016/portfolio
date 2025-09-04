package org.example.order.api.master;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * 테스트 전용 부트스트랩
 * - @SpringBootTest 등에서 경량 컨텍스트 로딩 시 사용
 * - 외부 인프라 자동설정 필요 시 프로젝트 정책에 맞게 제외 추가
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestBoot {
}
