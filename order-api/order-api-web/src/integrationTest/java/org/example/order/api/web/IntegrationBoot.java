package org.example.order.api.web;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * (선택) 통합 테스트 전용 부트스트랩
 * - @WebMvcTest 에서는 사용되지 않음
 * - 만약 @SpringBootTest 로 전부 띄워야 할 때만 사용
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class IntegrationBoot {
}
