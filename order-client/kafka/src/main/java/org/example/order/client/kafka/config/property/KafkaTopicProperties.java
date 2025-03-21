package org.example.order.client.kafka.config.property;

import lombok.Getter;
import lombok.Setter;
import org.example.order.client.kafka.config.KafkaTopic;
import org.example.order.common.code.CommonExceptionCode;
import org.example.order.common.code.MessageCategory;
import org.example.order.common.exception.CommonException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties("kafka")
public class KafkaTopicProperties {
    private List<KafkaTopic> topic;

    public String getName(MessageCategory category) {
        return this.topic.stream()
                .filter(item -> item.getCategory().equals(category))
                .findFirst()
                .orElseThrow(() -> new CommonException(CommonExceptionCode.UNKNOWN_SEVER_ERROR))
                .getName();
    }
}
