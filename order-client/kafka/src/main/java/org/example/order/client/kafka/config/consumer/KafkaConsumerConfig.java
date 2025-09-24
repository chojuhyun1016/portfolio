package org.example.order.client.kafka.config.consumer;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.order.client.kafka.config.properties.KafkaConsumerProperties;
import org.example.order.client.kafka.config.properties.KafkaSSLProperties;
import org.example.order.client.kafka.interceptor.MdcRecordInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * consumer.enabled=true 일 때만 활성화되는 Consumer 구성 (공용 팩토리).
 * - AckMode: MANUAL_IMMEDIATE
 * - ErrorHandlingDeserializer(delegate=JsonDeserializer) → 역직렬화 실패도 루프 없이 처리
 * - JsonDeserializer.TRUSTED_PACKAGES 등록
 * - JsonDeserializer.USE_TYPE_INFO_HEADERS=false (타입 헤더 무시)
 * - VALUE_DEFAULT_TYPE 은 전역에서 설정하지 않음(리스너별 @KafkaListener.properties 로 지정)
 * - RecordInterceptor: Kafka 헤더 traceId → MDC 복원(+ key → orderId)
 */
@Configuration
@EnableConfigurationProperties({KafkaConsumerProperties.class, KafkaSSLProperties.class})
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka.consumer", name = "enabled", havingValue = "true")
@EnableKafka
public class KafkaConsumerConfig {

    private final KafkaConsumerProperties properties;
    private final KafkaSSLProperties sslProperties;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // 수동 커밋(ack 호출 시 즉시 커밋)
        ContainerProperties cp = factory.getContainerProperties();
        cp.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // 에러 핸들러: 재시도 없음
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0L)));

        // ★ 프로퍼티 기반으로만 deserializer 구성 (new 하지 않음)
        Map<String, Object> props = getDefaultConfigProps();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);

        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // JsonDeserializer 공통 설정
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "org.example.order.*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        DefaultKafkaConsumerFactory<String, Object> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props);

        factory.setConsumerFactory(consumerFactory);

        // 공용 MDC 인터셉터
        factory.setRecordInterceptor(new MdcRecordInterceptor<>());

        return factory;
    }

    @Bean(name = "kafkaBatchListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaBatchListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        ContainerProperties cp = factory.getContainerProperties();
        cp.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0L)));

        Map<String, Object> props = getDefaultConfigProps();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);

        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        props.put(JsonDeserializer.TRUSTED_PACKAGES, "org.example.order.*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        DefaultKafkaConsumerFactory<String, Object> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props);

        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.setRecordInterceptor(new MdcRecordInterceptor<>());

        return factory;
    }

    private Map<String, Object> getDefaultConfigProps() {
        Map<String, Object> propsMap = new HashMap<>();

        // 필수
        propsMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());

        // 옵션 널 안전 처리
        if (properties.getOption() != null) {
            if (properties.getOption().getEnableAutoCommit() != null) {
                propsMap.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, properties.getOption().getEnableAutoCommit());
            }
            if (properties.getOption().getAutoOffsetReset() != null) {
                propsMap.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, properties.getOption().getAutoOffsetReset());
            }
        }

        // 보안 설정: 명시적으로 켜진 경우에만
        if (sslProperties.isEnabled()) {
            propsMap.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, sslProperties.getSecurityProtocol());
            propsMap.put(SaslConfigs.SASL_MECHANISM, sslProperties.getSaslMechanism());
            propsMap.put(SaslConfigs.SASL_JAAS_CONFIG, sslProperties.getSaslJaasConfig());
            propsMap.put(SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS, sslProperties.getSaslClientCallbackHandlerClass());
        }

        return propsMap;
    }
}
