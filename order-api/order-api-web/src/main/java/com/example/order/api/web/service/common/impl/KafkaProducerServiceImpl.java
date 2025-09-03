package com.example.order.api.web.service.common.impl;

import com.example.order.api.web.service.common.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.client.kafka.service.KafkaProducerCluster;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties({KafkaTopicProperties.class})
@ConditionalOnBean(KafkaProducerCluster.class)
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private final KafkaProducerCluster cluster;

    private void send(Object message, String topic) {
        cluster.sendMessage(message, topic);
    }
}
