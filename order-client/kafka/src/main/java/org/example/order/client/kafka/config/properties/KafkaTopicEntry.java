package org.example.order.client.kafka.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.example.order.common.core.code.type.RegionCode;

/**
 * Kafka 토픽 엔트리 (인프라 모듈 독립 유지)
 * - category: 문자열 키 (예: "ORDER_LOCAL", "ORDER_API" ...)
 * - regionCode: 선택(지역 구분 필요시)
 * - name: 실제 토픽명
 * <p>
 * 계약/도메인 enum에 절대 의존하지 않는다.
 */
@Getter
@Setter
public class KafkaTopicEntry {

    /**
     * 메시지 카테고리(문자열)
     * - 예) "ORDER_LOCAL", "PAYMENT_COMPLETED"
     * - 각 서비스 모듈에서 enum↔string 매핑을 담당한다.
     */
    private String category;

    /**
     * 지역 코드 (예: KR, US, JP 등) — 선택적
     */
    private RegionCode regionCode;

    /**
     * 실제 카프카 토픽 이름
     */
    private String name;
}
