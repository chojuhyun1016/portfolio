package org.example.order.client.kafka.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.example.order.common.core.code.type.RegionCode;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.core.messaging.order.code.MessageCategory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties("kafka")
public class KafkaTopicProperties {
    private List<KafkaTopicEntry> topic;

    public String getName(MessageCategory category) {
        return this.topic.stream()
                .filter(item -> item.getCategory().equals(category))
                .findFirst()
                .orElseThrow(() -> new CommonException(CommonExceptionCode.UNKNOWN_SEVER_ERROR))
                .getName();
    }

    public String getName(MessageCategory category, RegionCode regionCode) {
        return this.topic.stream()
                .filter(item -> item.getRegionCode() != null)
                .filter(item -> item.getRegionCode().equals(regionCode) && item.getCategory().equals(category))
                .findFirst()
                .orElseThrow(() -> new CommonException(CommonExceptionCode.UNKNOWN_SEVER_ERROR))
                .getName();
    }
}
