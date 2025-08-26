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
 * 큰 맥락
 * - producer.enabled=true 일 때에만 ProducerFactory/KafkaTemplate 빈을 생성한다.
 * - VALUE 직렬화는 JsonSerializer(ObjectMapper 기반)로 고정해 도메인 객체 전송을 표준화한다.
 * - 전송 효율을 위해 LZ4 압축과 batch.size(기본 64KiB)를 적용한다.
 * - SSL/SASL 설정은 kafka.ssl.enabled=true 일 때만 주입해 로컬/테스트 환경을 간단히 유지한다.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({KafkaProducerProperties.class, KafkaSSLProperties.class})
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka.producer", name = "enabled", havingValue = "true")
public class KafkaProducerConfig {

    private final KafkaProducerProperties kafkaProducerProperties;
    private final KafkaSSLProperties sslProperties;

    /**
     * ProducerFactory
     * - 부트스트랩 서버, 직렬화, 압축, 배치 크기 등 핵심 프로듀서 옵션 구성
     * - SSL/SASL은 명시적으로 활성화된 경우에만 설정
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProducerProperties.getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, CompressionType.LZ4.name);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 65_536);

        // 보안 설정은 필요할 때만 주입
        if (sslProperties.isEnabled()) {
            configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, sslProperties.getSecurityProtocol());
            configProps.put(SaslConfigs.SASL_MECHANISM, sslProperties.getSaslMechanism());
            configProps.put(SaslConfigs.SASL_JAAS_CONFIG, sslProperties.getSaslJaasConfig());
            configProps.put(SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS, sslProperties.getSaslClientCallbackHandlerClass());
        }

        // JsonSerializer에 서비스 공통 ObjectMapper 주입
        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(configProps);
        factory.setValueSerializer(new JsonSerializer<>(ObjectMapperFactory.defaultObjectMapper()));
        return factory;
    }

    /**
     * KafkaTemplate
     * - 애플리케이션에서 메시지 전송을 단순화하는 템플릿
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
