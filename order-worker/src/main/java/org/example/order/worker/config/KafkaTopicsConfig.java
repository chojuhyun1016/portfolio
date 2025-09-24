package org.example.order.worker.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * 로컬/개발에서 토픽 자동 생성
 * prod에서는 app.kafka.auto-create-topics=false 로 설정
 * <p>
 * 역할 분리:
 * - 토픽 이름(String) 빈은 KafkaListenerTopicConfig에서만 관리
 * - 여기서는 NewTopic(생성)만 관리
 */
@Configuration
@ConditionalOnProperty(prefix = "app.kafka", name = "auto-create-topics", havingValue = "true", matchIfMissing = true)
public class KafkaTopicsConfig {

    public static final String TOPIC_ORDER_LOCAL = "local-order-local";
    public static final String TOPIC_ORDER_API = "local-order-api";
    public static final String TOPIC_ORDER_CRUD = "local-order-crud";
    public static final String TOPIC_ORDER_REMOTE = "local-order-remote";

    @Bean
    public NewTopic orderLocalTopicNew() {
        return TopicBuilder.name(TOPIC_ORDER_LOCAL).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic orderApiTopicNew() {
        return TopicBuilder.name(TOPIC_ORDER_API).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic orderCrudTopicNew() {
        return TopicBuilder.name(TOPIC_ORDER_CRUD).partitions(10).replicas(1).build();
    }

    @Bean
    public NewTopic orderRemoteTopicNew() {
        return TopicBuilder.name(TOPIC_ORDER_REMOTE).partitions(2).replicas(1).build();
    }
}
