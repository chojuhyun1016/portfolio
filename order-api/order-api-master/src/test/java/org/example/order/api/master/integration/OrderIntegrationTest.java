package org.example.order.api.master.integration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.order.common.core.messaging.code.MessageMethodType;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 빠른 통합 테스트: Embedded Kafka + H2
 * - 시크릿 관련 빈은 @MockBean 으로 대체하여 초기화 예외 방지
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"order.local"})
@TestPropertySource(properties = {
        // embedded-kafka 사용
        "kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",

        // 토픽 바인딩
        "kafka.topic[0].category=ORDER_LOCAL",
        "kafka.topic[0].name=order.local",

        // 운영용 구성 차단
        "kafka.ssl.enabled=false",
        "order.api.infra.security.enabled=false",
        "order.api.infra.logging.enabled=false",
        "order.api.infra.web.enabled=false",
        "order.api.infra.format.enabled=false",

        // 방어적 플래그
        "order.core.secrets.enabled=false",
        "order.crypto.enabled=false"
})
class OrderIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Value("${spring.embedded.kafka.brokers}")
    String embeddedKafkaBrokers;

    // ✅ 시크릿 리졸버를 목으로 교체 (실제 구현 초기화 방지)
    @MockBean
    SecretsKeyResolver secretsKeyResolver;

    @BeforeEach
    void stubSecrets() {
        // 호출되는 어떤 키 이름에 대해서도 유효 길이의 더미 키 반환
        byte[] any32 = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8); // 32 bytes
        when(secretsKeyResolver.getCurrentKey(ArgumentMatchers.anyString()))
                .thenReturn(any32);
    }

    @DisplayName("[IT][EmbeddedKafka] POST /order → 202, Kafka 발행 확인")
    @Test
    void sendOrder_and_publishToKafka() throws Exception {
        long orderId = 1001L;
        String payloadJson = """
            { "orderId": %d, "methodType": "%s" }
            """.formatted(orderId, MessageMethodType.values()[0].name());

        String topic = "order.local";
        String groupId = "it-" + UUID.randomUUID();

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBrokers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(java.util.List.of(topic));

            mockMvc.perform(
                            post("/order")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(payloadJson)
                    )
                    .andExpect(status().isAccepted())
                    .andDo(document(
                            "order-send-it",
                            requestFields(
                                    fieldWithPath("orderId").description("주문 ID"),
                                    fieldWithPath("methodType").description("메서드 타입 enum")
                            ),
                            relaxedResponseFields(
                                    beneathPath("data").withSubsectionId("data"),
                                    fieldWithPath("orderId").description("요청된 주문 ID"),
                                    fieldWithPath("status").description("처리 상태")
                            )
                    ));

            var records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records.count()).isGreaterThan(0);
        }
    }
}
