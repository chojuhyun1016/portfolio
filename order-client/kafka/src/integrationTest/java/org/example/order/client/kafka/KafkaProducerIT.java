package org.example.order.client.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.order.client.kafka.config.KafkaModuleConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 통합 테스트 개요
 * - EmbeddedKafka로 브로커 기동
 * - KafkaTemplate로 전송하고, 로우 KafkaConsumer로 수신 검증
 * - 프로덕션의 JsonSerializer 특성 반영(문자열이 JSON 문자열로 실전송될 수 있음)
 * - IDE 자동주입 경고는 스프링 확장 + 경고억제로 처리(테스트 컨텍스트에선 정상 주입)
 */
@SpringBootTest(
        classes = KafkaModuleConfig.class,
        properties = {
                "kafka.producer.enabled=true",
                "kafka.ssl.enabled=false"
        }
)
@EmbeddedKafka(partitions = 1, controlledShutdown = false, topics = {KafkaProducerIT.TOPIC})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(SpringExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
class KafkaProducerIT {

    static final String TOPIC = "it-kafka-topic";

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static String resolveBrokers() {
        String brokers = System.getProperty("spring.embedded.kafka.brokers");
        return (brokers == null || brokers.isBlank()) ? "127.0.0.1:9092" : brokers;
    }

    @DynamicPropertySource
    static void registerKafkaBootstrap(DynamicPropertyRegistry registry) {
        registry.add("kafka.producer.bootstrap-servers", KafkaProducerIT::resolveBrokers);
    }

    @Test
    @DisplayName("KafkaTemplate 전송 → Consumer 폴링 수신 검증")
    void sendAndConsume() {
        String key = "it-key";
        String value = "hello-kafka-" + UUID.randomUUID();
        String jsonEncodedValue = "\"" + value + "\"";

        kafkaTemplate.send(TOPIC, key, value).join();
        kafkaTemplate.flush();

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-consumer-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        boolean matched = false;

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));
            long deadline = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis();

            while (System.currentTimeMillis() < deadline && !matched) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, String> r : records.records(TOPIC)) {
                    String recKey = r.key();
                    String recVal = r.value();

                    if (key.equals(recKey) && (value.equals(recVal) || jsonEncodedValue.equals(recVal))) {
                        matched = true;
                        break;
                    }
                }
            }
        }

        assertTrue(matched, "Produced message must be consumed from topic: " + TOPIC);
    }
}
