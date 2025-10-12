package org.example.order.client.kafka.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.example.order.common.core.code.type.RegionCode;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Kafka 토픽 설정 바인딩
 * - application.yml 의 kafka.topic 리스트를 문자열 category 기반으로 조회한다.
 * - 인프라 모듈의 독립성을 위해 외부 계약 enum에 의존하지 않는다.
 */
@Getter
@Setter
@ConfigurationProperties("kafka")
public class KafkaTopicProperties {

    private List<KafkaTopicEntry> topic;

    /**
     * 카테고리 기준 토픽명 조회 (대소문자 무시)
     */
    public String getName(String category) {
        String key = nvl(category);
        return this.topic.stream()
                .filter(item -> nvl(item.getCategory()).equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new CommonException(CommonExceptionCode.UNKNOWN_SERVER_ERROR,
                        "Kafka topic not found for category=" + category))
                .getName();
    }

    /**
     * 카테고리 + 지역 기준 토픽명 조회 (대소문자 무시)
     */
    public String getName(String category, RegionCode regionCode) {
        String key = nvl(category);
        return this.topic.stream()
                .filter(item -> item.getRegionCode() != null)
                .filter(item -> item.getRegionCode().equals(regionCode)
                        && nvl(item.getCategory()).equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new CommonException(CommonExceptionCode.UNKNOWN_SERVER_ERROR,
                        "Kafka topic not found for category=" + category + ", region=" + regionCode))
                .getName();
    }

    private static String nvl(String v) {
        return (v == null ? "" : v.trim());
    }
}
