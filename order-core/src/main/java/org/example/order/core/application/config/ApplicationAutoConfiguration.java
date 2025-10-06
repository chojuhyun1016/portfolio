package org.example.order.core.application.config;

import org.example.order.core.application.order.mapper.config.OrderMapperConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * ApplicationAutoConfiguration
 * ------------------------------------------------------------------------
 * 목적
 * - 애플리케이션 레이어 전반의 Auto-Config 집합 지점.
 * - 각 서비스별 구성(Order, ...)을 한 번에 등록한다.
 * <p>
 * 확장 방법
 * - 새로운 서비스가 생기면 해당 서비스 전용 Config를 만들고
 * 아래 @Import 리스트에 추가한다.
 * 예) PaymentMapperConfig.class (※ 요청에 따라 여긴 추가하지 않음)
 */
@AutoConfiguration
@Import({
        OrderMapperConfig.class
})
public class ApplicationAutoConfiguration {
}
