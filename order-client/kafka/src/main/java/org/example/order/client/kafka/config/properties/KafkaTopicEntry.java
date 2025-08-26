package org.example.order.client.kafka.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.example.order.common.core.code.type.RegionCode;
import org.example.order.core.messaging.order.code.MessageCategory;

/**
 * 큰 맥락
 * - Kafka 토픽 엔트리를 표현하는 단순 DTO 클래스.
 * - 카테고리(MessageCategory), 지역 코드(RegionCode), 토픽명(name)을 조합하여
 * 프로듀서/컨슈머 설정에서 사용한다.
 * - 외부 설정(application.yml 등)에 리스트 형태로 바인딩되어, 지역/카테고리별 토픽 구성을 유연하게 할 수 있다.
 */
@Getter
@Setter
public class KafkaTopicEntry {

    /**
     * 메시지 카테고리 (예: ORDER_CREATED, PAYMENT_COMPLETED 등)
     */
    private MessageCategory category;

    /**
     * 지역 코드 (예: KR, US, JP 등)
     */
    private RegionCode regionCode;

    /**
     * 실제 카프카 토픽 이름
     */
    private String name;
}
