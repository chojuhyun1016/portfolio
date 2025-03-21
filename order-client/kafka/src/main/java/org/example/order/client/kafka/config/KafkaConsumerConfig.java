package org.example.order.client.kafka.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.order.client.kafka.config.property.KafkaConsumerProperties;
import org.example.order.client.kafka.config.property.KafkaSSLProperties;
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

@EnableKafka
@Configuration
@EnableConfigurationProperties({KafkaConsumerProperties.class, KafkaSSLProperties.class})
@RequiredArgsConstructor
public class KafkaConsumerConfig {
    private final KafkaConsumerProperties properties;
    private final KafkaSSLProperties sslProperties;

    // default
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();

        ContainerProperties containerProperties = factory.getContainerProperties();
        containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE); // acknowledgment.acknowledge() 동작 시 offset 즉시 커밋

        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0L))); // 재처리 하지 않음
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(getDefaultConfigProps()));

        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaBatchListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        KafkaConsumerProperties.KafkaConsumerOption option = properties.getOption();


        ContainerProperties containerProperties = factory.getContainerProperties();
        containerProperties.setIdleBetweenPolls(option.getIdleBetweenPolls()); // polling 사이 대기 시간
        containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE); // acknowledgment.acknowledge() 동작 시 offset 즉시 커밋

        Map<String, Object> configProps = getDefaultConfigProps();
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, option.getMaxPollRecords()); // 한번에 가져올 최대 메세지
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, option.getFetchMaxWaitMs()); // 메세지 크기를 채우지 못했을 때 기다릴 최대 시간
        configProps.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, option.getFetchMaxBytes()); // 50mb
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, option.getMaxPollIntervalMs()); // 해당 시간동안 poll 발생 안할 시 리밸런싱

        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0L))); // 재처리 하지 않음
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(configProps));
        factory.setBatchListener(true);

        return factory;
    }

    private Map<String, Object> getDefaultConfigProps() {
        Map<String, Object> defaultConfigProps = new HashMap<>();
        defaultConfigProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        defaultConfigProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        defaultConfigProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        defaultConfigProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, properties.getOption().getEnableAutoCommit()); // offset auto commit
        defaultConfigProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, properties.getOption().getAutoOffsetReset()); // offset commit 이후 메시지 부터 읽음

        if (sslProperties.isEnabled()) {
            defaultConfigProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, sslProperties.getSecurityProtocol());
            defaultConfigProps.put(SaslConfigs.SASL_MECHANISM, sslProperties.getSaslMechanism());
            defaultConfigProps.put(SaslConfigs.SASL_JAAS_CONFIG, sslProperties.getSaslJaasConfig());
            defaultConfigProps.put(SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS, sslProperties.getSaslClientCallbackHandlerClass());
        }

        return defaultConfigProps;
    }
}
