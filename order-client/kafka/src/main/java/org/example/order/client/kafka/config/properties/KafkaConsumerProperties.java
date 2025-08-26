package org.example.order.client.kafka.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * 큰 맥락
 * - Kafka Consumer 관련 설정을 외부 프로퍼티(application.yml 등)로부터 주입받는 전용 클래스.
 * - kafka.consumer.enabled 플래그로 컨슈머 모듈 on/off를 제어한다.
 * - enabled=true 일 때 bootstrapServers는 반드시 지정되어야 하며(@NotBlank).
 * - 하위 option 클래스에는 poll 관련 상세 설정(maxPollRecords 등)을 담아
 *   컨테이너 팩토리에서 활용할 수 있다.
 */
@Getter
@Setter
@ConfigurationProperties("kafka.consumer")
@Validated
public class KafkaConsumerProperties {

    /** 컨슈머 전체 활성/비활성 스위치 */
    private boolean enabled = false;

    /** consumer.enabled=true 일 때 반드시 지정해야 하는 bootstrap 서버 */
    @NotBlank(message = "kafka.consumer.bootstrap-servers must not be blank when consumer is enabled")
    private String bootstrapServers;

    /** poll, commit 등 상세 옵션 */
    private KafkaConsumerOption option;

    @Getter
    @Setter
    public static class KafkaConsumerOption {
        /** 컨슈머 최대 실패 허용 횟수 */
        private Integer maxFailCount;

        /** 한 번의 poll() 호출에서 가져올 최대 레코드 수 */
        private Integer maxPollRecords;

        /** fetch 사이즈를 채우지 못했을 때 대기할 최대 시간(ms) */
        private Integer fetchMaxWaitMs;

        /** 한 번의 fetch에서 가져올 최대 바이트 크기 */
        private Integer fetchMaxBytes;

        /** poll 호출 사이의 최대 대기 시간(ms) — 초과 시 리밸런싱 */
        private Integer maxPollIntervalMs;

        /** 두 poll 호출 사이의 휴지 시간(ms) */
        private Integer idleBetweenPolls;

        /** offset 리셋 정책 (earliest/latest 등) */
        private String autoOffsetReset;

        /** offset 자동 커밋 여부 */
        private Boolean enableAutoCommit;
    }
}
