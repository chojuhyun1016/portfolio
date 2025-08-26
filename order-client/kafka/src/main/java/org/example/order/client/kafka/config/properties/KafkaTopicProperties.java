package org.example.order.client.kafka.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.example.order.common.core.code.type.RegionCode;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.core.messaging.order.code.MessageCategory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 큰 맥락
 * - Kafka 토픽 설정을 관리하는 Properties 클래스.
 * - application.yml 에 정의된 `kafka.topic` 리스트를 바인딩한다.
 * - MessageCategory, RegionCode 조합으로 적절한 토픽 이름을 반환하는 메서드 제공.
 * - 매칭되는 토픽이 없으면 CommonException(UNKNOWN_SERVER_ERROR) 발생시켜 fail-fast.
 */
@Getter
@Setter
@ConfigurationProperties("kafka")
public class KafkaTopicProperties {

    /**
     * 카프카 토픽 엔트리 리스트
     */
    private List<KafkaTopicEntry> topic;

    /**
     * 카테고리 기준으로 토픽명 조회
     *
     * @param category 메시지 카테고리
     * @return 토픽 이름
     */
    public String getName(MessageCategory category) {
        return this.topic.stream()
                .filter(item -> item.getCategory().equals(category))
                .findFirst()
                .orElseThrow(() -> new CommonException(CommonExceptionCode.UNKNOWN_SEVER_ERROR))
                .getName();
    }

    /**
     * 카테고리 + 지역 코드 기준으로 토픽명 조회
     *
     * @param category   메시지 카테고리
     * @param regionCode 지역 코드
     * @return 토픽 이름
     */
    public String getName(MessageCategory category, RegionCode regionCode) {
        return this.topic.stream()
                .filter(item -> item.getRegionCode() != null)
                .filter(item -> item.getRegionCode().equals(regionCode) && item.getCategory().equals(category))
                .findFirst()
                .orElseThrow(() -> new CommonException(CommonExceptionCode.UNKNOWN_SEVER_ERROR))
                .getName();
    }
}
