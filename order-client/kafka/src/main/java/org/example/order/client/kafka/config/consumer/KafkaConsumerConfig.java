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
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * consumer.enabled=true 일 때만 활성화되는 Consumer 구성.
 * - AckMode: MANUAL_IMMEDIATE 고정(리스너 내 ack.acknowledge() 호출 전제)
 * - 재처리 기본 없음(DefaultErrorHandler + FixedBackOff(0,0))
 * - SSL/SASL: kafka.ssl.enabled=true 일 때만 주입
 * - RecordInterceptor: Kafka 헤더 traceId → MDC 복원
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
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // 수동 커밋(ack 호출 시 즉시 커밋)
        ContainerProperties cp = factory.getContainerProperties();
        cp.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // 에러 핸들러: 재시도 없음(필요 시 바꿔서 사용)
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0L)));

        // 표준 설정
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(getDefaultConfigProps()));

        // traceId 복원 인터셉터 주입
        factory.setRecordInterceptor(new MdcRecordInterceptor());

        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaBatchListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // 옵션 널 안전 처리
        KafkaConsumerProperties.KafkaConsumerOption option =
                properties.getOption() != null ? properties.getOption() : new KafkaConsumerProperties.KafkaConsumerOption();

        // 배치 환경에서도 수동 커밋
        ContainerProperties cp = factory.getContainerProperties();

        if (option.getIdleBetweenPolls() != null) {
            cp.setIdleBetweenPolls(option.getIdleBetweenPolls());
        }

        cp.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // 배치 튜닝 옵션 반영 (널 체크 후 주입)
        Map<String, Object> configProps = getDefaultConfigProps();

        if (option.getMaxPollRecords() != null) {
            configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, option.getMaxPollRecords());
        }

        if (option.getFetchMaxWaitMs() != null) {
            configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, option.getFetchMaxWaitMs());
        }

        if (option.getFetchMaxBytes() != null) {
            configProps.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, option.getFetchMaxBytes());
        }

        if (option.getMaxPollIntervalMs() != null) {
            configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, option.getMaxPollIntervalMs());
        }

        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0L)));
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(configProps));
        factory.setBatchListener(true);

        // 배치 리스너에도 동일 인터셉터 주입
        factory.setRecordInterceptor(new MdcRecordInterceptor());

        return factory;
    }

    private Map<String, Object> getDefaultConfigProps() {

        Map<String, Object> propsMap = new HashMap<>();

        // 필수
        propsMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        propsMap.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propsMap.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

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
