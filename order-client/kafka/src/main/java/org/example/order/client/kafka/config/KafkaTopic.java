package org.example.order.client.kafka.config;

import lombok.Getter;
import lombok.Setter;
import org.example.order.common.code.MessageCategory;

@Getter
@Setter
public class KafkaTopic {
    private MessageCategory category;
    private String name;
}
