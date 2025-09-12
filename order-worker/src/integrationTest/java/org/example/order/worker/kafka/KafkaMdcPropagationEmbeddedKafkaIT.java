package org.example.order.worker.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.example.order.common.support.logging.CorrelationAspect;
import org.example.order.worker.IntegrationBoot;
import org.example.order.worker.facade.order.OrderApiMessageFacade;
import org.example.order.worker.facade.order.OrderCrudMessageFacade;
import org.example.order.worker.facade.order.OrderLocalMessageFacade;
import org.example.order.worker.kafka.support.EmbeddedKafkaITBase;
import org.example.order.worker.kafka.support.TestOrderApiListener; // ★ 테스트용 리스너 (수정본)
import org.example.order.worker.listener.order.impl.OrderCrudMessageListenerImpl;
import org.example.order.worker.listener.order.impl.OrderLocalMessageListenerImpl;
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
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.BatchInterceptor;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

/**
 * Kafka 리스너 → Facade → Service 경유 시 MDC(traceId/orderId) 전파 통합 테스트
 * - ORDER_API : @Correlate 로 payload.id → traceId/orderId 덮어쓰기 검증 (TestOrderApiListener 사용)
 * - ORDER_LOCAL/ORDER_CRUD : 헤더 traceId → MDC 복원(Record/Batch 인터셉터) 검증
 */
@SpringBootTest(
        classes = {
                IntegrationBoot.class,
                KafkaMdcPropagationEmbeddedKafkaIT.TestKafkaConfig.class,
                KafkaMdcPropagationEmbeddedKafkaIT.KafkaInfraConfig.class,
                TestOrderApiListener.class,             // ★ 변경: 이 리스너가 @Correlate를 정확히 적용
                OrderLocalMessageListenerImpl.class,
                OrderCrudMessageListenerImpl.class,
                CorrelationAspect.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.main.web-application-type=none"
)
@EmbeddedKafka(
        topics = {"ORDER_API", "ORDER_LOCAL", "ORDER_CRUD"},
        partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"}
)
@DirtiesContext
@Import({KafkaMdcPropagationEmbeddedKafkaIT.TestKafkaConfig.class, KafkaMdcPropagationEmbeddedKafkaIT.KafkaInfraConfig.class})
class KafkaMdcPropagationEmbeddedKafkaIT extends EmbeddedKafkaITBase {

    @MockBean
    private OrderApiMessageFacade orderApiMessageFacade;

    @MockBean
    private OrderLocalMessageFacade orderLocalMessageFacade;

    @MockBean
    private OrderCrudMessageFacade orderCrudMessageFacade;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    // 1) ORDER_API (@Correlate override)
    @Test
    void orderApi_listener_should_override_traceId_with_payload_id_and_propagate_to_facade_and_service() throws Exception {
        long id = 777L;
        String payload = "{\"id\":" + id + ",\"publishedTimestamp\":" + System.currentTimeMillis() + "}";

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ok = new AtomicBoolean(false);

        Mockito.doAnswer(inv -> {
            ok.set(Objects.equals("777", MDC.get("traceId")) && Objects.equals("777", MDC.get("orderId")));
            latch.countDown();
            return null;
        }).when(orderApiMessageFacade).requestApi(any());

        ProducerRecord<String, String> record = new ProducerRecord<>("ORDER_API", "k", payload);

        record.headers().add(new RecordHeader("traceId",
                "incoming-should-be-overridden".getBytes(StandardCharsets.UTF_8)));

        send(record);

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(ok.get()).isTrue();
    }

    // 2) ORDER_LOCAL (헤더 → MDC)
    @Test
    void orderLocal_listener_should_restore_traceId_from_header_and_propagate_to_facade_and_service() throws Exception {
        String traceId = "T-LOCAL-123";
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ok = new AtomicBoolean(false);

        Mockito.doAnswer(inv -> {
            ok.set(Objects.equals(traceId, MDC.get("traceId")));
            latch.countDown();
            return null;
        }).when(orderLocalMessageFacade).sendOrderApiTopic(any());

        ProducerRecord<String, String> record = new ProducerRecord<>("ORDER_LOCAL", "k", "hello-local");
        record.headers().add(new RecordHeader("traceId", traceId.getBytes(StandardCharsets.UTF_8)));

        send(record);

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(ok.get()).isTrue();
    }

    // 3) ORDER_CRUD (배치: 헤더 → MDC)
    @Test
    void orderCrud_listener_should_restore_traceId_from_header_in_batch_and_propagate_to_facade_and_service() throws Exception {
        String traceId = "T-CRUD-999";
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ok = new AtomicBoolean(false);

        Mockito.doAnswer(inv -> {
            ok.set(Objects.equals(traceId, MDC.get("traceId")));
            latch.countDown();
            return null;
        }).when(orderCrudMessageFacade).executeOrderCrud(any());

        ProducerRecord<String, String> r1 = new ProducerRecord<>("ORDER_CRUD", "k1", "v1");
        r1.headers().add(new RecordHeader("traceId", traceId.getBytes(StandardCharsets.UTF_8)));

        send(r1);

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(ok.get()).isTrue();
    }

    // 공통 전송
    private void send(ProducerRecord<String, String> record) throws Exception {
        CompletableFuture<SendResult<String, String>> f = kafkaTemplate.send(record);
        f.get(10, TimeUnit.SECONDS);
    }

    // ===== 테스트 전용 구성 =====
    @TestConfiguration
    @EnableKafka
    @EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
    static class KafkaInfraConfig {
    }

    @TestConfiguration
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
                    org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                    org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    org.apache.kafka.common.serialization.StringDeserializer.class,
                    org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    org.apache.kafka.common.serialization.StringDeserializer.class
            );
            return new DefaultKafkaConsumerFactory<>(props);
        }

        @Bean
        RecordInterceptor<String, String> mdcRecordInterceptor() {
            return (record, consumer) -> {
                var h = record.headers().lastHeader("traceId");

                if (h != null) {
                    MDC.put("traceId", new String(h.value(), StandardCharsets.UTF_8));
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
    }
}
