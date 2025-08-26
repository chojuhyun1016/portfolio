package org.example.order.client.kafka.config.consumer;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.order.client.kafka.config.properties.KafkaConsumerProperties;
import org.example.order.client.kafka.config.properties.KafkaSSLProperties;
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
 * 큰 맥락
 * - consumer.enabled=true 일 때에만 컨슈머 관련 빈과 @EnableKafka를 활성화한다.
 * - 기본 팩토리(단건)와 배치용 팩토리를 분리 제공한다.
 * - Ack 모드는 MANUAL_IMMEDIATE(리스너에서 ack.acknowledge() 호출 전제)로 고정한다.
 * - 재처리는 테스트/운영 정책에 맞추기 위해 기본 값은 "재시도 없음"으로 둔다.
 * - SSL/SASL 설정은 kafka.ssl.enabled=true 일 때만 주입한다.
 */
@Configuration
@EnableConfigurationProperties({KafkaConsumerProperties.class, KafkaSSLProperties.class})
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka.consumer", name = "enabled", havingValue = "true")
@EnableKafka
public class KafkaConsumerConfig {

    private final KafkaConsumerProperties properties;
    private final KafkaSSLProperties sslProperties;

    /**
     * 단건 리스너 컨테이너 팩토리
     */
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
        return factory;
    }

    /**
     * 배치 리스너 컨테이너 팩토리
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaBatchListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        KafkaConsumerProperties.KafkaConsumerOption option = properties.getOption();

        // 배치 환경에서도 수동 커밋
        ContainerProperties cp = factory.getContainerProperties();
        cp.setIdleBetweenPolls(option.getIdleBetweenPolls());
        cp.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // 배치 튜닝 옵션 반영
        Map<String, Object> configProps = getDefaultConfigProps();
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, option.getMaxPollRecords());
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, option.getFetchMaxWaitMs());
        configProps.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, option.getFetchMaxBytes());
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, option.getMaxPollIntervalMs());

        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0L)));
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(configProps));
        factory.setBatchListener(true);
        return factory;
    }

    /**
     * 공통 컨슈머 프로퍼티
     */
    private Map<String, Object> getDefaultConfigProps() {
        Map<String, Object> propsMap = new HashMap<>();
        propsMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        propsMap.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propsMap.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propsMap.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, properties.getOption().getEnableAutoCommit());
        propsMap.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, properties.getOption().getAutoOffsetReset());

        // 보안 설정은 명시적으로 켜진 경우에만 적용
        if (sslProperties.isEnabled()) {
            propsMap.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, sslProperties.getSecurityProtocol());
            propsMap.put(SaslConfigs.SASL_MECHANISM, sslProperties.getSaslMechanism());
            propsMap.put(SaslConfigs.SASL_JAAS_CONFIG, sslProperties.getSaslJaasConfig());
            propsMap.put(SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS, sslProperties.getSaslClientCallbackHandlerClass());
        }
        return propsMap;
    }
}
