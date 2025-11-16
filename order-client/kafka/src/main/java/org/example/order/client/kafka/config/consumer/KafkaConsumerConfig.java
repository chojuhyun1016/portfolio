package org.example.order.client.kafka.config.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.order.client.kafka.config.properties.KafkaConsumerProperties;
import org.example.order.client.kafka.config.properties.KafkaSSLProperties;
import org.example.order.client.kafka.interceptor.MdcBatchInterceptor;
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
import org.springframework.util.StringUtils;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * consumer.enabled=true 일 때만 활성화되는 Consumer 구성 (공용 팩토리).
 * - AckMode: MANUAL_IMMEDIATE
 * - ErrorHandlingDeserializer(delegate=JsonDeserializer) → 역직렬화 실패도 루프 없이 처리
 * - JsonDeserializer.TRUSTED_PACKAGES: 프로퍼티(kafka.consumer.trusted-packages, 필수)에서 주입
 * - spring.json.value.default.type 은 리스너별 @KafkaListener.properties 에서 지정
 * - RecordInterceptor / BatchInterceptor: Kafka 헤더 traceId/orderId → MDC 복원 + 역직렬화 실패 진단 로그
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({KafkaConsumerProperties.class, KafkaSSLProperties.class})
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka.consumer", name = "enabled", havingValue = "true")
@EnableKafka
public class KafkaConsumerConfig {

    private final KafkaConsumerProperties properties;
    private final KafkaSSLProperties sslProperties;

    // =====================================================================
    // 단건 리스너용 팩토리 (local / api 등)
    // =====================================================================
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // 수동 커밋(ack 호출 시 즉시 커밋)
        ContainerProperties cp = factory.getContainerProperties();
        cp.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // poll 간 idle 시간 (ms) - 옵션이 있으면 적용
        if (properties.getOption() != null && properties.getOption().getIdleBetweenPolls() != null) {
            cp.setIdleBetweenPolls(properties.getOption().getIdleBetweenPolls());
        }

        // 에러 핸들러: 재시도 없음 (DLQ 등은 애플리케이션 레벨에서 처리)
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0L)));

        // 공통 ConsumerFactory
        DefaultKafkaConsumerFactory<String, Object> consumerFactory = createConsumerFactory();

        factory.setConsumerFactory(consumerFactory);

        // 단건 리스너용 MDC 인터셉터
        factory.setRecordInterceptor(new MdcRecordInterceptor<>());

        return factory;
    }

    // =====================================================================
    // 배치 리스너용 팩토리 (crud 등)
    // =====================================================================
    @Bean(name = "kafkaBatchListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaBatchListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        ContainerProperties cp = factory.getContainerProperties();
        cp.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        if (properties.getOption() != null && properties.getOption().getIdleBetweenPolls() != null) {
            cp.setIdleBetweenPolls(properties.getOption().getIdleBetweenPolls());
        }

        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0L)));

        DefaultKafkaConsumerFactory<String, Object> consumerFactory = createConsumerFactory();

        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.setBatchInterceptor(new MdcBatchInterceptor<>());

        return factory;
    }

    // =====================================================================
    // 공용 ConsumerFactory (모든 리스너가 공유)
    // - 타입 결정은 @KafkaListener.properties 의 spring.json.value.default.type 에 맡김
    // =====================================================================
    private DefaultKafkaConsumerFactory<String, Object> createConsumerFactory() {
        Map<String, Object> props = getDefaultConfigProps();

        // ErrorHandlingDeserializer + JsonDeserializer 조합 설정
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // JsonDeserializer: 신뢰 패키지 설정
        props.put(JsonDeserializer.TRUSTED_PACKAGES, requireTrustedPackages());

        // 타입 결정은:
        // - @KafkaListener.properties 의 spring.json.value.default.type
        // - 또는 type headers (USE_TYPE_INFO_HEADERS=true 인 경우)
        return new DefaultKafkaConsumerFactory<>(props);
    }

    // =====================================================================
    // 공통 기본 프로퍼티
    // =====================================================================
    private Map<String, Object> getDefaultConfigProps() {
        Map<String, Object> propsMap = new HashMap<>();

        // 필수
        propsMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());

        if (properties.getOption() != null) {
            KafkaConsumerProperties.KafkaConsumerOption opt = properties.getOption();

            if (opt.getEnableAutoCommit() != null) {
                propsMap.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, opt.getEnableAutoCommit());
            }

            if (opt.getAutoOffsetReset() != null) {
                propsMap.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, opt.getAutoOffsetReset());
            }

            if (opt.getMaxPollRecords() != null) {
                propsMap.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, opt.getMaxPollRecords());
            }

            if (opt.getFetchMaxBytes() != null) {
                propsMap.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, opt.getFetchMaxBytes());
            }

            if (opt.getFetchMaxWaitMs() != null) {
                propsMap.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, opt.getFetchMaxWaitMs());
            }

            if (opt.getMaxPollIntervalMs() != null) {
                propsMap.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, opt.getMaxPollIntervalMs());
            }
            // 나머지 maxFailCount 등은 애플리케이션 레벨에서 활용
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

    /**
     * trusted-packages 누락 시 즉시 실패 (명시적 메시지/로그)
     */
    private String requireTrustedPackages() {
        String trusted = properties.getTrustedPackages();

        if (!StringUtils.hasText(trusted)) {
            String msg = """
                    Missing required property: 'kafka.consumer.trusted-packages'
                    - 이유: JsonDeserializer의 보안 정책상 신뢰 가능한 패키지를 명시해야 역직렬화가 허용됩니다.
                    - 예시) kafka.consumer.trusted-packages: "org.example.order.*,org.example.common.*"
                    - 주의) "*" 전체 허용은 테스트/로컬 한정으로만 사용하세요.
                    """;
            log.error(msg);

            throw new IllegalStateException("kafka.consumer.trusted-packages is required but not set");
        }

        return trusted;
    }
}
