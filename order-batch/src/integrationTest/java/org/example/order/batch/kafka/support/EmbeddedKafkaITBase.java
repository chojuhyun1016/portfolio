package org.example.order.batch.kafka.support;

import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Embedded Kafka 기본 베이스.
 * - 동적 포트 브로커 기동
 * - 테스트 런타임에 Kafka 관련 프로퍼티 주입
 */
@EmbeddedKafka(
        topics = {"ORDER_LOCAL", "ORDER_API", "ORDER_CRUD", "ORDER_DLQ", "ORDER_ALARM"},
        partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"}
)
public abstract class EmbeddedKafkaITBase {

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers",
                () -> System.getProperty("spring.embedded.kafka.brokers"));

        registry.add("spring.kafka.producer.key-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");

        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");

        registry.add("spring.kafka.consumer.key-deserializer",
                () -> "org.apache.kafka.common.serialization.StringDeserializer");

        registry.add("spring.kafka.consumer.value-deserializer",
                () -> "org.apache.kafka.common.serialization.StringDeserializer");
    }
}
