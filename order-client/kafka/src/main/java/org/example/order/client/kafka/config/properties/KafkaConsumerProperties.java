package org.example.order.client.kafka.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Consumer 설정
 * - enabled 플래그 추가: kafka.consumer.enabled=true 일 때만 Consumer 관련 빈 생성
 * - enabled=true 인 경우 bootstrapServers 등 필수값 유효성 검증 가능(선택)
 */
@Getter
@Setter
@ConfigurationProperties("kafka.consumer")
@Validated
public class KafkaConsumerProperties {

    /** ✨ 신규: 컨슈머 전체 on/off */
    private boolean enabled = false;

    /** 컨슈머가 enable=true일 때 필수 (선택: @NotBlank로 fail-fast) */
    @NotBlank(message = "kafka.consumer.bootstrap-servers must not be blank when consumer is enabled")
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
