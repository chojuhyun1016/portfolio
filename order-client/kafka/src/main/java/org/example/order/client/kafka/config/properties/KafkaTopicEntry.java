package org.example.order.client.kafka.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.example.order.common.core.code.type.RegionCode;
import org.example.order.core.messaging.order.code.MessageCategory;

@Getter
@Setter
public class KafkaTopicEntry {
    private MessageCategory category;
    private RegionCode regionCode;
    private String name;
}
