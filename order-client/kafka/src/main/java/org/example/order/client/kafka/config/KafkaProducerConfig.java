package org.example.order.client.kafka.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.order.client.kafka.config.property.KafkaProducerProperties;
import org.example.order.client.kafka.config.property.KafkaSSLProperties;
import org.example.order.common.jackson.config.CommonObjectMapperFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableConfigurationProperties({KafkaProducerProperties.class, KafkaSSLProperties.class})
@RequiredArgsConstructor
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
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 65536);

        if (sslProperties.isEnabled()) {
            configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, sslProperties.getSecurityProtocol());
            configProps.put(SaslConfigs.SASL_MECHANISM, sslProperties.getSaslMechanism());
            configProps.put(SaslConfigs.SASL_JAAS_CONFIG, sslProperties.getSaslJaasConfig());
            configProps.put(SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS, sslProperties.getSaslClientCallbackHandlerClass());
        }

        DefaultKafkaProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(configProps);
        producerFactory.setValueSerializer(new JsonSerializer<>(CommonObjectMapperFactory.defaultObjectMapper()));
        return producerFactory;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
