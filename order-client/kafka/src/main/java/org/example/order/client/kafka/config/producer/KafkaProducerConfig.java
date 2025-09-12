package org.example.order.client.kafka.config.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.order.client.kafka.config.properties.KafkaProducerProperties;
import org.example.order.client.kafka.config.properties.KafkaSSLProperties;
import org.example.order.client.kafka.interceptor.TraceIdProducerInterceptor;
import org.example.order.common.support.json.ObjectMapperFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * producer.enabled=true 일 때만 활성화되는 Producer 구성.
 * - VALUE: JsonSerializer(ObjectMapper) 고정 → 도메인 객체 전송 표준화
 * - 압축: LZ4, 배치 크기: 64KiB
 * - SSL/SASL: kafka.ssl.enabled=true 일 때만 주입
 * - 인터셉터(TraceIdProducerInterceptor) 자동 등록
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({KafkaProducerProperties.class, KafkaSSLProperties.class})
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka.producer", name = "enabled", havingValue = "true")
public class KafkaProducerConfig {

    private final KafkaProducerProperties kafkaProducerProperties;
    private final KafkaSSLProperties sslProperties;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {

        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProducerProperties.getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, CompressionType.LZ4.name);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 65_536);

        // Producer 인터셉터 등록(FQN)
        configProps.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, TraceIdProducerInterceptor.class.getName());

        // 보안 설정: 필요 시에만
        if (sslProperties.isEnabled()) {
            configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, sslProperties.getSecurityProtocol());
            configProps.put(SaslConfigs.SASL_MECHANISM, sslProperties.getSaslMechanism());
            configProps.put(SaslConfigs.SASL_JAAS_CONFIG, sslProperties.getSaslJaasConfig());
            configProps.put(SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS, sslProperties.getSaslClientCallbackHandlerClass());
        }

        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(configProps);
        factory.setValueSerializer(new JsonSerializer<>(ObjectMapperFactory.defaultObjectMapper()));

        return factory;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
