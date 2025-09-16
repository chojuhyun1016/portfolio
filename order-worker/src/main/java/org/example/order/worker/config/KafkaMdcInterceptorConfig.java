package org.example.order.worker.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.BatchInterceptor;
import org.springframework.kafka.listener.RecordInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * 운영 환경에서도 MDC(traceId/orderId) 전파를 항상 보장하기 위한 인터셉터 구성.
 * <p>
 * - RecordInterceptor
 * * ORDER_API 토픽일 때: payload(JSON)의 "id"를 traceId/orderId로 강제 세팅(덮어쓰기).
 * * 그 외: Kafka 헤더 "traceId"를 MDC로 복원.
 * <p>
 * - BatchInterceptor
 * * 배치(예: CRUD)에서 첫 레코드의 Kafka 헤더 "traceId"를 MDC로 복원.
 * <p>
 * 주의:
 * - 프로젝트 Spring Kafka 버전에 getRecordInterceptor()/getBatchInterceptor() 게터가 없을 수 있으므로
 * "이미 있는지 확인" 로직 없이 세터만 사용합니다.
 */
@Configuration
@ConditionalOnClass(ConcurrentKafkaListenerContainerFactory.class)
public class KafkaMdcInterceptorConfig {

    /**
     * RecordInterceptor: ORDER_API는 payload.id → traceId/orderId, 그 외는 헤더 traceId 복원.
     * - orderApiTopic 빈이 없으면(테스트/환경에 따라) 헤더 복원만 수행합니다.
     */
    @Bean
    public RecordInterceptor<Object, Object> mdcRecordInterceptor(
            @Qualifier("orderApiTopic") ObjectProvider<String> orderApiTopicOpt,
            ObjectMapper objectMapper
    ) {
        final Optional<String> orderApiTopic = Optional.ofNullable(orderApiTopicOpt.getIfAvailable());

        return new RecordInterceptor<>() {
            @Override
            public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record,
                                                            Consumer<Object, Object> consumer) {
                try {
                    // ORDER_API: payload(JSON)의 id로 traceId/orderId 덮어쓰기
                    if (orderApiTopic.isPresent() && orderApiTopic.get().equals(record.topic())) {
                        Object val = record.value();

                        if (val instanceof String s) {
                            JsonNode root = objectMapper.readTree(s);

                            if (root.hasNonNull("id")) {
                                String id = root.get("id").asText();
                                MDC.put("traceId", id);
                                MDC.put("orderId", id);

                                return record;
                            }
                        }
                    }

                    // 그 외: 헤더 traceId 복원 (또는 ORDER_API에서 JSON 파싱 실패 시 fallback)
                    var h = record.headers().lastHeader("traceId");

                    if (h != null) {
                        MDC.put("traceId", new String(h.value(), StandardCharsets.UTF_8));
                    }
                } catch (Exception ignore) {
                    // 파싱 실패 시 헤더 fallback
                    var h = record.headers().lastHeader("traceId");

                    if (h != null) {
                        MDC.put("traceId", new String(h.value(), StandardCharsets.UTF_8));
                    }
                }

                return record;
            }
        };
    }

    /**
     * BatchInterceptor: 배치의 첫 레코드 헤더에서 traceId 복원.
     */
    @Bean
    public BatchInterceptor<Object, Object> mdcBatchInterceptor() {
        return (records, consumer) -> {
            if (records != null && !records.isEmpty()) {
                var h = records.iterator().next().headers().lastHeader("traceId");

                if (h != null) {
                    MDC.put("traceId", new String(h.value(), StandardCharsets.UTF_8));
                }
            }

            return records;
        };
    }

    /**
     * 컨텍스트가 올라온 뒤, 등록된 모든 ConcurrentKafkaListenerContainerFactory에
     * 우리 MDC 인터셉터를 **타입 안전하게** 세팅.
     * <p>
     * - 단건 팩토리: RecordInterceptor 설정
     * - 배치 팩토리: RecordInterceptor + BatchInterceptor 설정
     */
    @Bean
    public SmartInitializingSingleton attachMdcInterceptors(
            Map<String, ConcurrentKafkaListenerContainerFactory<?, ?>> factories,
            RecordInterceptor<Object, Object> mdcRecordInterceptor,
            BatchInterceptor<Object, Object> mdcBatchInterceptor
    ) {
        return () -> factories.values().forEach(factory ->
                attachFactory(factory, mdcRecordInterceptor, mdcBatchInterceptor)
        );
    }

    /**
     * 제네릭 헬퍼: 한 지점에서만 안전하게 캐스팅을 수행하고,
     * 나머지는 K/V 제네릭에 맞춰 타입을 보존.
     */
    private static <K, V> void attachFactory(
            ConcurrentKafkaListenerContainerFactory<K, V> factory,
            RecordInterceptor<Object, Object> recordInterceptorObj,
            BatchInterceptor<Object, Object> batchInterceptorObj
    ) {
        @SuppressWarnings("unchecked")
        RecordInterceptor<K, V> recordInterceptor =
                (RecordInterceptor<K, V>) (RecordInterceptor<?, ?>) recordInterceptorObj;

        factory.setRecordInterceptor(recordInterceptor);

        if (factory.isBatchListener()) {
            @SuppressWarnings("unchecked")
            BatchInterceptor<K, V> batchInterceptor =
                    (BatchInterceptor<K, V>) (BatchInterceptor<?, ?>) batchInterceptorObj;

            factory.setBatchInterceptor(batchInterceptor);
        }
    }
}
