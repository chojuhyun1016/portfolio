package org.example.order.client.kafka.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KafkaTopic {
    private MessageCategory category;
    private String name;
}
