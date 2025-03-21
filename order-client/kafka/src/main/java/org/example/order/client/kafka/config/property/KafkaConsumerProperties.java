package org.example.order.client.kafka.config.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("kafka.consumer")
public class KafkaConsumerProperties {
    private String bootstrapServers;
    private KafkaConsumerOption option;

    @Getter
    @Setter
    public static class KafkaConsumerOption {
        private Integer maxFailCount;
        private Integer maxPollRecords;
        private Integer fetchMaxWaitMs;
        private Integer fetchMaxBytes;
        private Integer maxPollIntervalMs;
        private Integer idleBetweenPolls;
        private String autoOffsetReset;
        private Boolean enableAutoCommit;
    }
}
