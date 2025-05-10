package org.example.order.client.kafka.config.property;

import lombok.Getter;
import lombok.Setter;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka Topic 설정을 읽어들이는 프로퍼티 클래스.
 *
 * - 기존: KafkaTopic 객체 리스트 → 개선: 단순 Map<String, String>
 * - 예: kafka.topic.order = order-topic
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaTopicProperties {

    /**
     * 토픽 설정 Map (key: 카테고리, value: 토픽명)
     */
    private Map<String, String> topic;

    /**
     * 카테고리 키를 통해 토픽명을 반환.
     *
     * @param categoryKey 예: "order", "payment"
     * @return 매칭된 Kafka 토픽 이름
     */
    public String getName(String categoryKey) {
        String topicName = topic.get(categoryKey);

        if (topicName == null) {
            throw new CommonException(CommonExceptionCode.UNKNOWN_SEVER_ERROR,
                    "Kafka topic not found for category: " + categoryKey);
        }

        return topicName;
    }
}
