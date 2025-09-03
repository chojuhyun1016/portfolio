//package org.example.order.api.master.integration;
//
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.clients.consumer.KafkaConsumer;
//import org.apache.kafka.common.serialization.StringDeserializer;
//import org.example.order.common.core.messaging.code.MessageMethodType;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Tag;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.time.Duration;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
//import static org.springframework.restdocs.payload.PayloadDocumentation.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * 빠른 통합 테스트: Embedded Kafka + H2
// * - 외부 Docker 불필요
// * - Kafka 토픽으로 발행되는지 확인
// */
//@Tag("integration")
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureMockMvc
//@AutoConfigureRestDocs
//@ActiveProfiles("test")
//@EmbeddedKafka(partitions = 1, topics = {"order.local"})
//@TestPropertySource(properties = {
//        // embedded-kafka로 교체
//        "kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
//        "kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
//
//        // 토픽 바인딩
//        "kafka.topic[0].category=ORDER_LOCAL",
//        "kafka.topic[0].name=order.local",
//
//        // 운영/보조 자동구성 차단 (필요 최소한만 유지)
//        "kafka.ssl.enabled=false",
//        "order.api.infra.security.enabled=false",
//        "order.api.infra.logging.enabled=false",
//        "order.api.infra.web.enabled=false",
//        "order.api.infra.format.enabled=false"
//})
//class OrderIntegrationTest {
//
//    @Autowired
//    MockMvc mockMvc;
//
//    @Value("${spring.embedded.kafka.brokers}")
//    String embeddedKafkaBrokers;
//
//    @DisplayName("[IT][EmbeddedKafka] POST /order → 202, Kafka 발행 확인")
//    @Test
//    void sendOrder_and_publishToKafka() throws Exception {
//        long orderId = 1001L;
//        String payloadJson = """
//                { "orderId": %d, "methodType": "%s" }
//                """.formatted(orderId, MessageMethodType.values()[0].name());
//
//        String topic = "order.local";
//        String groupId = "it-" + UUID.randomUUID();
//
//        Map<String, Object> consumerProps = new HashMap<>();
//        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBrokers);
//        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
//        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
//        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//
//        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
//            consumer.subscribe(java.util.List.of(topic));
//
//            mockMvc.perform(
//                            post("/order")
//                                    .contentType(MediaType.APPLICATION_JSON)
//                                    .accept(MediaType.APPLICATION_JSON)
//                                    .content(payloadJson)
//                    )
//                    .andExpect(status().isAccepted())
//                    .andDo(document(
//                            "order-send-it",
//                            requestFields(
//                                    fieldWithPath("orderId").description("주문 ID"),
//                                    fieldWithPath("methodType").description("메서드 타입 enum")
//                            ),
//                            relaxedResponseFields(
//                                    beneathPath("data").withSubsectionId("data"),
//                                    fieldWithPath("orderId").description("요청된 주문 ID"),
//                                    fieldWithPath("status").description("처리 상태")
//                            )
//                    ));
//
//            var records = consumer.poll(Duration.ofSeconds(10));
//            assertThat(records.count()).isGreaterThan(0);
//        }
//    }
//}
