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
 * - 편의를 위해 Enum 오버로드(getName(Enum), getName(Enum, RegionCode))도 제공한다.
 */
@Getter
@Setter
@ConfigurationProperties("kafka")
public class KafkaTopicProperties {

    private List<KafkaTopicEntry> topic;

    /* ========= String 기반 ========= */

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

    /* ========= Enum 편의 오버로드 (인프라 독립성 유지) ========= */

    /**
     * Enum.name()을 사용해 카테고리 검색
     */
    public String getName(Enum<?> category) {
        return getName(category == null ? null : category.name());
    }

    /**
     * Enum.name() + RegionCode 조합으로 검색
     */
    public String getName(Enum<?> category, RegionCode regionCode) {
        return getName(category == null ? null : category.name(), regionCode);
    }

    private static String nvl(String v) {
        return (v == null ? "" : v.trim());
    }
}
