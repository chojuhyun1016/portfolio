package org.example.order.client.kafka.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Producer 설정
 * - enabled 플래그 추가: kafka.producer.enabled=true 일 때만 Producer 관련 빈 생성
 */
@Getter
@Setter
@ConfigurationProperties("kafka.producer")
@Validated
public class KafkaProducerProperties {

    /** ✨ 신규: 프로듀서 전체 on/off */
    private boolean enabled = false;

    /** 프로듀서가 enable=true일 때 필수 */
    @NotBlank(message = "kafka.producer.bootstrap-servers must not be blank when producer is enabled")
    private String bootstrapServers;
}
