package org.example.order.batch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.order.contract.order.messaging.dlq.DeadLetter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * KafkaDeadLetterConsumerConfig
 * ------------------------------------------------------------------------
 * 목적
 * - DLQ(Dead Letter Queue) 전용 ConsumerFactory 구성.
 * - Kafka 레코드 value(JSON)를 컨슈머 레이어에서 바로 DeadLetter<?> POJO로 역직렬화.
 * <p>
 * 특징
 * - 기존 ConsumerFactory<String, String>은 그대로 유지 -> 다른 소비자 경로 영향 없음.
 * - DLQ 경로만 별도의 @Qualifier("deadLetterConsumerFactory")로 분리.
 * - order-worker와 동일 철학: "엣지"에서 JSON -> POJO 처리.
 * <p>
 * 보안/운영 권장사항
 * - 로컬/테스트: addTrustedPackages("*") 허용(개발 편의).
 * - 운영: 실제 계약 패키지로 화이트리스트 제한(예: org.example.order.contract.*).
 */
@Configuration
public class KafkaDeadLetterConsumerConfig {

    private final ObjectMapper objectMapper;
    private final KafkaProperties kafkaProperties;

    /**
     * 계약 패키지 상수
     * - 운영 환경에서 JsonDeserializer.addTrustedPackages(...) 에 사용
     * - 변경 시 여기를 수정하면 모든 DLQ 컨슈머에 일괄 반영
     */
    private static final String CONTRACT_PACKAGE_PREFIX = "org.example.order.contract.*";

    public KafkaDeadLetterConsumerConfig(ObjectMapper objectMapper, KafkaProperties kafkaProperties) {
        this.objectMapper = objectMapper;
        this.kafkaProperties = kafkaProperties;
    }

    /**
     * DLQ 전용 ConsumerFactory
     * - key: String
     * - value: DeadLetter<?>
     * - JsonDeserializer에 trustedPackages/ignoreTypeHeaders 설정으로 헤더 타입정보 없이도 역직렬화 가능
     */
    @Bean
    @Qualifier("deadLetterConsumerFactory")
    public ConsumerFactory<String, DeadLetter<?>> deadLetterConsumerFactory() {

        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties(null));

        StringDeserializer keyDeserializer = new StringDeserializer();
        JsonDeserializer<DeadLetter<?>> valueDeserializer =
                new JsonDeserializer<>(DeadLetter.class, objectMapper);

        valueDeserializer.ignoreTypeHeaders();
        valueDeserializer.setUseTypeMapperForKey(false);

        String activeProfile = System.getProperty("spring.profiles.active", "local");

        if ("local".equals(activeProfile) || "test".equals(activeProfile)) {
            valueDeserializer.addTrustedPackages("*");
        } else {
            valueDeserializer.addTrustedPackages(CONTRACT_PACKAGE_PREFIX);
        }

        return new DefaultKafkaConsumerFactory<>(props, keyDeserializer, valueDeserializer);
    }
}
