package org.example.order.worker;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.config.OrderCoreConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * (선택) 컨텍스트 로딩이 필요한 별도 테스트에서 재사용할 수 있는 Boot 스켈레톤.
 * - 실제 테스트 메서드는 없음(자동 실행 방지).
 */
@Slf4j
@EnableAutoConfiguration
@SpringBootTest(classes = {OrderCoreConfig.class})
@ActiveProfiles("local")
public class TestBoot {
}
