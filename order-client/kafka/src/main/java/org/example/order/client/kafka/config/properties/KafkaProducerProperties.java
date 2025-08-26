package org.example.order.client.kafka.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * 큰 맥락
 * - Kafka Producer 관련 설정을 외부 프로퍼티(application.yml 등)에서 주입받는 전용 클래스.
 * - kafka.producer.enabled 플래그로 프로듀서 모듈의 on/off를 제어한다.
 * - enabled=true 인 경우 bootstrapServers 값은 필수이며(@NotBlank),
 * ProducerFactory, KafkaTemplate 등의 빈 생성 시 해당 서버 주소로 연결된다.
 */
@Getter
@Setter
@ConfigurationProperties("kafka.producer")
@Validated
public class KafkaProducerProperties {

    /**
     * 프로듀서 전체 활성/비활성 스위치
     */
    private boolean enabled = false;

    /**
     * producer.enabled=true 일 때 반드시 지정해야 하는 bootstrap 서버
     */
    @NotBlank(message = "kafka.producer.bootstrap-servers must not be blank when producer is enabled")
    private String bootstrapServers;
}
