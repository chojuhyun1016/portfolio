package org.example.order.worker.kafka.support;

import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * EmbeddedKafka 베이스
 * - 동적 포트 브로커 실행
 * - bootstrap-servers 프로퍼티를 런타임에 주입
 */
@EmbeddedKafka(
        topics = {"ORDER_LOCAL"},
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
    }
}
