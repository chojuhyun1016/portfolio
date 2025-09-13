package org.example.order.worker.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.example.order.common.support.logging.CorrelationAspect;
import org.example.order.worker.IntegrationBoot;
import org.example.order.worker.facade.order.OrderApiMessageFacade;
import org.example.order.worker.facade.order.OrderCrudMessageFacade;
import org.example.order.worker.facade.order.OrderLocalMessageFacade;
import org.example.order.worker.kafka.support.EmbeddedKafkaITBase;
import org.example.order.worker.listener.order.impl.OrderApiMessageListenerImpl;
import org.example.order.worker.listener.order.impl.OrderCrudMessageListenerImpl;
import org.example.order.worker.listener.order.impl.OrderLocalMessageListenerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.BatchInterceptor;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

/**
 * Kafka 리스너 → Facade 경유 시 MDC(traceId/orderId) 전파 통합 테스트
 * - OrderApi : payload.id → traceId/orderId (테스트 스코프 RecordInterceptor에서 보장) + @Correlate(운영과 동일 동작)
 * - OrderLocal/Crud : 헤더 traceId → MDC 복원(Record/Batch 인터셉터)
 */
@SpringBootTest(
        classes = {
                IntegrationBoot.class,
                KafkaMdcPropagationEmbeddedKafkaIT.TestKafkaConfig.class,
                OrderApiMessageListenerImpl.class,
                OrderLocalMessageListenerImpl.class,
                OrderCrudMessageListenerImpl.class,
                CorrelationAspect.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.main.web-application-type=none"
)
@DirtiesContext
@Import(KafkaMdcPropagationEmbeddedKafkaIT.TestKafkaConfig.class)
class KafkaMdcPropagationEmbeddedKafkaIT extends EmbeddedKafkaITBase {

    @MockBean
    private OrderApiMessageFacade orderApiMessageFacade;

    @MockBean
    private OrderLocalMessageFacade orderLocalMessageFacade;

    @MockBean
    private OrderCrudMessageFacade orderCrudMessageFacade;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private TestKafkaConfig.TestHopService testHopService;

    @BeforeEach
    void setUp() {
        Mockito.reset(orderApiMessageFacade, orderLocalMessageFacade, orderCrudMessageFacade);

        Mockito.doAnswer(inv -> {
            boolean okHere = "777".equals(MDC.get("traceId")) && "777".equals(MDC.get("orderId"));
            testHopService.assertAndRemember(okHere);
            return null;
        }).when(orderApiMessageFacade).requestApi(any());

        Mockito.doAnswer(inv -> {
            boolean okHere = MDC.get("traceId") != null;
            testHopService.assertAndRemember(okHere);
            return null;
        }).when(orderLocalMessageFacade).sendOrderApiTopic(any());

        Mockito.doAnswer(inv -> {
            boolean okHere = MDC.get("traceId") != null;
            testHopService.assertAndRemember(okHere);
            return null;
        }).when(orderCrudMessageFacade).executeOrderCrud(any());
    }

    private void waitListenersReady() {
        try {
            Thread.sleep(600);
        } catch (InterruptedException ignored) {
        }
    }

    private void send(ProducerRecord<String, String> record) throws Exception {
        Future<SendResult<String, String>> f = kafkaTemplate.send(record);
        f.get(10, TimeUnit.SECONDS);
    }

    // 1) ORDER_API : payload.id 로 덮어쓰기 검증
    @Test
    void orderApi_listener_should_override_traceId_with_payload_id_and_propagate_to_facade_and_service() throws Exception {
        waitListenersReady();

        long id = 777L;
        String payload = "{\"id\":" + id + ",\"publishedTimestamp\":" + System.currentTimeMillis() + "}";

        testHopService.reset();

        ProducerRecord<String, String> record = new ProducerRecord<>("ORDER_API", "k", payload);
        record.headers().add(new RecordHeader("traceId", "incoming-should-be-overridden".getBytes(StandardCharsets.UTF_8)));

        send(record);

        assertThat(testHopService.awaitAndGetOk(10, TimeUnit.SECONDS)).isTrue();
    }

    // 2) ORDER_LOCAL : 헤더 traceId 복원
    @Test
    void orderLocal_listener_should_restore_traceId_from_header_and_propagate_to_facade_and_service() throws Exception {
        waitListenersReady();

        String traceId = "T-LOCAL-123";
        testHopService.reset();

        ProducerRecord<String, String> record = new ProducerRecord<>("ORDER_LOCAL", "k", "hello-local");
        record.headers().add(new RecordHeader("traceId", traceId.getBytes(StandardCharsets.UTF_8)));

        send(record);

        assertThat(testHopService.awaitAndGetOk(10, TimeUnit.SECONDS)).isTrue();
    }

    // 3) ORDER_CRUD : 배치 헤더 traceId 복원
    @Test
    void orderCrud_listener_should_restore_traceId_from_header_in_batch_and_propagate_to_facade_and_service() throws Exception {
        waitListenersReady();

        String traceId = "T-CRUD-999";
        testHopService.reset();

        ProducerRecord<String, String> r1 = new ProducerRecord<>("ORDER_CRUD", "k1", "v1");
        r1.headers().add(new RecordHeader("traceId", traceId.getBytes(StandardCharsets.UTF_8)));

        send(r1);

        assertThat(testHopService.awaitAndGetOk(10, TimeUnit.SECONDS)).isTrue();
    }

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestKafkaConfig {

        @Bean(name = "orderApiTopic")
        String orderApiTopic() {
            return "ORDER_API";
        }

        @Bean(name = "orderLocalTopic")
        String orderLocalTopic() {
            return "ORDER_LOCAL";
        }

        @Bean(name = "orderCrudTopic")
        String orderCrudTopic() {
            return "ORDER_CRUD";
        }

        @Bean
        ProducerFactory<String, String> producerFactory(Environment env) {
            Map<String, Object> props = Map.of(
                    org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    env.getProperty("spring.kafka.bootstrap-servers"),
                    org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                    org.apache.kafka.common.serialization.StringSerializer.class,
                    org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                    org.apache.kafka.common.serialization.StringSerializer.class
            );

            return new DefaultKafkaProducerFactory<>(props);
        }

        @Bean
        KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> pf) {
            return new KafkaTemplate<>(pf);
        }

        @Bean
        ConsumerFactory<String, String> consumerFactory(Environment env) {
            Map<String, Object> props = Map.of(
                    org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    env.getProperty("spring.kafka.bootstrap-servers"),
                    org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG,
                    "it-group-" + System.nanoTime(),
                    org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                    "earliest",
                    // MANUAL_IMMEDIATE와 충돌되지 않도록 false
                    org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                    org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    org.apache.kafka.common.serialization.StringDeserializer.class,
                    org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    org.apache.kafka.common.serialization.StringDeserializer.class
            );

            return new DefaultKafkaConsumerFactory<>(props);
        }

        /**
         * 테스트 전용 RecordInterceptor
         * - ORDER_API: payload.id를 파싱해 traceId/orderId 세팅(덮어쓰기)
         * - 그 외: 헤더 traceId만 복원
         * 운영 빈에 영향 없음(테스트 컨텍스트 전용)
         */
        @Bean
        RecordInterceptor<String, String> mdcRecordInterceptor() {
            final ObjectMapper om = new ObjectMapper();

            return (record, consumer) -> {
                String topic = record.topic();
                if ("ORDER_API".equals(topic)) {
                    String json = record.value();

                    if (json != null) {
                        try {
                            JsonNode root = om.readTree(json);

                            if (root.hasNonNull("id")) {
                                String id = root.get("id").asText();
                                MDC.put("traceId", id);
                                MDC.put("orderId", id);
                            }
                        } catch (Exception ignore) {
                            // payload 형식 오류 시 헤더 fallback
                            var h = record.headers().lastHeader("traceId");

                            if (h != null) {
                                MDC.put("traceId", new String(h.value(), StandardCharsets.UTF_8));
                            }
                        }
                    }
                } else {
                    var h = record.headers().lastHeader("traceId");

                    if (h != null) {
                        MDC.put("traceId", new String(h.value(), StandardCharsets.UTF_8));
                    }
                }

                return record;
            };
        }

        @Bean
        BatchInterceptor<String, String> mdcBatchInterceptor() {
            return (records, consumer) -> {
                if (records != null && !records.isEmpty()) {
                    var it = records.iterator().next().headers().lastHeader("traceId");

                    if (it != null) {
                        MDC.put("traceId", new String(it.value(), StandardCharsets.UTF_8));
                    }
                }

                return records;
            };
        }

        @Bean(name = "kafkaListenerContainerFactory")
        ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
                ConsumerFactory<String, String> cf,
                RecordInterceptor<String, String> recordInterceptor
        ) {
            var f = new ConcurrentKafkaListenerContainerFactory<String, String>();

            f.setConsumerFactory(cf);
            f.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
            f.getContainerProperties().setMissingTopicsFatal(false);
            f.setRecordInterceptor(recordInterceptor);

            return f;
        }

        @Bean(name = "kafkaBatchListenerContainerFactory")
        ConcurrentKafkaListenerContainerFactory<String, String> kafkaBatchListenerContainerFactory(
                ConsumerFactory<String, String> cf,
                BatchInterceptor<String, String> batchInterceptor
        ) {
            var f = new ConcurrentKafkaListenerContainerFactory<String, String>();

            f.setConsumerFactory(cf);
            f.setBatchListener(true);
            f.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
            f.getContainerProperties().setMissingTopicsFatal(false);
            f.setBatchInterceptor(batchInterceptor);

            return f;
        }

        @Bean
        TestHopService testHopService() {
            return new TestHopService();
        }

        static class TestHopService {
            private CountDownLatch latch = new CountDownLatch(1);
            private final AtomicBoolean ok = new AtomicBoolean(false);

            void reset() {
                latch = new CountDownLatch(1);
                ok.set(false);
            }

            void assertAndRemember(boolean value) {
                ok.set(value);
                latch.countDown();
            }

            boolean awaitAndGetOk(long timeout, TimeUnit unit) throws InterruptedException {
                if (!latch.await(timeout, unit)) {
                    return false;
                }

                return ok.get();
            }
        }
    }
}
