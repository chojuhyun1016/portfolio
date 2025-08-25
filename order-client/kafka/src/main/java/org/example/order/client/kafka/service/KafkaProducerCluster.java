package org.example.order.client.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Producer 사용 서비스
 * - ✨ KafkaTemplate 빈이 있을 때만 생성(즉, producer.enabled=true일 때만)
 *   → @ConditionalOnBean(KafkaTemplate.class)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(KafkaTemplate.class)
public class KafkaProducerCluster implements SmartLifecycle {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private Boolean isRunning = false;

    public void sendMessage(Object data, String topic) {
        Message<Object> message = MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sending kafka message - topic: {}, message: {}, offset: {}",
                        topic, result.getProducerRecord().value().toString(), result.getRecordMetadata().offset());
            } else {
                log.error("error : Sending kafka message failed - topic: {}, message: {}", topic, ex.getMessage(), ex);
            }
        });
    }

    @Override
    public void start() {
        this.isRunning = true;
    }

    @Override
    public void stop() {
        log.info("Stopping kafka producer");
        kafkaTemplate.flush();
        this.isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    /**
     * SmartLifecycle 구현 하지 않은 일반 Bean 보다 먼저 시작, 나중에 종료
     * SmartLifecycle 구현한 class(ex. KafkaListener)들 중 가장 먼저 시작 되고 가장 나중에 종료
     */
    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }
}
