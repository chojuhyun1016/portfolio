package org.example.order.worker.config;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
@Profile("local")
@ConditionalOnProperty(prefix = "app.kafka", name = "auto-create-topics", havingValue = "true", matchIfMissing = true)
public class KafkaTopicsConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaTopicsConfig.class);

    // ===== 토픽 이름 =====
    public static final String TOPIC_ORDER_LOCAL = "local-order-local";
    public static final String TOPIC_ORDER_API = "local-order-api";
    public static final String TOPIC_ORDER_CRUD = "local-order-crud";
    public static final String TOPIC_ORDER_REMOTE = "local-order-remote";
    public static final String TOPIC_ORDER_DEAD_LETTER = "local-order-dead-letter";
    public static final String TOPIC_ORDER_ALARM = "local-order-alarm";

    private final Environment env;

    public KafkaTopicsConfig(Environment env) {
        this.env = env;
    }

    // ===== KafkaAdmin (스프링이 토픽 빈을 생성할 때 참조) =====
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> props = new HashMap<>();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, resolveBootstrap());

        // 필요시 추가 Admin 설정 가능
        return new KafkaAdmin(props);
    }

    // ===== NewTopic (스프링 KafkaAdmin 경로) =====
    @Bean(name = "newTopic.orderLocal")
    public NewTopic orderLocalTopicNew() {
        return TopicBuilder.name(TOPIC_ORDER_LOCAL).partitions(2).replicas(1).build();
    }

    @Bean(name = "newTopic.orderApi")
    public NewTopic orderApiTopicNew() {
        return TopicBuilder.name(TOPIC_ORDER_API).partitions(2).replicas(1).build();
    }

    @Bean(name = "newTopic.orderCrud")
    public NewTopic orderCrudTopicNew() {
        return TopicBuilder.name(TOPIC_ORDER_CRUD).partitions(10).replicas(1).build();
    }

    @Bean(name = "newTopic.orderRemote")
    public NewTopic orderRemoteTopicNew() {
        return TopicBuilder.name(TOPIC_ORDER_REMOTE).partitions(2).replicas(1).build();
    }

    @Bean(name = "newTopic.orderDeadLetter")
    public NewTopic orderDeadLetterTopicNew() {
        return TopicBuilder.name(TOPIC_ORDER_DEAD_LETTER).partitions(1).replicas(1).build();
    }

    @Bean(name = "newTopic.orderAlarm")
    public NewTopic orderAlarmTopicNew() {
        return TopicBuilder.name(TOPIC_ORDER_ALARM).partitions(1).replicas(1).build();
    }

    @Bean(name = "newTopics.all")
    public KafkaAdmin.NewTopics defineAllTopics() {
        return new KafkaAdmin.NewTopics(
                orderLocalTopicNew(),
                orderApiTopicNew(),
                orderCrudTopicNew(),
                orderRemoteTopicNew(),
                orderDeadLetterTopicNew(),
                orderAlarmTopicNew()
        );
    }

    // ===== 기동 완료 후 최종 보장 (관리자용 AdminClient) =====
    @EventListener(ApplicationReadyEvent.class)
    @ConditionalOnProperty(prefix = "app.kafka", name = "ensure-at-startup", havingValue = "true", matchIfMissing = true)
    public void ensureTopicsAfterStartup() {
        String bootstrap = resolveBootstrap();
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "30000");
        props.put(AdminClientConfig.RETRIES_CONFIG, "5");
        props.put(AdminClientConfig.RETRY_BACKOFF_MS_CONFIG, "500");
        props.putIfAbsent("security.protocol", "PLAINTEXT");

        Map<String, NewTopic> desired = new LinkedHashMap<>();
        desired.put(TOPIC_ORDER_LOCAL, orderLocalTopicNew());
        desired.put(TOPIC_ORDER_API, orderApiTopicNew());
        desired.put(TOPIC_ORDER_CRUD, orderCrudTopicNew());
        desired.put(TOPIC_ORDER_REMOTE, orderRemoteTopicNew());
        desired.put(TOPIC_ORDER_DEAD_LETTER, orderDeadLetterTopicNew());
        desired.put(TOPIC_ORDER_ALARM, orderAlarmTopicNew());

        // 브로커 준비 대기(최대 30초, 1초 간격)
        int maxWaitSec = 30;
        Exception lastError = null;

        for (int i = 0; i < maxWaitSec; i++) {
            try (AdminClient admin = AdminClient.create(props)) {
                Set<String> existing = admin.listTopics(new ListTopicsOptions().listInternal(false))
                        .names().get(10, TimeUnit.SECONDS);

                List<NewTopic> toCreate = desired.entrySet().stream()
                        .filter(e -> !existing.contains(e.getKey()))
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList());

                if (!toCreate.isEmpty()) {
                    log.info("[KafkaTopicsConfig] 생성 대상: {}", toCreate.stream().map(NewTopic::name).toList());

                    var result = admin.createTopics(toCreate, new CreateTopicsOptions().timeoutMs(30000));

                    for (Map.Entry<String, KafkaFuture<Void>> e : result.values().entrySet()) {
                        try {
                            e.getValue().get(30, TimeUnit.SECONDS);

                            log.info("[KafkaTopicsConfig] 토픽 생성 성공: {}", e.getKey());
                        } catch (Exception ex) {
                            log.warn("[KafkaTopicsConfig] 토픽 생성 경고: {} - {}", e.getKey(), ex.toString());
                        }
                    }
                }

                // 최종 검증
                Set<String> after = admin.listTopics(new ListTopicsOptions().listInternal(false))
                        .names().get(10, TimeUnit.SECONDS);
                List<String> missing = desired.keySet().stream().filter(t -> !after.contains(t)).toList();

                if (!missing.isEmpty()) {
                    throw new IllegalStateException("아직 존재하지 않는 토픽: " + missing);
                }

                log.info("[KafkaTopicsConfig] 최종 확인 완료. 모든 토픽 존재: {}", desired.keySet());

                return;
            } catch (Exception e) {
                lastError = e;
                String msg = e.toString();

                if (msg.contains("UnknownHostException") || msg.contains("Timeout")) {
                    log.warn("[KafkaTopicsConfig] 브로커 준비 대기 중... ({}s/{}) - {}", i + 1, maxWaitSec, msg);

                    sleep(Duration.ofSeconds(1));

                    continue;
                }

                log.error("[KafkaTopicsConfig] 토픽 보장 중 예외", e);

                break;
            }
        }

        log.error("[KafkaTopicsConfig] 토픽 보장 실패 (bootstrap={}, wait={}s)", bootstrap, maxWaitSec, lastError);
    }

    // ===== 유틸 =====
    private String resolveBootstrap() {
        // 우선순위:
        // 1) spring.kafka.bootstrap-servers
        // 2) kafka.producer.bootstrap-servers
        // 3) kafka.consumer.bootstrap-servers
        // 4) 디폴트
        String v =
                env.getProperty("spring.kafka.bootstrap-servers",
                        env.getProperty("kafka.producer.bootstrap-servers",
                                env.getProperty("kafka.consumer.bootstrap-servers",
                                        "127.0.0.1:29092")));

        log.info("[KafkaTopicsConfig] bootstrap-servers={}", v);

        return v;
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException ignored) {
        }
    }
}
